# D15 — integração com o ERP (Arthur)

## Inicialização

Na branch de integração que depende do PR #24 de Mauro, `bash scripts/run.sh portal`
compila `src/` e inicia `autobotz.portal.PortalLauncher` com o driver JDBC no classpath.
O modo exige execução a partir da raiz do repositório. Ele é uma opção explícita; abrir o
ERP Swing não inicia o servidor. O launcher atual de Mauro ainda usa `MockPortalBackend`,
portanto os eventos dessa versão não são persistidos até que a integração dele com o
`TreinamentoService` e o `DataSource` seja concluída.

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
- `tests/D14JdbcIntegrationTest.java`: teste JDBC D14 com guarda para banco `_test`.
- Nesta cópia não há script separado de provisionamento de banco. Migrations 013, 011 e
  012 ainda dependem de PRs/implementações que não estão nesta base.

Adryan é responsável pelo script de testes, bootstrap isolado, CI e execução dos testes.
Este handoff identifica os arquivos existentes; não altera nem assume a parte dele.

## Proposta para autorização de `/api/treinamentos/metricas`

O acesso permanece negado até a integração HTTP usar a sessão autenticada no servidor.
A branch do portal ainda não integra `SessaoWebService`, embora João já a tenha integrado
à `main`. Opção mínima para combinar com João e Mauro:

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

### Proposta de código de uso único no fragmento da URL

O fragmento (`#...`) não é enviado automaticamente na requisição HTTP nem no cabeçalho
`Referer`, o que é melhor que colocar um bearer token em query string. Ainda assim, o
fragmento fica acessível ao JavaScript da página, extensões e histórico do navegador; XSS,
redirecionamentos e cópia da URL podem expô-lo. O serviço atual de João não oferece emissão
nem troca de código de uso único, portanto o portal não implementa esse fluxo.

Se a equipe preferir SSO iniciado pelo ERP, João precisa fornecer no próprio
`SessaoWebService` uma emissão e troca atômica de código: código aleatório opaco, vinculado à
identidade autenticada e ao público do portal, validade de no máximo 60 segundos, uso único
e invalidação após logout. O browser deve removê-lo do histórico imediatamente e trocá-lo
por POST same-origin; a resposta cria somente o cookie HttpOnly, SameSite=Strict e Secure
sob HTTPS. O endpoint deve validar Origin/CSRF e a rota de métricas deve sempre chamar
`autorizarMetricas` no servidor. O fragmento nunca carrega papel ou métricas, e o portal
continua respondendo 401 até esse contrato existir e ser testado. Alternativamente, o portal
pode apresentar login/MFA próprio usando a mesma instância do serviço de João, sem SSO por
fragmento.
