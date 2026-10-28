# D15 — Status e decisões do portal de treinamento

Atualizado em 28/10/2026. Responsável: Mauro (portal e servidor). Conteúdo e métricas: Higor.
Execução e testes: `docs/D15_PORTAL.md`. Contrato das rotas: `docs/D15_CONTRATO_API.md`.

## Estado

- Branch `feature/d15-training-portal-ui`, Pull Request #24 em Draft.
- Implementado na branch: servidor HTTP local, interface web, catálogo, registro de eventos,
  adaptador `TreinamentoPortalBackend`, `PortalAtalho`, testes e documentação.
- Alterado nesta atualização: o launcher e o atalho padrão passam a construir o backend persistente
  usando `TreinamentoService(ConexaoBanco.getDataSource())`, em vez do mock.
- Ainda pendente: confirmar integração com o PR #22 na base de compilação e executar build/testes
  atualizados; `/metricas` permanece negada até sessão ADMIN server-side; modo `portal` no
  `scripts/run.sh`, atalho no `AppFrame` e bundles são do Arthur; painel de métricas depende de
  autenticação acordada.

## Decisões

1. Rotas, `slug` e formato de erro seguem o contrato com Higor.
2. O catálogo fica em `portal-treinamento/content/catalogo.json`. O nome do arquivo nunca é enviado
   ao navegador e a leitura fica restrita à pasta de conteúdo.
3. Os textos oficiais ficam em `docs/` (Higor). As cópias em `content/` devem permanecer
   sincronizadas, verificadas por `D15ConteudoTest`.
4. Servidor somente em localhost e sem framework.
5. Eventos não carregam sessão, IP nem dados pessoais.
6. `/api/treinamentos/metricas` continua respondendo `401` até haver sessão ADMIN validada no
   servidor. Não aceitar papel ou token administrativo fornecido pelo cliente.
7. O backend padrão usa `TreinamentoPortalBackend` e `TreinamentoService`; o mock fica para testes.
   A integração depende de `ConexaoBanco.getDataSource()` do PR #22.
8. `PortalAtalho.abrir()` inicia o servidor uma vez em thread daemon e devolve um `Status` para
   a interface traduzir pelos bundles.
9. Testes de persistência só podem ser executados em banco isolado cujo nome termine em `_test`.

## Testes

Os resultados anteriores registrados para as suítes D15 pertencem à versão anterior do código.
Após esta atualização, executar novamente:
- `D15PortalTest` (sem banco).
- `D15ConteudoTest` (sincronização de materiais).
- `D15AtalhoTest` (entrada pelo ERP, sem abrir navegador real).
- `D15PersistenciaTest` (MariaDB isolado `_test`, com variáveis de teste).

Não afirmar que o build ou as suítes da nova versão passaram até serem executados.

## Ordem de integração sugerida

PR #22 (DataSource) → PR #24 (portal) → PR do Arthur (`run.sh`, `AppFrame` e bundles).

## Pendências e responsáveis

| Pendência | Responsável |
|---|---|
| Integrar o PR #22 e confirmar DataSource na base de build | Arthur / equipe |
| Revisar o PR #24 | Arthur e Higor |
| `run.sh`, atalho no `AppFrame` e chaves PT-BR/EN-US | Arthur |
| Definir sessão ADMIN para `/metricas` | Arthur e João |
| Executar novamente build e suítes D15 | Mauro |
| Criar painel de métricas após decisão de autenticação | Mauro |
| Roteiro de vídeo e materiais de transição | Higor e Mauro |
| Conferir documentos de integração e métricas contra o contrato | Higor |
| Incluir suítes D15 no CI | Adryan |
| Clone limpo, build e regressão | equipe |

## Evidências para a entrega

- Captura do portal no navegador (lista de materiais e material aberto).
- Saída das quatro suítes após a integração.
- Guia rápido e roteiro de vídeo revisados.
- URL do PR #24 e lista de commits.
- Dados simulados sempre identificados como simulados; nenhuma URL pública declarada.

## Limitações declaradas

- A persistência ainda precisa ser confirmada com o PR #22 integrado e teste em banco isolado.
- `/metricas` permanece negada por falta de sessão ADMIN.
- Markdown aparece como texto puro; portal apenas em localhost.
