# D15 — integração com o ERP (Arthur)

## Inicialização

Na branch de integração que depende do PR #24 de Mauro, `bash scripts/run.sh portal`
compila `src/` e inicia `autobotz.portal.PortalLauncher` com o driver JDBC no classpath.
O modo exige execução a partir da raiz do repositório. Ele é uma opção explícita; abrir o
ERP Swing não inicia o servidor. O launcher atual de Mauro ainda usa `MockPortalBackend`,
portanto os eventos dessa versão não são persistidos até que a integração dele com o
`TreinamentoService` e o `DataSource` seja concluída.

## Dependências da navegação Swing

Na head `ee0eff4` do PR #24 não existe `src/autobotz/portal/PortalAtalho.java` (nem essa
classe em outra branch remota consultada). Por isso a chamada de `PortalAtalho.abrir()` no
`AppFrame` ainda depende de Mauro entregar essa classe no PR #24 ou em commit separado.
Arthur não inicia o servidor a partir do ERP e não duplica a classe/implementação do portal.
Quando a classe estiver disponível, o item de menu deve ser visível a ADMIN e VENDEDOR,
usar `gui.training_center` e apenas abrir a URL local já servida; indisponibilidade deve
ser apresentada ao usuário, sem iniciar um segundo servidor.

## Banco, migrations e testes — handoff para Adryan

- `scripts/run.sh`: compila e inicia Swing, console ou portal. Não cria banco, não aplica
  migrations e não é script de provisionamento/teste de MariaDB.
- `schema.sql`: schema de instalação que contém `CREATE DATABASE`/`USE autobotz_db`;
  não deve ser importado cegamente em um banco `_test`.
- `db/migrations/014_d14_security.sql`: estruturas de autenticação/MFA de João.
- `db/migrations/015_d15_treinamento.sql`: tabela de eventos do portal de Higor; esta
  integração não altera a migration.
- `tests/D15PersistenciaTest.java` (PR #24): exige `AUTOBOTZ_TEST_DB_URL`, usuário e senha,
  recusa URL cujo banco não termine em `_test`, aplica a migration 015 e valida gravação,
  métricas e rota HTTP. A limpeza do teste apaga as linhas de `treinamento_eventos`; use
  somente banco descartável exclusivo.
- `tests/D14JdbcIntegrationTest.java`: teste JDBC D14 com guarda para banco `_test`.
- Nesta cópia não há script separado de provisionamento de banco. Migrations 013, 011 e
  012 ainda dependem de PRs/implementações que não estão nesta base.

Adryan é responsável pelo script de testes, bootstrap isolado, CI e execução dos testes.
Este handoff identifica os arquivos existentes; não altera nem assume a parte dele.

## Proposta para autorização de `/api/treinamentos/metricas`

O acesso permanece negado até a integração HTTP usar a sessão autenticada no servidor.
Na `origin/main` atual, João já fornece `SessaoWebService`; a branch-base do PR #24 ainda
precisa incorporar essa dependência. Opção para combinar com João e Mauro:

1. Criar uma única instância do `SessaoWebService` no processo do portal e encaminhar os
   passos HTTP de login/MFA/logout para essa instância, sem criar outro `AuthService` ou
   sessão paralela.
2. Após login/MFA, enviar o token opaco em cookie de sessão `HttpOnly`, `SameSite=Strict`,
   `Path=/` e `Secure` quando servido por HTTPS. Não colocar tokens em URLs nem aceitar
   `role` enviado pelo navegador. Em operações que alterem estado, validar `Origin` e
   proteção CSRF acordada com João.
3. Em cada pedido de métricas, extrair o cookie e chamar `SessaoWebService.autorizarMetricas`;
   mapear sem sessão válida para 401, sessão válida sem permissão para 403 e permitir a
   resposta apenas quando a autorização server-side resultar em 200. Falha SQL não deve
   liberar acesso.

O contrato e as rotas de login/MFA/logout precisam de acordo de João antes de substituir o
`401` fixo. Esta proposta não altera a autenticação nem enfraquece a proteção atual.
