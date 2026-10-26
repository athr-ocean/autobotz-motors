# D15 — pontos de integração de Arthur

**Data:** 2026-10-26

**Escopo:** conexão JDBC compartilhada e requisitos para autorização administrativa. Este documento não implementa o adaptador do portal nem autenticação HTTP.

## Conexão JDBC para `TreinamentoService`

Use `autobotz.util.ConexaoBanco.getDataSource()`, cujo retorno é `javax.sql.DataSource`:

```java
var treinamento = new TreinamentoService(ConexaoBanco.getDataSource());
```

O serviço de Higor recebe um `DataSource`; seus DAOs abrem e fecham uma `Connection` com `try-with-resources`. Por isso, **não** passe `ConexaoBanco.getConexao()`: esse método existente devolve a conexão singleton compartilhada pelo ERP, que não é segura para esse ciclo de vida nem para chamadas concorrentes.

`ConexaoBanco.getDataSource()` abre uma conexão JDBC física nova a cada `getConnection()`, usando a configuração existente de `ConexaoBanco` e sem criar outro arquivo de credenciais ou outro pool. O provedor lê `AUTOBOTZ_DB_URL`, `AUTOBOTZ_DB_USER` e `AUTOBOTZ_DB_PASSWORD` pelo mesmo caminho central já usado pelo aplicativo. Não registre nem imprima esses valores.

- O chamador é dono da conexão retornada e deve fechá-la, de preferência em `try-with-resources`.
- O provedor não mantém pool nem fecha conexões em nome do chamador.
- O DAO/serviço propaga `SQLException`; o limite HTTP deve convertê-la em erro genérico sem SQL, credenciais ou detalhes internos na resposta.
- Mauro pode construir o serviço no ponto de composição do backend e delegar os métodos homônimos de `PortalBackend` a `TreinamentoService`. Não é necessário alterar `ConexaoBanco` global nem criar uma segunda configuração.
- A migration `015_d15_treinamento.sql` continua separada e deve ser aplicada apenas a um banco isolado `_test` para testes de integração. Esta fábrica não executa migrações.

O teste de ciclo de vida incluído usa um driver JDBC fictício e recusa executar sem `AUTOBOTZ_DB_URL=jdbc:autobotz-test:isolated`; não conecta ao MariaDB.

## Autorização de `GET /api/treinamentos/metricas`

O projeto possui `SessaoUsuario.getInstancia()` e `PerfilUsuario.ADMIN`, mas essa sessão guarda somente um usuário na memória da JVM. O `PortalLauncher` de Mauro inicia um servidor standalone; a sessão Swing/console não é associada a uma requisição do navegador, não tem identificador/cookie por usuário e pode nem existir nessa JVM. Logo, consultar o singleton dentro da rota ou confiar em `role=ADMIN` vindo do cliente não autentica o chamador.

Até João fornecer e integrar um resolvedor de sessão HTTP ligado ao login concluído (incluindo MFA), o portal deve continuar negando métricas. A semântica necessária é:

- `401 Unauthorized`: cookie/token ausente, expirado, inválido ou sessão encerrada.
- `403 Forbidden`: sessão válida reconhecida no servidor, mas papel diferente de `ADMIN`.
- `200 OK`: somente sessão válida com papel `ADMIN`, com a checagem feita no servidor em toda requisição.

Contrato necessário de João para implementação posterior: um serviço de sessão que resolva a requisição HTTP para um principal autenticado obtido do estado de servidor, por exemplo `Optional<Principal> resolver(HttpExchange)`, com identificador do usuário e papel; sessão criada somente após autenticação/MFA bem-sucedidos, expiração e invalidação no logout. O navegador nunca escolhe nem envia o papel autorizado. João e Mauro devem combinar a emissão/uso da sessão no fluxo real de lançamento do portal. Arthur não implementou login HTTP, cookies, MFA, RBAC nem um administrador de demonstração.

## Delegação a Mauro e João

- **Mauro:** usar `ConexaoBanco.getDataSource()` para construir `TreinamentoService` e adaptar `PortalBackend`; manter conexões por operação fechadas por `try-with-resources`; manter `/metricas` negado até receber o resolvedor de sessão de João.
- **João:** definir a interface de resolução e ciclo de vida da sessão web após MFA, inclusive invalidação no logout; não usar diretamente o singleton `SessaoUsuario` como prova de identidade HTTP.
- **Arthur:** manter a fábrica JDBC alinhada à configuração única existente e revisar compatibilidade/migrações sem executar alterações em banco real.
