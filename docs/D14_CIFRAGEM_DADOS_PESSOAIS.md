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
2. Aplique a versão atualizada de schema.sql ao banco pretendido. Ela amplia as colunas de texto e acrescenta cpf_hash e seu índice; as alterações DDL do MariaDB podem fazer commit implícito.
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

CoreRegression verifica que novos clientes são armazenados com campos cifrados, que o CPF é pesquisável pelo índice HMAC, que o DAO devolve os dados descriptografados e que atualização e anonimização preservam a proteção. A suíte deve rodar somente em uma base isolada cujo nome satisfaça o guard já existente no teste; não rode a regressão no banco pessoal.

## Limites e responsabilidades

- Criptografia em repouso não substitui RBAC, controle de acesso ao servidor, backups seguros, atualização de dependências ou revisão legal de LGPD.
- cpf_hash permite localizar valores iguais, embora não revele diretamente o CPF; não deve ser exposto pela interface ou por relatórios.
- A rotação de chave exige procedimento separado de recriptografia. Não substitua a chave sem planejar a migração.
- Os hashes de senha e o MFA pertencem à frente de autenticação; esta mudança não modifica AuthService nem substitui a frente de João.
