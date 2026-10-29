# D15 — integração com o ERP (Arthur)

## Inicialização

Na integração, `bash scripts/run.sh portal` compila `src/` e inicia
`autobotz.portal.PortalLauncher` a partir da raiz do repositório. O modo é explícito; abrir o
ERP Swing não inicia o servidor. O launcher do PR #24 atualizado constrói o backend com
`TreinamentoService(ConexaoBanco.getDataSource())`. O teste JDBC usa a mesma factory do
launcher e confirmou persistência no MariaDB descartável; o comando interativo completo e a
janela Swing ainda não foram exercitados nesta validação.

## Dependências da navegação Swing

A branch atual de Mauro no PR #24 fornece `PortalAtalho`. A integração de Arthur acrescenta
a entrada Swing para ADMIN e VENDEDOR. O servidor só é iniciado quando o usuário clica no
item; abrir o ERP não inicia o portal. A ação traduz os cinco estados do `PortalAtalho` nos
bundles PT-BR e EN-US. Se o navegador não estiver disponível, a mensagem mostra a URL
local para abertura manual. O item não cria uma segunda instância do servidor.

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
- `tests/D15AuthHttpJdbcTest.java` e `scripts/test-d15-auth-jdbc.sh` (integração Arthur):
  usam contas fictícias `d15http_*` e validam login, MFA, cookie, logout e métricas por HTTP
  contra o `SessaoWebService` e backend persistente do launcher. O teste exige migrations
  014/015 e recusa URL que não termine em `_test`; remove apenas as contas e eventos de
  segurança criados por ele.
- `tests/D14JdbcIntegrationTest.java`: teste JDBC D14 com guarda para banco `_test`; o
  ajuste para refletir MFA obrigatório após migração de hash legado está no PR #31.
- Nesta cópia não há script separado de provisionamento de banco. Migrations 013, 011 e
  012 ainda dependem de PRs/implementações que não estão nesta base.

Adryan é responsável pelo script de testes, bootstrap isolado, CI e execução dos testes.
Este handoff identifica os arquivos existentes; não altera nem assume a parte dele.

## Autorização HTTP de `/api/treinamentos/metricas`

O servidor do portal usa `SessaoWebService` no processo HTTP. O login exige senha e MFA,
emite o cookie opaco `ABM_SESSAO` só após validar o segundo fator e o logout invalida a
sessão no servidor. A rota consulta `autorizarMetricas`: sem sessão retorna 401, VENDEDOR
autenticado retorna 403 e ADMIN autenticado pode receber 200. O teste
`D15AuthHttpJdbcTest` cobriu esses estados por HTTP com usuários fictícios persistidos em
MariaDB `_test`, inclusive logout seguido de 401. O código MFA enviado pela resposta é
marcado explicitamente como demonstração; não há envio real por SMS ou e-mail. O servidor
é local em HTTP loopback e não está preparado como serviço de produção exposto à rede.

Não existe SSO iniciado pelo Swing nem fluxo por fragmento de URL. O atalho abre o portal;
o usuário autentica-se no próprio portal. A validação reproduzível do teste JDBC é
`bash scripts/test-d15-auth-jdbc.sh`, após compor as dependências #22 e #24 e aplicar
migrations 014 e 015 no banco `_test`. A branch #29 isolada depende dessas fontes e não é
uma instalação autônoma.
