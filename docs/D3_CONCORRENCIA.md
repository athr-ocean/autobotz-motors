# D3 - Concorrência: AutoBotz vs concorrente com IA generativa

AutoBotz Motors · Análise comparativa, lacunas e proposta de diferenciais

## 1. Objetivo

Comparar o AutoBotz Motors com um concorrente fictício que usa IA generativa para criar relatórios e dashboards, identificar o que falta no nosso ERP e propor melhorias viáveis, explicando como funcionariam e qual a prioridade de cada uma. Esta entrega é uma análise e proposta: não há implementação de código nem treinamento de modelo.

## 2. O concorrente fictício: AutoIA Pro

O enunciado define apenas que o concorrente usa IA generativa para criar relatórios e dashboards. O restante abaixo é **suposição** para permitir a comparação.

| Item | Descrição |
| --- | --- |
| Nome e público | AutoIA Pro, ERP para concessionárias |
| Dado pelo enunciado | Usa IA generativa para criar relatórios e dashboards |
| Suposição | O usuário digita uma pergunta em texto ("quanto vendemos em outubro?") e recebe um relatório pronto |
| Suposição | Monta gráficos e dashboards automaticamente a partir dos dados |
| Suposição | Escreve um resumo em texto com os destaques do período |
| Limitações supostas | Depende de internet, tem custo recorrente de API e pode errar números |

## 3. O AutoBotz hoje

O AutoBotz Motors é um sistema simples de gerenciamento de concessionária em Java, com interface gráfica feita em Java Swing. Funcionalidades listadas:

- Cadastro, edição, listagem e exclusão de veículos.
- Cadastro e listagem de clientes.
- Registro de vendas.
- Menu em dois idiomas (português e inglês).

## 4. Comparação

| Funcionalidade | AutoIA Pro (suposição) | AutoBotz hoje | Lacuna |
| --- | --- | --- | --- |
| Cadastro de veículos | Sim | Sim (CRUD completo, conforme README) | Nenhuma |
| Cadastro de clientes | Sim | Cadastro e listagem (conforme README) | Nenhuma |
| Registro de vendas | Sim | Sim | Nenhuma |
| Armazenamento | Banco de dados | Banco de dados | Nenhuma |
| Relatório de vendas | Automático, por pergunta | Não | Alta |
| Dashboard com gráficos | Automático | Não | Alta |
| Busca e filtros (período, marca, cliente) | Sim | Não | Média |
| Interface | Provavelmente web | Java Swing (desktop) | Média |

## 5. Cenários de uso

Situações reais de uma concessionária, respondidas pelos dois sistemas. A coluna do AutoIA Pro é simulada; a do AutoBotz deve ser confirmada no código.

| Situação | AutoIA Pro (simulado) | AutoBotz hoje |
| --- | --- | --- |
| "Quanto vendemos no mês passado?" | Responde com o total e um gráfico | Lista vendas; o gestor soma manualmente |
| "Qual marca vende mais?" | Mostra ranking por marca | Sem relatório; exige contagem manual&#32; |
| "Quais veículos estão parados no estoque?" | Lista por tempo em estoque | Lista todos os veículos, sem filtro por tempo |
| "Quero um resumo para a reunião de segunda" | Gera texto com destaques da semana | Não existe |

## 6. Lacunas do AutoBotz

1. **Sem relatórios:** o gestor precisa somar e cruzar dados manualmente.
2. **Sem dashboard:** não há visão geral do negócio de relance.
3. **Sem busca e filtros:** não dá para consultar por período, marca ou cliente.
4. **Sem resumo automático:** nenhuma interpretação dos números.
5. **Interface desktop (Swing):** roda só como aplicativo no computador, sem acesso pelo navegador.

## 7. Melhorias propostas

Custos são **estimativas relativas** (baixo, médio ou alto), não valores em reais. A prioridade combina impacto e esforço.

### Melhoria 1: Relatórios fixos

- **Como funcionaria:** um item "Relatórios" no menu pede o período e mostra total vendido, veículo mais vendido, vendas por cliente e estoque atual, usando consultas ao banco que já existe.
- **Custo:** baixo, pois os dados já estão no banco.
- **Prioridade:** **alta.** É a base para qualquer recurso de IA, que precisa de números organizados para resumir.

### Melhoria 2: Busca e filtros

- **Como funcionaria:** filtros por período, marca, faixa de preço e cliente nas listagens.
- **Custo:** baixo.
- **Prioridade:** **alta.**

### Melhoria 3: Dashboard simples

- **Como funcionaria:** uma tela com gráficos de vendas por mês e estoque por marca, alimentados pelos relatórios da Melhoria 1.
- **Custo:** médio.
- **Prioridade:** **média-alta.**

### Melhoria 4: Resumo automático com IA

- **Como funcionaria:** o sistema pega os números de um relatório, envia a um serviço de IA por API e recebe um parágrafo com os destaques. Os dados pessoais de clientes não são enviados.
- **Custo:** médio, mais gasto recorrente com a API.
- **Prioridade:** **média.** Depende das Melhorias 1 e 3.

### Melhoria 5: Perguntas em linguagem natural

- **Como funcionaria:** o gestor digita "quais carros estão parados há mais tempo?" e a IA escolhe uma consulta segura, já pronta, entre as disponíveis. Isso evita que a IA gere comandos livres no banco.
- **Custo:** alto.
- **Prioridade:** **baixa.** Só faz sentido depois das anteriores.

### Melhoria 6: Previsão de vendas e estoque

- **Como funcionaria:** a partir do histórico de vendas, o sistema indica tendências e sugere reposição de estoque.
- **Custo:** alto.
- **Prioridade:** **baixa.**

## 8. Resumo e ordem sugerida

| Ordem | Melhoria | Custo | Prioridade |
| --- | --- | --- | --- |
| 1 | Relatórios fixos | Baixo | Alta |
| 2 | Busca e filtros | Baixo | Alta |
| 3 | Dashboard simples | Médio | Média-alta |
| 4 | Resumo automático com IA | Médio | Média |
| 5 | Perguntas em linguagem natural | Alto | Baixa |
| 6 | Previsão de vendas e estoque | Alto | Baixa |

A lógica é construir primeiro a base de dados organizada (relatórios e filtros) e só depois acrescentar a camada de IA em cima dela. Assim, o AutoBotz alcança o concorrente nos recursos essenciais antes de investir em diferenciais mais caros.
