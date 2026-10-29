# Roteiro da apresentação — AutoBotz Motors | Rodada 2 (D11–D15)

**Disciplina:** Administração para Computação 2026.2 · **Equipe B** · **Duração planejada:** 30 minutos.

Este roteiro destina-se à gravação do vídeo. Os integrantes devem conferir a divisão final e ensaiar os tempos. **O vídeo não está gravado.** Dados de assinaturas, incidentes, API, pagamentos e tickets são simulados onde indicado. A demonstração usa somente banco `_test` e dados fictícios.

## Preparar as telas antes de gravar

Na máquina do responsável, com a `main` atualizada e o MariaDB de teste já preparado:

```bash
cd ~/GitHub/autobotz-motors
source "$HOME/.local/state/autobotz-final/test-env.sh"
export AUTOBOTZ_DB_URL="jdbc:mariadb://127.0.0.1:3306/autobotz_d13_test"
export AUTOBOTZ_TEST_DB_URL="$AUTOBOTZ_DB_URL"
export AUTOBOTZ_TEST_DB_USER="$AUTOBOTZ_DB_USER"
export AUTOBOTZ_TEST_DB_PASSWORD="$AUTOBOTZ_DB_PASSWORD"
export AUTOBOTZ_JDBC_JAR="$HOME/.m2/repository/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar"
bash scripts/run.sh build
```

Em terminais separados, experimente `bash scripts/run.sh swing` e `bash scripts/run.sh portal`. O portal local costuma usar `http://127.0.0.1:8085`. **Não mostre a saída de `env`, as chaves, o conteúdo de `test-env.sh` ou credenciais em vídeo.** Não execute scripts que recriem ou apaguem bancos durante a gravação. Se o login visual não estiver preparado, mostre o teste automatizado como evidência, sem fingir uma sessão interativa.

## Bloco 1 — 00:00–02:00 · Arthur · Apresentação (2 min)

**Na tela:** nome do projeto, diretórios principais e equipe.

**Fala:**

“Boa tarde. Nós somos a Equipe B e vamos apresentar a segunda rodada do AutoBotz Motors. O projeto é um sistema de gestão desenvolvido em Java com MariaDB, interface Swing e uma aplicação de console. Na primeira fase ele já tinha recursos de clientes, veículos, vendas, oficina, CRM, projetos e controle de acesso. Nesta rodada o objetivo foi ampliar a operação sem perder essa base.

Dividimos o trabalho em cinco desafios: compras e sustentabilidade, planos e assinaturas em um cenário econômico difícil, contratos de fornecedores e rastreabilidade, segurança dos dados e treinamento dos usuários. Cada pessoa trabalhou na sua frente, e a etapa mais delicada foi fazer as peças conversarem entre si.

Vamos mostrar o que foi desenvolvido, os testes que realmente passaram e o que é apenas uma simulação acadêmica. Nosso foco não é dizer que o sistema se tornou um produto comercial, mas demonstrar decisões, integração e resultados verificáveis.”

**Demonstração (40 s):** mostrar `src/`, `tests/`, `db/migrations/` e `portal-treinamento/`.

## Bloco 2 — 02:00–05:00 · Arthur · Arquitetura e continuidade (3 min)

**Na tela:** estrutura MVC/DAO/serviços, `schema.sql` e migrations.

**Fala:**

“A estrutura do projeto separa modelos, DAOs, serviços e interface. Isso permite que a validação de uma operação não fique só no botão da tela: o serviço aplica a regra e a camada de persistência registra o resultado no banco.

As novas tabelas foram criadas por migrations numeradas de 011 a 016. A ordem importa: contratos de fornecedores, por exemplo, dependem da estrutura de fornecedores do módulo de compras. A parte de criptografia também precisa conviver com as alterações de login e segurança sem substituir as tabelas do outro integrante.

No fechamento, nós testamos a importação do esquema e a aplicação dessas migrations num banco descartável. A sequência foi repetida e não apresentou erro. Além disso, executamos a regressão do sistema anterior para verificar que a expansão não tinha quebrado os fluxos básicos de clientes, vendas, oficina e autorização.

Aqui é importante separar duas coisas: compilar o projeto confirma a consistência do código, mas não confirma tudo que acontece na tela. Por isso mantivemos testes de banco e de serviços, além de demonstrar visualmente o que estiver disponível.”

**Demonstração (1 min):** listar `db/migrations/`; apontar tabelas 011–016; mostrar `CoreRegression` no registro de testes.

## Bloco 3 — 05:00–09:00 · Alexandre e Adryan · D11 compras, qualidade e sustentabilidade (4 min)

**Na tela:** `ComprasPanel`, `ComprasService`, `D11ComprasTest` e `D11ComprasJdbcTest`.

**Alexandre — fala:**

“No desafio de compras, o fluxo começa com o fornecedor e o orçamento. A pessoa solicita uma compra, informa o valor e a descrição, e a aprovação verifica as permissões e o saldo disponível. Se a operação for válida, o sistema gera o pedido e desconta o valor do orçamento. Se houver uma falha, a transação precisa preservar o saldo anterior.

Esse detalhe é importante porque uma aprovação não pode reduzir o orçamento sem registrar o pedido correspondente. A regra fica no serviço, não apenas no formulário. O DAO participa da mesma transação, e o banco usa InnoDB para dar suporte ao rollback.

Também tratamos casos de erro: dados inválidos, orçamento insuficiente e uma solicitação que já foi aprovada. Não é só o caminho de sucesso que interessa; os testes precisam mostrar que o sistema sabe recusar operações incorretas.”

**Adryan — fala:**

“A outra parte do D11 foi automatizar a verificação. Executamos testes de serviço e testes JDBC para conferir pedido, saldo, rollback e autorização. Eles passaram na integração final. O GitHub Actions também aprovou as rotinas de regressão configuradas.

A automação ajuda a repetir o mesmo cenário e identificar problemas mais cedo. Mas nós não fizemos uma medição comparável de tempo manual nem telemetria energética. Por isso não vamos afirmar uma redução percentual de energia ou produtividade que não foi medida. O resultado comprovado aqui é a automação e a capacidade de reproduzir a validação.”

**Demonstração (1 min):** mostrar a aprovação e o teste de rollback ou a saída já registrada.

## Bloco 4 — 09:00–13:30 · André · D12 crise econômica e assinaturas (4 min 30 s)

**Na tela:** `AssinaturaService`, `AssinaturaDAO`, modelos e testes.

**Fala:**

“O desafio do cenário econômico foi pensar em como adaptar uma empresa a mudanças na capacidade de pagamento dos clientes. Para isso o ERP ganhou estrutura de planos, módulos opcionais, contratação, alteração e cancelamento de assinaturas.

Duas medidas orientam a análise: o MRR, que é a receita mensal recorrente, e o churn, que representa a proporção de cancelamentos no período. O sistema guarda eventos das assinaturas e permite simular o efeito de mudar preços ou migrar de plano.

No cenário didático, trabalhamos com uma divisão de 30% migrando para planos mais baratos, 50% permanecendo e 20% migrando para planos mais caros. Os números usados são fictícios; servem para validar cálculos e discutir estratégia, não para representar uma carteira real da empresa.

Uma dificuldade de integração foi compatibilizar o resultado da simulação com a persistência das assinaturas, inclusive valores de módulos adicionais. Isso foi testado pelo `AssinaturaServiceTest` e pelo `AssinaturaDAOJdbcTest`, que passaram no banco de teste.

A parte de comunicação é igualmente importante: um cliente precisa entender o motivo da mudança e as opções que possui. Nossa documentação descreve como apresentar essa transição sem prometer uma condição que o sistema não pode cumprir.”

**Demonstração (aprox. 2 min):** mostrar fórmulas e teste, não anunciar dados simulados como medição real.

## Bloco 5 — 13:30–17:30 · Gabriel · D13 contratos e rastreabilidade (4 min)

**Na tela:** `ContratoService`, ledger, exemplos de hash e testes.

**Fala:**

“O desafio de contratos amplia o cadastro de fornecedores que veio do D11. Agora é possível acompanhar o ciclo de vida contratual, valor, vencimento, estado e validação da nota fiscal. As regras de aviso e elegibilidade para pagamento consideram essas condições, mas o pagamento é apenas simulado.

Para manter uma trilha de integridade, usamos um ledger local encadeado. Cada registro contém seu conteúdo, um hash calculado a partir dele e a referência ao registro anterior. Se alguém altera um evento antigo, a cadeia deixa de conferir. O objetivo é permitir detectar adulterações no histórico.

É importante deixar claro que essa prova de conceito não é blockchain pública e não usa uma rede de nós independentes. Também existe uma API simulada para representar a troca de status. Ela não envia valores para bancos ou fornecedores de verdade.

Nos testes, exercitamos alertas de vencimento, mudanças de estado, recusa de pagamentos inválidos e uma alteração deliberada de registro, que precisa ser detectada. Tanto o teste de integridade quanto o JDBC passaram na versão integrada.”

**Demonstração (1–2 min):** mostrar o evento de ledger e a verificação de adulteração no teste.

## Bloco 6 — 17:30–22:00 · João e Arthur · D14 segurança e dados pessoais (4 min 30 s)

**Na tela:** autenticação, `CriptografiaDadosPessoais`, `D14SegurancaTest`, `CoreRegression`.

**João — fala:**

“A autenticação agora registra falhas e bloqueia uma conta quando há cinco tentativas incorretas na janela de dez minutos. Para os perfis de acesso crítico existe um segundo passo, o MFA demonstrativo, antes da sessão ser autorizada.

Os testes verificam códigos inválidos, vencidos e reutilizados. Também acompanhamos eventos de segurança, e o sistema oferece migração do hash legado de senha para um formato com derivação e sal. O plano de resposta a ransomware que apresentamos é uma simulação: descreve identificação, contenção, recuperação, comunicação e o que aprenderíamos depois do incidente.”

**Arthur — fala:**

“Minha parte foi proteger os dados pessoais dos clientes sem quebrar o restante do ERP. Aplicamos AES-GCM nos campos pessoais e usamos um índice HMAC para a busca exata por CPF. Assim, o CPF não precisa ficar disponível em texto puro como índice.

A chave fica fora do repositório. A migração de dados antigos também não acontece silenciosamente; ela possui etapa de diagnóstico e confirmação. Isso evita que um procedimento de teste modifique registros reais por engano.

Executamos a regressão integrada e o teste JDBC de segurança. A validação HTTP do portal confirma que sem sessão a API de métricas responde 401, com perfil sem permissão responde 403 e com ADMIN autenticado após MFA responde 200.”

**Demonstração (1 min):** mostrar saída dos testes e a variável de configuração pelo nome, nunca seu valor.

## Bloco 7 — 22:00–26:30 · Mauro e Higor · D15 treinamento (4 min 30 s)

**Na tela:** Central de Treinamento em `http://127.0.0.1:8085`, catálogo, guias, FAQ, `TreinamentoService` e testes.

**Mauro — fala:**

“O D15 foi pensado para reduzir a dificuldade de aprender um sistema que foi crescendo de tamanho. O portal local organiza materiais publicados, separa itens ainda em preparação e oferece acesso por navegador. A entrada do portal foi integrada à aplicação principal.

Ele não depende de um serviço na nuvem. O backend registra eventos no MariaDB por meio do `TreinamentoService` e do `DataSource` compartilhado com a aplicação. O servidor também valida os dados recebidos e não deve servir arquivos fora da pasta pública.”

**Higor — fala:**

“Minha frente envolveu o conteúdo e as métricas. Preparamos guia rápido, FAQ, materiais e roteiro de tutorial. Também distinguimos abertura do portal, visualização de material e visualização de vídeo, para que esses eventos possam ser contabilizados.

Esses eventos são registros da execução do sistema, enquanto dados históricos de tickets e pesquisas foram simulados no trabalho. Portanto não podemos afirmar que houve redução real de chamados. O que existe é a estrutura para medir esse efeito futuramente.

Os testes cobrem catálogo, persistência dos eventos e autorização HTTP. As métricas administrativas não são disponibilizadas a qualquer visitante; a sessão e o perfil são conferidos no servidor.”

**Demonstração (1–2 min):** abrir catálogo e FAQ; mostrar a saída `D15PersistenciaTest` e `D15AuthHttpJdbcTest`.

## Bloco 8 — 26:30–28:30 · Arthur · Integração e evidências (2 min)

**Na tela:** GitHub, PR #37 mesclada, workflows e log de teste.

**Fala:**

“O principal resultado da integração foi reunir os cinco desafios na mesma base de código. O GitHub preservou os commits dos integrantes e a consolidação foi feita por uma PR específica, sem reescrever o trabalho original.

Na máquina de validação, o projeto compilou com Java 21. Executamos dez testes offline e oito testes JDBC com MariaDB 13.0.2, todos aprovados. As seis migrations foram reaplicadas no banco descartável sem erro. O GitHub Actions também registrou sucesso na regressão integrada e na segurança D14.

Esses testes não provam que uma aplicação está pronta para uso comercial. O que eles mostram é que os cenários definidos para a entrega têm execução reproduzível e que os módulos não ficaram somente documentados ou isolados em branches.”

**Demonstração (40 s):** resultados do GitHub Actions e trecho final do log, sem credenciais.

## Bloco 9 — 28:30–30:00 · Arthur · Limitações e conclusão (1 min 30 s)

**Na tela:** checklist e título de encerramento.

**Fala:**

“Para concluir, quero destacar os limites do trabalho. Não medimos economia real de energia nem uma comparação manual de produtividade em condições controladas. Também não temos dados reais de redução de chamados por treinamento.

O pagamento e a blockchain do D13 são simulados. O MFA do D14 é demonstrativo, e os dados de churn, tickets e pesquisa usados nas análises acadêmicas também são fictícios. Os roteiros de vídeos e webinars não devem ser confundidos com gravações já produzidas.

Dentro desse escopo, conseguimos integrar os módulos, preparar o banco, validar os fluxos críticos e documentar os resultados. A segunda rodada mostrou, principalmente, como requisitos de negócio, segurança e testes precisam funcionar em conjunto. Obrigado.”

## Referências para conferência antes da gravação

- `RELATORIO_GERAL_RODADA2.md`, `CHECKLIST_FINAL.md` e `docs/AUDITORIA_LIMPEZA_FINAL.md`.
- `docs/D11_QUALIDADE_SUSTENTABILIDADE.md`, `docs/D13_CONTRATOS_LEDGER.md`, `docs/D14_RELATORIO_INCIDENTE_SIMULADO.md`, `docs/D15_GUIA_RAPIDO.md`.
- `tests/CoreRegression.java`, `tests/D14JdbcIntegrationTest.java`, `tests/D15AuthHttpJdbcTest.java`.
- A gravação é responsabilidade da equipe. Antes de mostrar uma tela específica, ensaie esse fluxo manualmente no banco descartável e ajuste a fala se o comportamento divergir.
