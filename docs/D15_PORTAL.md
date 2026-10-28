# D15 — Portal de treinamento (como executar e verificar)

Responsável: Mauro. Conteúdo e métricas: Higor. Contrato das rotas: `docs/D15_CONTRATO_API.md`.

## O que é

Uma central de treinamento web, separada do Swing e do console. Um servidor leve do JDK
(`com.sun.net.httpserver`, sem framework) serve a página e três rotas de API. O portal roda
somente em `localhost`.

## Requisitos

- JDK 21 (o mesmo do projeto).
- Executar sempre a partir da raiz do repositório.
- Banco configurado pelas variáveis `AUTOBOTZ_DB_URL`, `AUTOBOTZ_DB_USER` e
  `AUTOBOTZ_DB_PASSWORD`. Não versionar nem expor a senha.

## Executar

    mkdir -p /tmp/portal-out
    javac -sourcepath src -d /tmp/portal-out src/autobotz/portal/*.java
    java -cp /tmp/portal-out autobotz.portal.PortalLauncher

Depois abra `http://127.0.0.1:8085`. Para usar outra porta: `AUTOBOTZ_PORTAL_PORT=8090`.
Para usar outra pasta do portal: `AUTOBOTZ_PORTAL_DIR=/caminho/portal-treinamento`.
Para parar, `Ctrl+C`. O modo `portal` no `scripts/run.sh` ainda depende do trabalho de integração do Arthur.

O launcher padrão usa `TreinamentoPortalBackend` e `TreinamentoService` com o
`ConexaoBanco.getDataSource()`: os eventos não devem ser considerados persistidos até a
integração com o PR #22 e o teste de integração em banco isolado serem confirmados.

## Testes das rotas

    javac -sourcepath src -d /tmp/portal-out src/autobotz/portal/*.java tests/D15PortalTest.java
    java -cp /tmp/portal-out D15PortalTest

Os testes de rota não usam banco: criam uma pasta temporária com catálogo de teste e backend em memória.

## Estrutura

- `src/autobotz/portal/` — servidor (`PortalServer`), catálogo (`CatalogoMateriais`),
  interface do backend (`PortalBackend`), backend em memória para testes (`MockPortalBackend`)
  e adaptador persistente (`TreinamentoPortalBackend`).
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
- Cada abertura do portal e cada material aberto enviam um evento (`PORTAL_OPEN`,
  `MATERIAL_VIEW`, `VIDEO_VIEW`). Para testar a rota à mão, com o servidor ligado:

        curl -s -X POST -H 'Content-Type: application/json' \
          -d '{"tipoEvento":"PORTAL_OPEN"}' 127.0.0.1:8085/api/treinamentos/eventos

  Com banco e migration corretamente configurados, a resposta esperada é `201` com o identificador do evento.

## Estado atual e limitações

- O launcher e o atalho do ERP foram alterados para usar o adaptador persistente. A ligação efetiva
  depende de o PR #22 estar integrado na base usada para compilar e de a configuração JDBC funcionar.
  Validar a persistência real com `D15PersistenciaTest` em banco isolado `_test` antes de afirmar
  que o fluxo integrado foi aprovado.
- `GET /api/treinamentos/metricas` responde `401` até haver uma sessão ADMIN validada no servidor
  (definição de Arthur e João). Não há painel de métricas; não contornar a autenticação.
- A interface renderiza Markdown básico com elementos DOM seguros: títulos, parágrafos, listas, negrito e código em linha. Links, tabelas, itálico e extensões Markdown ainda não são suportados.
- O portal não está publicado na internet: funciona apenas em `localhost`.
- Os textos em `content/` são cópias dos arquivos de `docs/` do Higor; o teste de conteúdo deve
  continuar a confirmar que as cópias estão sincronizadas.

## Segurança aplicada

- Servidor ligado só ao endereço local.
- O navegador nunca envia caminho de arquivo: o servidor resolve o conteúdo pelo catálogo e
  só lê dentro de `portal-treinamento/content/`.
- Slugs inválidos são recusados; materiais não publicados respondem `404`.
- Eventos exigem `Content-Type: application/json` e só aceitam material publicado.
- Nenhum dado pessoal, IP ou identificador de sessão é coletado.
- Métricas permanecem negadas até existir autorização administrativa server-side.

## Executar as suítes D15

Na raiz do repositório, execute:

    bash scripts/test-d15.sh

O script compila o portal e executa `D15PortalTest`, `D15ConteudoTest` e `D15AtalhoTest`
sem banco. Se as três variáveis `AUTOBOTZ_TEST_DB_URL`, `AUTOBOTZ_TEST_DB_USER` e
`AUTOBOTZ_TEST_DB_PASSWORD` estiverem definidas, também executa `D15PersistenciaTest`;
essa suíte recusa URLs cujo banco não termine em `_test`. O driver MariaDB pode ser indicado
por `AUTOBOTZ_JDBC_JAR`. O script não cria banco nem aplica testes fora do banco explicitamente
configurado.

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
O backend padrão usa `TreinamentoPortalBackend` com o serviço JDBC; os testes podem injetar
`MockPortalBackend` para não depender do banco. Teste: `tests/D15AtalhoTest.java`.

## Tipos de material

O catálogo aceita qualquer tipo, e a interface mostra um filtro por tipo. Hoje: `GUIA`, `FAQ`,
`ROTEIRO` e `COMUNICADO`. Só o tipo `VIDEO` gera `VIDEO_VIEW`; os demais geram `MATERIAL_VIEW`.
Um roteiro nunca deve ser apresentado como vídeo disponível.
