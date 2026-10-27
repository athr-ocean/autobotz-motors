# D13 — Contratos de fornecedores e ledger de integridade

Implementação da prova de conceito do Desafio 13, usando o cadastro de fornecedores do D11.

## Limite e dependências

- A branch compila contra `Fornecedor` e `FornecedorDAO.buscarPorId(Connection, int)` já presentes na `main`.
- A migração `013_d13_contratos_ledger.sql` depende da tabela `fornecedores(id)` criada pelo D11. A auditoria de 26/10 não encontrou a migração D11 no banco de instalação; não aplique a migração D13 até Alexandre/Adryan entregarem e integrarem a estrutura D11.
- A solução é um ledger local de registros com hashes SHA-256 encadeados. Não é uma rede blockchain, não tem consenso distribuído nem imutabilidade absoluta contra quem possa alterar simultaneamente o ledger e a âncora do contrato no banco.
- “Pagamento liberado” é somente um estado de demonstração. Nenhum pagamento é realizado. Eventos guardam apenas IDs, estados, valor e datas; não guardam dados pessoais nem documentos.

## Funcionamento

- `ContratoService` valida o fornecedor usando o DAO compartilhado, cria contrato em `RASCUNHO` e registra o primeiro evento na mesma transação.
- As transições são `RASCUNHO → EM_NEGOCIACAO → ASSINADO → EM_EXECUCAO → FINALIZADO`; cancelamento é permitido antes de um estado terminal.
- Cada mudança acrescenta um evento no `ledger_contratos`. O DAO expõe inserção e leitura, sem métodos de edição ou exclusão.
- O hash do registro inclui contrato, sequência, tipo, data/hora, hash do payload e hash anterior. A linha do contrato guarda a sequência e o hash final esperados, permitindo detectar alteração, lacuna e remoção inclusive do último evento.
- Cada evento inclui um retrato canônico de fornecedor, valor, vencimento, estado, validação da nota e estado de pagamento. A verificação compara o retrato final com a linha do contrato, e uma escrita posterior só é aceita quando o estado anterior coincide com o último evento.
- O alerta é registrado uma única vez quando o vencimento está entre hoje e os próximos 10 dias.
- A liberação simulada exige nota fiscal validada, vencimento atingido e contrato não cancelado/finalizado; processamento repetido não cria outra liberação.
- O serviço recebe um `Autorizador`; a integração Swing deve conectar as ações às permissões de RBAC aprovadas por Arthur.

## Banco

Aplicar em uma base isolada, depois da migração de fornecedores do D11 e antes das migrações posteriores que dependam do D13. A migração cria `contratos_fornecedor` e `ledger_contratos`; não altera `schema.sql`, que é consolidado por Arthur.

## Validação local

`D13LedgerIntegrityTest` é independente de banco e cobre hashes, alteração/remoção, âncora, janela de alerta, transições e condições do pagamento simulado. `D13JdbcIntegrationTest` cobre persistência, alerta idempotente, transições, NF, pagamento simulado e detecção de alteração em uma base isolada.

Para integração JDBC, configure `AUTOBOTZ_DB_URL` para uma base exclusiva chamada `autobotz_d13_test`, aplique primeiro a estrutura D11 e depois a migração 013. Não use banco de produção ou com dados pessoais.
