# D14 — Revisão da PR #18 e compatibilidade da migration 016

**Escopo da revisão:** fronteira entre `db/migrations/014_d14_security.sql` (autenticação/MFA) e a migration de dados pessoais atribuída a Arthur.

## Evidências disponíveis

- A PR [#18 — feat(d14): protege dados pessoais de clientes](https://github.com/athr-ocean/autobotz-motors/pull/18) estava aberta na consulta de 28/10/2026, propondo integrar `feature/d14-client-pii-encryption` à `main`; a página indicava 14 commits e nenhum review registrado.
- Na atualização da PR publicada em 28/10, Arthur informa que os ajustes de PII foram movidos para `db/migrations/016_d14_client_pii.sql`, preservando `014_d14_security.sql`, e que testou a migration duas vezes em uma base descartável `_test`, verificando colunas, índice, preservação de registro fictício e idempotência. Isso é uma declaração do autor da PR, não uma execução repetida nesta sessão.
- O ZIP de projeto recebido nesta sessão contém `014_d14_security.sql` e `015_d15_treinamento.sql`, mas **não contém `016_d14_client_pii.sql`**. O conteúdo SQL exato de 016, portanto, não pôde ser inspecionado nem executado nesta revisão.

## Comparação de escopo: migration 014

A migration 014 altera `usuarios.senha_hash` para `VARCHAR(255)` e cria `eventos_seguranca` e `mfa_desafios`. Ela não cria nem altera colunas da tabela `clientes`, nem cria o índice HMAC descrito no escopo da PR #18. A migration 015 cria `treinamento_eventos`, sem dependência declarada de colunas de PII em `clientes`.

Assim, **não há colisão visível no SQL 014/015 presente no pacote** com a separação descrita para 016. A sequência nominal 014 → 015 → 016 é compatível no nível de escopo documentado, desde que 016 seja realmente aditiva, não altere objetos de autenticação e suas pré-condições sobre `clientes`/`schema.sql` sejam satisfeitas.

## Resultado e limite obrigatório

**Revisão parcial; compatibilidade integral ainda não confirmada.** Não seria correto certificar o SQL de 016 sem seu conteúdo. Antes de aprovar a PR #18, obter a versão atual de `db/migrations/016_d14_client_pii.sql` e verificar:

1. se altera somente as colunas/índices de PII esperados e não redefine `usuarios`, `eventos_seguranca` ou `mfa_desafios`;
2. se funciona com o schema-base real e após 014 e 015;
3. se a segunda execução é idempotente e não perde dados existentes;
4. se o procedimento de migração de registros legados é explícito, transacional e não executado automaticamente no startup;
5. se a configuração de chave externa é documentada sem segredos no repositório.

Não aplicar migrations em banco normal ou de produção. A verificação final deve ser feita em uma base descartável com nome terminado em `_test`, usando a migration exata do branch atual da PR.
