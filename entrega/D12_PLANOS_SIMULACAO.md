# D12 — Crise econômica: planos, churn e simulação financeira

**AutoBotz Motors — Equipe B · 29/10/2026.** Relatório e proposta didáticos da segunda rodada.

## Análise do problema

O enunciado pede uma avaliação de cancelamentos e receita recorrente perdida nos seis meses anteriores, com motivos, segmentos e funcionalidades utilizadas. **Não foi fornecida uma carteira real de clientes assinantes nem um histórico verificado de cancelamentos.** Assim, não há base para afirmar que determinado segmento teve maior churn. O diagnóstico abaixo é um exercício hipotético, não uma consulta ao banco ou uma pesquisa real.

### Cenário de cancelamentos — exclusivamente SIMULADO

Para demonstrar a estrutura do relatório, suponha 120 clientes ativos no início do semestre e 12 cancelamentos ao longo de seis meses, distribuídos assim:

| Mês didático | Cancelamentos fictícios | Principal motivo ilustrativo |
|---|---:|---|
| Abril/2026 | 2 | custo mensal elevado |
| Maio/2026 | 1 | funcionalidades pouco utilizadas |
| Junho/2026 | 3 | custo mensal elevado |
| Julho/2026 | 2 | necessidade de versão básica |
| Agosto/2026 | 2 | custo mensal elevado |
| Setembro/2026 | 2 | funcionalidades pouco utilizadas |
| **Total** | **12** | **registros inventados para análise** |

Se todos os contratos de exemplo custassem R$ 100/mês, a perda de receita mensal recorrente associada aos 12 cancelamentos seria **R$ 1.200** após os cancelamentos; 12/120 = **10% de churn acumulado hipotético**. Isto não equivale a churn real da empresa nem demonstra a distribuição de segmentos. Para medir de verdade, seriam necessárias datas de início e cancelamento, preço contratado, porte/segmento e módulos de cada assinatura.

O código oferece `AssinaturaService.calcularChurn`, `calcularReceitaPerdida` e `calcularMRR`; a persistência de assinaturas e eventos está em `AssinaturaDAO` e na migration 012. O teste `AssinaturaDAOJdbcTest` passou com MariaDB descartável, e o teste de serviço verificou o cálculo de churn e a simulação.

## Proposta de planos e módulos

| Oferta didática | Valor mensal simulado | Características |
|---|---:|---|
| Básico | R$ 80 | preço inferior ao plano atual; conjunto essencial a definir comercialmente |
| Atual | R$ 100 | referência de custo para o modelo de comparação |
| Premium | R$ 150 | preço superior; escopo de funcionalidades a negociar |
| Módulo opcional CRM | + R$ 20 | acréscimo simulado |
| Módulo opcional Vendas Avançadas | + R$ 30 | acréscimo simulado |

Os preços acima são **parâmetros fictícios editáveis no simulador**, não uma tabela comercial efetivamente implantada. O sistema persiste `planos_assinatura`, `modulos_opcionais`, `assinaturas_clientes`, vínculos e eventos, conforme a migration 012. Uma migração real de clientes demandaria aprovação comercial e autorização de cada transição.

## Simulação 30% / 50% / 20% no ERP

A entrada Swing **Simulador de planos** aparece para ADMIN e VENDEDOR, usa `AssinaturaService.simularMigracao` e não grava contas fictícias. Permite informar quantidade de clientes, preços atual/básico/premium e módulos opcionais. Mostra distribuição, MRR anterior e MRR projetado.

No exemplo automatizado, suponha **100 assinaturas fictícias**, todas com plano atual de R$ 100 e módulo CRM de R$ 20:

| Grupo | Participação | Clientes fictícios | MRR resultante |
|---|---:|---:|---:|
| Migram para Básico (R$ 80 + CRM R$ 20) | 30% | 30 | R$ 3.000 |
| Permanecem no Atual (R$ 100 + CRM R$ 20) | 50% | 50 | R$ 6.000 |
| Migram para Premium (R$ 150 + CRM R$ 20) | 20% | 20 | R$ 3.400 |
| **Total projetado** | **100%** | **100** | **R$ 12.400** |

MRR anterior: 100 × R$ 120 = **R$ 12.000**. MRR projetado: **R$ 12.400** (**+R$ 400; +3,33%**), **se todos permanecerem assinantes**. Se ocorrer churn adicional, o MRR real será menor. Por exemplo, mantendo a composição e removendo 10% dos contratos em cada grupo, o MRR ilustrativo seria R$ 11.160, inferior à base inicial. Isso mostra que preço sozinho não garante viabilidade.

A simulação demonstra coerência aritmética, **não comprova viabilidade financeira completa**: faltam custo por cliente, impostos, inadimplência, retenção real, margem e aquisição de novos clientes. O teste `D12SimuladorUITest` verifica proporções, MRR e rejeição de entradas inválidas.

## Pesquisa simulada e atendimento

**Roteiro de feedback hipotético (não enviado):**

- “O preço fixo está alto para meu uso atual.”
- “Pago por funções que minha empresa não utiliza.”
- “Gostaria de contratar só o CRM e os relatórios.”
- “Preciso comparar a diferença entre Básico e Premium antes de migrar.”

Classificar eventuais respostas **reais** nas categorias `PRECO_ALTO`, `BAIXA_UTILIZACAO`, `PLANO_SIMPLES` e `OUTROS`, preservando consentimento e dados pessoais.

### Comunicação proposta — texto para aprovação, ainda não enviado

**Assunto: Novas opções de assinatura do AutoBotz Motors**

“Olá. Estamos estudando um modelo mais flexível para que cada empresa escolha os módulos de que precisa. A proposta inclui planos com valores diferentes e recursos opcionais. Antes de qualquer mudança, apresentaremos o preço final, as funcionalidades e as regras de transição. Nenhum contrato será alterado apenas por esta simulação.”

### Guia breve para a equipe de suporte

1. Identificar a finalidade do ERP e as funções realmente utilizadas pelo cliente.
2. Comparar preços e módulos no simulador didático; informar que são exemplos, não oferta contratual.
3. Verificar se o plano proposto mantém as funções indispensáveis.
4. Explicar diferenças de preço, cancelamento e condições antes de qualquer alteração real.
5. Registrar eventuais objeções, com autorização e sem expor dados sensíveis.

## Evidências e limitações

- `src/autobotz/service/AssinaturaService.java`: cálculos de MRR, churn, receita perdida e migração.
- `src/autobotz/ui/swing/SimuladorPlanosPanel.java`: interface visual sem alterações de banco.
- `tests/AssinaturaServiceTest.java`: cálculo determinístico.
- `tests/AssinaturaDAOJdbcTest.java`: transações e persistência, aprovada no banco `_test`.
- `tests/D12SimuladorUITest.java`: comportamento e validação do novo simulador.

**Não há dados comerciais reais, pesquisa efetivamente enviada, economia comprovada ou carteira de clientes que sustente afirmações de desempenho no mercado.**
