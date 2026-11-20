# D3 - Concorrência: AutoBotz vs concorrente com IA generativa

AutoBotz Motors · Análise comparativa, lacunas e proposta de diferenciais

## 1. Objetivo

Comparar o AutoBotz Motors com um concorrente fictício que usa IA generativa para criar relatórios e dashboards, identificar o que falta no nosso ERP e propor melhorias viáveis, explicando como funcionariam e qual a prioridade de cada uma. Esta entrega é uma análise e proposta: não há implementação de código nem treinamento de modelo.

## 2. O concorrente fictício: AutoIA Pro

O enunciado define apenas que o concorrente usa IA generativa para criar relatórios e dashboards. Tudo o que está marcado como **hipótese** foi definido por nós para permitir a comparação e não é fato.

| Item | Descrição |
| --- | --- |
| Nome e público | AutoIA Pro, ERP para concessionárias |
| Dado pelo enunciado | Usa IA generativa para criar relatórios e dashboards |
| Hipótese | O usuário digita uma pergunta em texto ("quanto vendemos em outubro?") e recebe um relatório pronto |
| Hipótese | Monta gráficos e dashboards automaticamente a partir dos dados |
| Hipótese | Escreve um resumo em texto com os destaques do período |
| Limitações (hipótese) | Depende de internet, tem custo recorrente de API e pode errar números |

## 3. O AutoBotz hoje

O AutoBotz utiliza Java Swing na operação principal e MariaDB para persistência. Também possui um portal web local voltado ao treinamento. Os recursos abaixo foram conferidos na versão atual do projeto.

Módulos e recursos que já existem:

- **Veículos, clientes e vendas:** cadastro e registro, a base do sistema.
- **Relatórios:** faturamento por período, estoque e vendas por marca e modelo.
- **Dashboard:** indicadores gerenciais.
- **Consultas:** algumas consultas já disponíveis.
- **Compras, assinaturas, oficina e CRM.**
- **Auditoria e segurança.**
- **Idiomas:** português e inglês.

Ainda faltam, segundo o grupo: gráficos gerenciais mais avançados, filtros completos e recursos de IA.

## 4. Comparação

| Área | AutoIA Pro (hipótese) | AutoBotz hoje | Situação |
| --- | --- | --- | --- |
| Veículos, clientes e vendas | Sim | Sim | Equivalente |
| Relatórios | Gerados a partir de uma pergunta em texto | Relatórios fixos de faturamento por período, estoque e vendas por marca e modelo | Parcial: faltam relatórios gerados por pergunta |
| Dashboard | Gráficos montados automaticamente | Indicadores no dashboard | Parcial: faltam gráficos gerenciais avançados |
| Busca e filtros | Sim | Algumas consultas | Parcial: faltam filtros completos |
| Resumo em texto dos números | Sim | Não existe | Lacuna |
| Perguntas em linguagem natural | Sim | Não existe | Lacuna |
| Compras, assinaturas, oficina e CRM | Não definido no enunciado | Módulos existentes | Ponto forte do AutoBotz a destacar |
| Auditoria e segurança | Não definido no enunciado | Módulos existentes | Ponto forte; importante ao enviar dados a uma IA externa |
| Interface | Web (hipótese) | ERP operacional em Java Swing; portal web para treinamento | A operação principal continua voltada ao desktop |

## 5. Cenários de uso

Exemplos de situações de uma concessionária, comparados nos dois sistemas. A coluna do AutoIA Pro é simulada. A do AutoBotz segue a revisão do código feita pelo grupo.

| Situação | AutoIA Pro (simulado) | AutoBotz hoje | Conclusão |
| --- | --- | --- | --- |
| "Quanto faturamos no mês passado?" | Responde com o total e um gráfico | Relatório de faturamento por período existe; sem gráfico gerencial avançado | Parcial |
| "Qual marca ou modelo vende mais?" | Mostra ranking com gráfico | Relatório de vendas por marca e modelo existe | Coberto; falta o gráfico |
| "Quais veículos estão parados no estoque?" | Lista por tempo em estoque | Relatório de estoque existe; filtro por tempo em estoque não demonstrado no relatório atual | A confirmar |
| "Quero um resumo para a reunião de segunda" | Gera texto com os destaques da semana | Não existe | Lacuna |

## 6. Lacunas do AutoBotz

1. **Gráficos gerenciais avançados:** o dashboard mostra indicadores, mas faltam gráficos de tendência e comparação entre períodos.
2. **Filtros completos:** as consultas existem, mas não cobrem todas as combinações (período, marca, cliente, status).
3. **Sem resumo automático em texto:** os números aparecem, mas ninguém os interpreta.
4. **Sem perguntas em linguagem natural:** o gestor precisa navegar até o relatório certo.
5. **Operação principal no desktop:** os módulos operacionais são acessados pelo Swing; o portal web existente atende ao treinamento, não substitui as telas de gestão.

## 7. Melhorias propostas

As melhorias partem do que o AutoBotz já tem: nenhuma propõe refazer relatórios, dashboard ou consultas do zero. Custos são **estimativas relativas** (baixo, médio ou alto), não valores em reais. A prioridade combina impacto e esforço.

### Melhoria 1: Filtros completos nos relatórios existentes

- **Como funcionaria:** acrescentar filtros por período, marca, modelo, cliente e status aos relatórios e consultas que já existem.
- **Custo:** baixo, pois reaproveita as consultas atuais.
- **Prioridade:** **alta.** Melhora o que o gestor já usa e prepara os dados para gráficos e IA.

### Melhoria 2: Gráficos gerenciais no dashboard

- **Como funcionaria:** a partir dos indicadores e relatórios existentes, incluir gráficos de tendência de faturamento, vendas por marca e modelo e comparação entre períodos.
- **Custo:** médio.
- **Prioridade:** **média-alta.** Depende da Melhoria 1.

### Melhoria 3: Resumo automático com IA sobre os relatórios existentes

- **Como funcionaria:** o sistema pega os números de um relatório, envia a um serviço de IA por API e recebe um parágrafo com os destaques. Só dados agregados são enviados, sem dados pessoais de clientes, e o uso pode ser registrado na auditoria que já existe.
- **Custo:** médio, mais gasto recorrente com a API.
- **Prioridade:** **média.** Depende da Melhoria 1.

### Melhoria 4: Visão integrada dos módulos

- **Como funcionaria:** estender o resumo da Melhoria 3 para compras, assinaturas, oficina e CRM, gerando um resumo gerencial único com os módulos existentes.
- **Custo:** médio a alto.
- **Prioridade:** **média-baixa.** Depende da Melhoria 3.

### Melhoria 5: Perguntas em linguagem natural

- **Como funcionaria:** o gestor digita "quais carros estão parados há mais tempo?" e a IA escolhe uma consulta segura, já pronta, entre as que o sistema oferece. Isso evita que a IA gere comandos livres no banco, e as perguntas respeitam o controle de acesso existente.
- **Custo:** alto.
- **Prioridade:** **baixa.** Só faz sentido depois das anteriores.

### Melhoria 6: Previsão de vendas e estoque

- **Como funcionaria:** a partir do histórico de vendas, o sistema indica tendências e sugere reposição de estoque.
- **Custo:** alto.
- **Prioridade:** **baixa.**

## 8. Resumo e ordem sugerida

| Ordem | Melhoria | Custo | Prioridade |
| --- | --- | --- | --- |
| 1 | Filtros completos nos relatórios existentes | Baixo | Alta |
| 2 | Gráficos gerenciais no dashboard | Médio | Média-alta |
| 3 | Resumo automático com IA | Médio | Média |
| 4 | Visão integrada dos módulos | Médio a alto | Média-baixa |
| 5 | Perguntas em linguagem natural | Alto | Baixa |
| 6 | Previsão de vendas e estoque | Alto | Baixa |

A lógica é evoluir primeiro o que já existe (filtros e gráficos) e só depois acrescentar a camada de IA em cima dos relatórios e módulos atuais. Assim, o AutoBotz fecha as lacunas mais visíveis antes de investir em diferenciais mais caros, e aproveita pontos fortes que o concorrente do enunciado não menciona, como compras, oficina, CRM, auditoria e segurança.

## 9. O que veio do enunciado e o que é hipótese

- **Enunciado:** o concorrente é fictício e usa IA generativa para criar relatórios e dashboards.
- **Hipótese:** todo o resto do AutoIA Pro (perguntas em texto, resumos, interface web, limitações) e os cenários simulados.
- **Revisão do código pelo grupo:** os módulos, relatórios e indicadores atuais do AutoBotz e as lacunas da seção 6.
- **Estimativa:** custos e prioridades das melhorias.
