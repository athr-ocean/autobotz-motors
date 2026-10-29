# D15 — Portal de treinamento (como executar e verificar)

Responsável: Mauro. Conteúdo e métricas: Higor. Contrato das rotas: `docs/D15_CONTRATO_API.md`.

## O que é

Uma central de treinamento web, separada do Swing e do console. Um servidor leve do JDK
(`com.sun.net.httpserver`, sem framework) serve a página e três rotas de API. O portal roda
somente em `localhost`.

## Requisitos

- JDK 21 (o mesmo do projeto).
- Executar sempre a partir da raiz do repositório.

## Executar

    mkdir -p /tmp/portal-out
    javac -sourcepath src -d /tmp/portal-out src/autobotz/portal/*.java
    java -cp /tmp/portal-out:CAMINHO/mariadb-java-client-3.5.10.jar autobotz.portal.PortalLauncher

Configure `AUTOBOTZ_DB_URL`, `AUTOBOTZ_DB_USER` e `AUTOBOTZ_DB_PASSWORD` para o banco que já
recebeu a migration `015_d15_treinamento.sql`. O backend JDBC é o padrão e o launcher encerra
com uma mensagem se não conseguir abrir conexão. Para testes manuais sem persistência, selecione
explicitamente `AUTOBOTZ_PORTAL_BACKEND=mock`. Depois abra `http://localhost:8085`. Para usar outra
porta: `AUTOBOTZ_PORTAL_PORT=8090`. Para parar, `Ctrl+C`.

## Testes das rotas

    javac -sourcepath src -d /tmp/portal-out src/autobotz/portal/*.java tests/D15PortalTest.java
    java -cp /tmp/portal-out D15PortalTest

Resultado esperado: linhas `PASS` e, no fim, `TODOS OS TESTES PASSARAM`. Os testes não usam banco:
criam uma pasta temporária com catálogo de teste e um backend em memória.

## Estrutura

- `src/autobotz/portal/` — servidor (`PortalServer`), catálogo (`CatalogoMateriais`),
  interface do backend (`PortalBackend`) e implementação em memória (`MockPortalBackend`).
- `portal-treinamento/public/` — página, estilo e script do navegador.
- `portal-treinamento/content/` — `catalogo.json` e os textos dos materiais (Markdown).
- `tests/D15PortalTest.java` — testes das rotas.

## Como publicar um material

1. Coloque o arquivo de texto em `portal-treinamento/content/`.
2. Acrescente uma entrada em `catalogo.json` com `slug`, `titulo`, `tipo`, `status`,
   `formato` e `arquivo`.
3. O `slug` usa letras minúsculas sem acento, números e hífens (até 100 caracteres).
4. Só materiais com `status` igual a `PUBLICADO` aparecem no portal. O campo `arquivo` é o
   nome do arquivo dentro de `content/`, sem pastas, e nunca é enviado ao navegador.

## Como conferir o funcionamento

- A página lista os materiais publicados e mostra o texto ao clicar.
- Cada abertura do portal e cada material aberto geram um evento (`PORTAL_OPEN`,
  `MATERIAL_VIEW`, `VIDEO_VIEW`). Para testar a rota à mão, com o servidor ligado:

        curl -s -X POST -H 'Content-Type: application/json' \
          -d '{"tipoEvento":"PORTAL_OPEN"}' localhost:8085/api/treinamentos/eventos

  A resposta é `201` com o identificador do evento.

## Estado atual e limitações

- O launcher usa o `TreinamentoService` e o `DataSource` JDBC por padrão. A migration
  `015_d15_treinamento.sql` deve estar aplicada no banco configurado. O backend em memória só é
  usado quando `AUTOBOTZ_PORTAL_BACKEND=mock` for definido explicitamente.
- `GET /api/treinamentos/metricas` responde `401` sempre. A autorização administrativa
  depende da definição de sessão (Arthur e João). Não há painel de métricas.
- O texto dos materiais aparece como texto puro; o Markdown não é renderizado.
- O portal não está publicado na internet: funciona apenas em `localhost`.
- Os textos em `content/` são cópias dos arquivos de `docs/` do Higor; a manutenção da
  cópia ainda precisa ser combinada com ele.

## Segurança aplicada

- Servidor ligado só ao endereço local.
- O navegador nunca envia caminho de arquivo: o servidor resolve o conteúdo pelo catálogo e
  só lê dentro de `portal-treinamento/content/`.
- Slugs inválidos são recusados; materiais não publicados respondem `404`.
- Eventos exigem `Content-Type: application/json` e só aceitam material publicado.
- Nenhum dado pessoal, IP ou identificador de sessão é coletado.

## Testes de conteúdo e de persistência

Conteúdo (cópias em `content/` iguais aos textos oficiais em `docs/`):

    java -cp /tmp/portal-out D15ConteudoTest

Persistência dos eventos no MariaDB, **somente em banco isolado cujo nome termine em `_test`**
(o teste recusa qualquer outro). Variáveis: `AUTOBOTZ_TEST_DB_URL`, `AUTOBOTZ_TEST_DB_USER` e
`AUTOBOTZ_TEST_DB_PASSWORD`. A senha não deve ser versionada nem escrita no histórico do terminal.

    javac -sourcepath src -d /tmp/portal-out src/autobotz/portal/*.java tests/D15PersistenciaTest.java
    java -cp /tmp/portal-out:CAMINHO/mariadb-java-client-3.5.10.jar D15PersistenciaTest

## Abrir o portal pelo ERP (PortalAtalho)

O Swing e o console chamam `autobotz.portal.PortalAtalho.abrir()`. Ela inicia o servidor uma
única vez, em thread daemon, e abre o navegador. Nunca lança exceção: devolve um `Resultado`
com `Status` (`ABERTO`, `NAVEGADOR_INDISPONIVEL`, `PORTA_OCUPADA`, `CONTEUDO_INDISPONIVEL`,
`ERRO`) e a `url`. Os textos para o usuário vêm dos bundles da interface, não desta classe.
Teste: `tests/D15AtalhoTest.java`. O atalho inicia o launcher com backend JDBC padrão; prepare as
variáveis de conexão e aplique a migration antes de abrir o portal.

## Tipos de material

O catálogo aceita qualquer tipo, e a interface mostra um filtro por tipo. Hoje: `GUIA`, `FAQ`,
`ROTEIRO` e `COMUNICADO`. Só o tipo `VIDEO` gera `VIDEO_VIEW`; os demais geram `MATERIAL_VIEW`.
Um roteiro nunca deve ser apresentado como vídeo disponível.
