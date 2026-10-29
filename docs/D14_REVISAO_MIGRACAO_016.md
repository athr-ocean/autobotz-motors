# D14 — Compatibilidade das migrations 014 e 016

**Estado atualizado em 29/10/2026, após integração da PR #37.**

## Frentes e atribuições

- A migration `014_d14_security.sql` implementa as estruturas de eventos, MFA e adequação da coluna `senha_hash`, a partir do trabalho de João.
- A migration `016_d14_client_pii.sql` adiciona/ajusta as colunas de dados pessoais e o índice HMAC de CPF, a partir do trabalho de Arthur.
- A migration `015_d15_treinamento.sql` contém os eventos de treinamento e não substitui os objetos da autenticação.

## Verificação no código consolidado

Os arquivos **011–016 estão presentes no repositório** e são aplicados nessa ordem. A versão integrada testada em 29/10/2026 foi construída no commit `718fe28` e mesclada à `main` pela PR #37 (merge `f56c8dd`).

O responsável executou em MariaDB 13.0.2, no banco isolado `autobotz_d13_test`:
- importação de `schema.sql`;
- aplicação completa das migrations 011 a 016;
- reaplicação completa das mesmas migrations;
- `CoreRegression` e `D14JdbcIntegrationTest`, ambos aprovados;
- integração HTTP com autenticação/MFA e autorização de métricas (`D15AuthHttpJdbcTest`) aprovada.

A chave AES-GCM utilizada no ensaio foi gerada localmente e mantida fora do Git.

## Restrições de produção

A migração 016 amplia a estrutura do banco; **não cifra silenciosamente registros legados**. A conversão de linhas antigas exige execução deliberada de `MigracaoDadosPessoais --check` e, quando autorizada, `--apply --confirm-db=...`, usando chave externa. O uso em banco com dados reais exige planejamento, backup e revisão de acesso.

O MFA é demonstrativo. O resultado positivo da suíte não equivale a auditoria independente de segurança ou certificação de uso em produção.

**Observação histórica:** versões antigas deste documento afirmavam que a migration 016 não havia sido obtida para revisão. Esse bloqueio foi resolvido na consolidação de 29/10/2026; prevalece o estado atualizado acima.
