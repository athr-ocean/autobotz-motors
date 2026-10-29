# D15 — Status e decisões do portal de treinamento

Atualizado em 28/10/2026. Responsável: Mauro (portal e servidor). Conteúdo e métricas: Higor.
Execução e testes: `docs/D15_PORTAL.md`. Contrato das rotas: `docs/D15_CONTRATO_API.md`.

## Estado

- Branch `feature/d15-training-portal-ui`, localmente 10 commits atrás de `origin/main` (`13b32f2`). Pull Request #24, em Draft. O PR #29 do Arthur está aberto e empilhado sobre esta branch.
- Pronto: servidor HTTP em localhost, interface web, catálogo de materiais, registro de eventos,
  adaptador do `TreinamentoService`, `PortalAtalho` para o ERP, contrato e documentação.
- Em integração local: `PortalLauncher` usa o backend JDBC por padrão e só usa mock quando
  `AUTOBOTZ_PORTAL_BACKEND=mock` é definido. O serviço valida a conexão antes de iniciar.
- Não pronto: autenticação HTTP de login/MFA/logout no portal, `/metricas` (segue 401), página de
  métricas e validação da persistência no launcher com banco `_test`.

## Decisões

1. Rotas, `slug` e formato de erro seguem o contrato combinado com o Higor.
2. O catálogo fica em `portal-treinamento/content/catalogo.json`. O nome do arquivo de cada
   material nunca é enviado ao navegador, e a leitura fica restrita a `content/`.
3. Os textos oficiais ficam em `docs/` (Higor). As cópias em `content/` vão no mesmo commit, e o
   `D15ConteudoTest` falha se as duas versões divergirem.
4. Servidor só em localhost e sem framework, como o plano pede.
5. Eventos não carregam sessão, IP nem dados pessoais.
6. `/api/treinamentos/metricas` responde 401 até existir uma sessão ADMIN reconhecida pelo portal.
   Mecanismo em discussão com Arthur e João; token em query string foi descartado.
7. `PortalBackend` espelha o `TreinamentoService`. Há uma implementação em memória e um adaptador
   para o serviço real. O launcher agora usa `ConexaoBanco.getDataSource()` por padrão; a API do
   `DataSource` também está no PR #22, ainda aberto, e precisa ser consolidada na branch antes do merge.
8. `PortalAtalho.abrir()` sobe o servidor uma vez, em thread daemon, nunca lança exceção e devolve
   um `Status` sem texto, para a interface traduzir pelos bundles.
9. Testes em Java puro, com código de saída. O teste de persistência só roda em banco cujo nome
   termine em `_test` e recusa qualquer outro.

## Testes executados (JDK 21)

| Suíte | Casos | Banco | Resultado conhecido |
|---|---|---|---|
| `D15PortalTest` | 28 | não | passou |
| `D15ConteudoTest` | 4 | não | passou |
| `D15AtalhoTest` | 7 | não | passou |
| `D15PersistenciaTest` | 11 | MariaDB local, banco `autobotz_d15_test` | passou |

A migration `015_d15_treinamento.sql` foi aplicada duas vezes no banco de teste sem erro.
Nenhuma suíte foi executada ainda no CI, e a regressão `CoreRegression` não foi rodada por este módulo.
As mudanças locais de 28/10 que ligam `PortalLauncher` ao banco ainda não foram validadas.

## Ordem de merge sugerida

Atualizar a branch com a `main` atual → consolidar o `DataSource` do PR #22 e a integração do PR #29
→ validar eventos no banco `_test` → concluir o contrato HTTP de sessão com João → pedir revisão e
retirar o PR #24 do Draft.

## Pendências

| Pendência | Responsável |
|---|---|
| Consolidar o `DataSource` do PR #22, que segue aberto | Arthur e Mauro |
| Atualizar a branch D15 com a `main` que recebeu o PR #25 | Mauro |
| Integrar o modo `portal`, atalho Swing e bundles do PR #29, ainda aberto | Arthur e Mauro |
| Adicionar login/MFA/logout HTTP e autorização ADMIN para `/metricas` usando sessão server-side | Mauro, João e Arthur |
| Validar persistência do launcher e migration em banco `_test` | Mauro |
| Página de métricas no portal | Mauro, após a regra de sessão |
| Roteiro de vídeo e materiais de transição no catálogo | Higor e Mauro |
| Conferir `README_D15_INTEGRACAO.md` e `docs/D15_METRICAS.md` contra o contrato | Higor |
| Incluir as suítes D15 no CI | Adryan |
| Clone limpo, build e regressão até 29/10 | equipe |

## Evidências a guardar para a entrega

- Captura do portal no navegador (lista de materiais e um material aberto).
- Saída das quatro suítes de teste.
- Guia rápido e roteiro de vídeo do Higor.
- O endereço do PR #24 e a lista de commits da branch.
- Dados simulados, quando houver, rotulados como simulados. Nenhuma URL pública é declarada.

## Limitações declaradas

- Eventos do launcher só em memória até o PR #22.
- `/metricas` negada por falta de sessão ADMIN.
- Markdown exibido como texto puro; portal apenas em localhost.
