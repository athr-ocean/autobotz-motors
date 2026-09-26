# Validação

Registro da validação do working tree em `integration/final`, baseada em `eeac6be`. Nenhum resultado abaixo representa uma inspeção visual do Swing.

## Build e compilação

- `bash scripts/run.sh build` — passou.
- `javac -Xlint:all` sobre `src/autobotz` com MariaDB Connector/J 3.5.10 — passou, sem warnings.
- `tests/CoreRegression.java` foi compilado e executado em `autobotz_core_codex_20260926_rerun_test`, criado exclusivamente para teste e inicializado com o conteúdo de `schema.sql` sem modificar o arquivo.
- `git diff --check` — passou.

## Regressão de banco

`CoreRegression` passou: conexão JDBC, paridade dos 358 bundles, autenticação válida/inválida, permissões de ADMIN/VENDEDOR, criação e leitura de clientes, duas vendas, rejeição de revenda, OS com dois serviços diferentes, JOIN dos dois itens, total bruto 350, garantia com desconto 200 e cobrança 150, garantia expirada e data futura, CRM em PT-BR/EN-US, anonimização preservando referências, isolamento e atualização de projetos, datas localizadas e atualização de cliente.

Uma verificação complementar temporária, executada apenas no mesmo banco de teste, passou em duplicidade de CPF sem alterar o outro cliente, ID inválido/inexistente, status ativo/inativo, anonimização dos dados pessoais, CRM sem vendas, preço de serviço NaN/infinito/zero, valores de venda NaN/infinito/data futura, mudança do veículo para vendido, revenda, rollback após falha forçada no INSERT de venda, quantidade/preço inválidos em item de OS, dois membros por projeto com isolamento, relatórios com dados reais e consulta de auditoria.

Uma verificação adicional com cliente e venda próprios no banco de teste confirmou que `CRMService.consultarHistorico()` retorna exatamente uma venda, o total correto e somente registros do cliente consultado.

O console foi executado autenticando como ADMIN do banco de teste. Foram percorridos Clientes, Vendas, Oficina, Relatórios, Projetos e CRM; a OS exibiu os dois itens e o cálculo retornou 350 bruto, 200 de desconto e 150 a cobrar. Uma venda foi registrada pelo fluxo `VendaService` e a revenda foi rejeitada. Nenhuma dessas operações foi executada no banco normal.

O modo de diagnóstico de `MigracaoMembrosProjeto`, sem `--apply`, retornou “FK presente; vínculos válidos” no banco de teste. A migração não foi aplicada.

## Limites

- A autenticação Swing, a jornada de telas e os fluxos visuais continuam pendentes; esta validação não altera o freeze da camada Swing.
- A regressão não foi executada no banco normal configurado localmente.
- A consulta de auditoria foi executada sem erro; o teste não exige registros de auditoria artificiais.
