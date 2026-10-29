# Relatório de Qualidade, Regressão e TI Verde (D11)

## 1. Visão Geral
Este documento apresenta os resultados da automação de testes e da integração contínua (CI) do **Módulo de Compras (D11)** do AutoBotz ERP.

## 2. Medição de Tempos de Execução
Para avaliar o ganho de eficiência, comparou-se o fluxo de compras e regressão no cenário manual versus a execução automatizada:

* **Teste Manual (Interface Swing):** ~20 a 30 minutos por ciclo completo de testes (cadastro de fornecedor, criação de orçamento, solicitação, aprovação e verificação visual no banco).
* **Teste Automatizado Local (`scripts/test.sh`):** ~3 a 5 segundos (cobrindo a regressão legada e os 10 cenários do D11).
* **Pipeline de CI (GitHub Actions):** ~15 a 30 segundos (incluindo subida do container MariaDB, compilação, linting e suítes D11 e D15).
* **Redução no Tempo de Feedback:** Maior que **98%**, permitindo validações imediatas a cada alteração no código.

## 3. Garantias de Qualidade e Integridade Transacional
* **Isolamento de Segurança:** O script de teste valida rigorosamente se a URL do banco possui o sufixo `_test` (`autobotz_db_test`), prevenindo alterações acidentais em ambiente de produção ou desenvolvimento.
* **Integridade de Transação (Rollback):** Validação automatizada do mecanismo de *rollback* no `ComprasService.java`. Em caso de falhas na camada DAO durante a geração de pedidos, os orçamentos e saldos permanecem intactos.
* **Zero Regressão:** Integração com a suíte `CoreRegression.java`, garantindo que novos desenvolvimentos não quebrem as funcionalidades dos módulos legados (D6-D10).

## 4. Sustentabilidade e TI Verde (Green IT)
* **Premissa / Estimativa Calculada:** A execução em ambiente *headless* (sem interface gráfica) elimina o consumo computacional de renderização de telas Swing.
* **Eficiência de Recursos:** Redução do tempo de execução de minutos para segundos diminui diretamente o ciclo de atividade de CPU/GPU, otimizando o consumo energético tanto em máquinas de desenvolvimento quanto nos runners do GitHub Actions.