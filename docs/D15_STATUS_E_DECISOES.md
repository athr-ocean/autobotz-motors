# D15 — Status e decisões do portal de treinamento

Atualizado em 28/10/2026. Responsável: Mauro (portal e servidor). Conteúdo e métricas: Higor.
Execução e testes: `docs/D15_PORTAL.md`. Contrato das rotas: `docs/D15_CONTRATO_API.md`.

## Estado

- Branch `feature/d15-training-portal-ui`, Pull Request #24 em Draft.
- Implementado na branch: servidor HTTP local, interface web, catálogo, registro de eventos,
  adaptador `TreinamentoPortalBackend`, `PortalAtalho`, rotas de login/MFA/logout web, autorização server-side de métricas, testes e documentação.
- Adicionado: roteiro textual de vídeo de 30 minutos, identificado como roteiro e não como gravação; rascunho de e-mail e inventário honesto dos materiais.
- Alterado nesta atualização: o launcher e o atalho padrão passam a construir o backend persistente
  usando `TreinamentoService(ConexaoBanco.getDataSource())`, em vez do mock.
- Ainda pendente: PR #22 integrado, build e testes atuais, persistência em MariaDB isolado `_test`, teste ponta a ponta de login/MFA/logout e autorização 401/403/200 na base integrada. A integração usa o serviço D14 quando disponível e falha fechada caso contrário.
- Vídeo real, webinar realizado e fórum funcional não foram comprovados; o roteiro é apenas texto. O modo `portal` em `scripts/run.sh`, o atalho no `AppFrame` e os bundles são do Arthur.

## Decisões

1. Rotas, `slug` e formato de erro seguem o contrato com Higor.
2. O catálogo fica em `portal-treinamento/content/catalogo.json`. O nome do arquivo nunca é enviado
   ao navegador e a leitura fica restrita à pasta de conteúdo.
3. Os textos oficiais ficam em `docs/` (Higor). As cópias em `content/` devem permanecer
   sincronizadas, verificadas por `D15ConteudoTest`.
4. Servidor somente em localhost e sem framework.
5. Eventos não carregam sessão, IP nem dados pessoais.
6. `/api/treinamentos/metricas` valida a sessão no servidor D14: 401 sem sessão, 403 sem permissão e 200 apenas para ADMIN autorizado. Se o serviço D14 não estiver presente na base integrada, falha fechado com 401. Nunca aceitar papel administrativo enviado pelo cliente.
7. O backend padrão usa `TreinamentoPortalBackend` e `TreinamentoService`; o mock fica para testes.
   A integração depende de `ConexaoBanco.getDataSource()` do PR #22.
8. `PortalAtalho.abrir()` inicia o servidor uma vez em thread daemon e devolve um `Status` para
   a interface traduzir pelos bundles.
9. Testes de persistência só podem ser executados em banco isolado cujo nome termine em `_test`.

## Testes

Executor das três suítes sem banco: `bash scripts/test-d15.sh` (na raiz do repositório). A suíte `D15PersistenciaTest` só roda quando as variáveis de banco de teste estão definidas e recusa qualquer banco cujo nome não termine em `_test`.

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
| Confirmar integração do serviço de sessão D14 e política MFA | Arthur e João |
| Validar rotas de login/MFA/logout e métricas 401/403/200 ponta a ponta | Mauro + João |
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
- A autorização foi ligada ao serviço D14, mas o fluxo real de sessão depende da base integrada e ainda requer testes ponta a ponta.
- O roteiro de 30 minutos não é uma gravação; webinar e fórum não foram comprovados.
- Markdown básico é renderizado com segurança; links, tabelas e itálico ainda não são suportados. Portal apenas em localhost.
