# D14 — Criptografia e migração de dados pessoais

## Objetivo

A frente D14 protege nome, CPF, telefone e e-mail dos clientes em repouso no MariaDB. A criptografia é feita na camada de persistência, para que a interface, o CRM e as regras de negócio continuem recebendo os valores originais por meio do ClienteDAO.

## Desenho aplicado

- AES-256-GCM cifra os campos nome, CPF, telefone e e-mail. O prefixo enc:v1: identifica os campos protegidos.
- O nome do campo é autenticado como AAD; copiar o texto cifrado de uma coluna para outra causa falha na verificação de integridade.
- O CPF cifrado não permite uma pesquisa SQL por igualdade. Para manter buscarPorCpf e a checagem de duplicidade, cpf_hash contém um HMAC-SHA-256 determinístico, protegido por uma chave derivada separada. Ele não contém o CPF em texto puro e não pode ser recalculado sem a chave.
- A chave não fica no código nem no banco. A aplicação aceita AUTOBOTZ_PII_KEY ou AUTOBOTZ_PII_KEY_FILE; a recomendação é manter o conteúdo em arquivo fora do repositório, com acesso restrito.
- A leitura de registros antigos sem prefixo continua possível durante a transição. Novos cadastros e atualizações são gravados cifrados.
- A migração nunca é executada automaticamente durante a inicialização do ERP.

## Configuração da chave no Linux

Execute os comandos em uma sessão Bash. O bloco gera a chave somente se o arquivo ainda não existir, para não trocar acidentalmente uma chave já utilizada.

    mkdir -p "$HOME/.config/autobotz"
    chmod 700 "$HOME/.config/autobotz"
    if [ ! -s "$HOME/.config/autobotz/pii.key" ]; then
      (umask 077; openssl rand -base64 32 > "$HOME/.config/autobotz/pii.key")
    fi
    chmod 600 "$HOME/.config/autobotz/pii.key"
    export AUTOBOTZ_PII_KEY_FILE="$HOME/.config/autobotz/pii.key"

Mantenha a mesma chave em todas as execuções que precisam ler os mesmos dados. Faça uma cópia de segurança da chave em local seguro, separado do banco. Perder a chave torna os campos cifrados irrecuperáveis. Nunca adicione pii.key, a chave Base64 ou cópias de backup ao Git.

## Preparação de banco existente

1. Pare a aplicação e faça backup consistente do banco e da chave atual.
2. Em ambientes existentes, aplique `db/migrations/016_d14_client_pii.sql` ao banco escolhido, depois das migrations 014 (autenticação) e 015 (eventos D15). Ela amplia as colunas pessoais cifradas para VARCHAR(1024), dimensão necessária para acomodar a expansão Base64 em UTF-8, e acrescenta `cpf_hash` e seu índice. A migration 014 de João permanece sem alterações. Em instalação nova, `schema.sql` também contém essa estrutura; não execute os dois caminhos sem necessidade.
3. Configure AUTOBOTZ_PII_KEY_FILE na sessão Bash.
4. Compile a versão nova do projeto.
5. Execute primeiro o modo de diagnóstico:

    bash scripts/run.sh build
    JDBC_JAR="$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar"
    java -cp "build/classes:src:$JDBC_JAR" autobotz.util.MigracaoDadosPessoais --check

O diagnóstico não atualiza linhas. Confirme que o nome do banco impresso é o esperado e revise a contagem de registros pendentes.

## Aplicação deliberada da migração

Depois de conferir o backup, o nome do banco e a chave, execute a migração com o nome exato que foi mostrado no diagnóstico. Exemplo exclusivamente ilustrativo para o banco autobotz_db:

    java -cp "build/classes:src:$JDBC_JAR" autobotz.util.MigracaoDadosPessoais --apply --confirm-db=autobotz_db

O programa bloqueia os registros durante a transação e confirma todas as alterações em conjunto. Em caso de falha, faz rollback. O modo --apply nunca deve ser executado contra um banco sem backup conferido. Em banco de teste, valide consultas, duplicidade, atualização e anonimização antes de migrar dados de usuários reais.

## Testes adicionados

CoreRegression verifica que novos clientes são armazenados com campos cifrados, que o CPF é pesquisável pelo índice HMAC, que o DAO devolve os dados descriptografados, que a oficina não exibe nome cifrado, que campos UTF-8 longos cabem na coluna cifrada, que uma chave incorreta é rejeitada, e que atualização e anonimização preservam a proteção. Também insere um cliente legado em texto puro e verifica que `--check` não altera registros, que `--apply` cifra os campos e calcula o HMAC, que a consulta pelo DAO continua funcionando e que uma segunda execução não encontra pendências.

### Executar a regressão completa

Execute somente contra um banco de teste **vazio** cujo nome satisfaça o guard do teste, por exemplo `autobotz_d14_verify_test`. A suíte altera dados e executa a migração no banco configurado; nunca aponte para `autobotz_db` ou outra base com dados reais.

Na raiz do repositório, em Bash:

    export AUTOBOTZ_PII_KEY_FILE="$HOME/.config/autobotz/pii.key"
    export AUTOBOTZ_DB_URL="jdbc:mariadb://127.0.0.1:3306/autobotz_d14_verify_test"
    export AUTOBOTZ_DB_USER="autobotz_d14_test"
    export AUTOBOTZ_DB_PASSWORD="$(cat "$HOME/.config/autobotz/d14-test-db-password")"
    JDBC_JAR="$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar"
    bash scripts/run.sh build
    mkdir -p build/test-classes
    javac -encoding UTF-8 -cp "build/classes:src:$JDBC_JAR" -d build/test-classes tests/CoreRegression.java
    java -cp "build/test-classes:build/classes:src:$JDBC_JAR" CoreRegression

A saída esperada ao terminar é `CORE_REGRESSION_OK`, sem `FAIL` ou exceções. Crie um banco novo com `schema.sql` antes de rodar; a suíte não limpa nem recria o banco automaticamente.

## Impacto no desempenho e priorização dos dados

### Quais dados proteger primeiro

A implementação protege os quatro campos pessoais existentes de clientes:

1. **CPF:** identificador direto, usado em busca e detecção de duplicidade; é protegido com AES-GCM e acompanhado por um HMAC separado para pesquisa exata.
2. **Nome:** identifica diretamente a pessoa quando associado a outros dados.
3. **Telefone e e-mail:** dados de contato que podem identificar ou expor a pessoa; ambos também são protegidos.

A chave primária `id_cliente`, as chaves estrangeiras usadas por vendas/ordens de serviço e o indicador operacional `ativo` permanecem estruturados para preservar relações e consultas. Não são campos pessoais que precisem ser cifrados para cumprir este escopo.

### Efeito esperado

- **CPU:** cada gravação ou atualização cifra os campos não nulos; a leitura decifra os campos protegidos. O CPF também exige o cálculo de HMAC na busca. Isso adiciona trabalho de CPU em comparação com texto puro.
- **Armazenamento e I/O:** o formato guarda o prefixo `enc:v1:`, um IV aleatório de 12 bytes, a tag de autenticação GCM de 16 bytes e o conteúdo codificado em Base64. Para um valor com `n` bytes UTF-8, o texto armazenado ocupa aproximadamente `7 + ceil(4 × (n + 28) / 3)` caracteres, sem contar eventuais limites do banco. Por isso, as colunas passaram a `VARCHAR(1024)`, dimensionadas para a expansão dos tamanhos originais do schema.
- **Pesquisa por CPF:** o HMAC acrescenta um cálculo pequeno, mas o índice `idx_clientes_cpf_hash` evita depender de uma varredura e descriptografia de todos os clientes para a busca exata. O índice determinístico revela padrões de igualdade, mas não armazena o CPF em texto puro.
- **Compatibilidade:** o custo de criptografia/descriptografia acontece na camada DAO; a interface, o CRM e as relações por ID continuam usando os valores originais devolvidos pelo DAO.

### Evidência e limite da análise

A regressão executada validou funcionamento, integridade, dados UTF-8 longos, pesquisa, atualização, anonimização e migração. **Ela não é um benchmark de desempenho** e não foi usado um número de milissegundos, porcentagem de overhead ou consumo de energia como se tivesse sido medido. Para uma estimativa quantitativa, deve-se medir uma carga representativa em hardware identificado, comparando a mesma operação com e sem criptografia. Até lá, o impacto descrito acima é uma análise técnica qualitativa, não um resultado de produção.

## Limites e responsabilidades

- Criptografia em repouso não substitui RBAC, controle de acesso ao servidor, backups seguros, atualização de dependências ou revisão legal de LGPD.
- cpf_hash permite localizar valores iguais, embora não revele diretamente o CPF; não deve ser exposto pela interface ou por relatórios.
- A rotação de chave exige procedimento separado de recriptografia. Não substitua a chave sem planejar a migração.
- Os hashes de senha e o MFA pertencem à frente de autenticação; esta mudança não modifica AuthService nem substitui a frente de João.
