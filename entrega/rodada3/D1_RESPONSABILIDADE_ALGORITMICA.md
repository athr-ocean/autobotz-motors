# D1 — Mudanças regulatórias: responsabilidade algorítmica

**Projeto:** AutoBotz Motors — Equipe B  
**Responsável:** João  
**Rodada:** 3ª rodada — novembro de 2026  
**Tipo de entrega:** análise regulatória, avaliação de impacto no ERP e proposta de governança, auditoria e explicabilidade.

## 1. Objetivo e limite do material recebido

Esta entrega analisa como o AutoBotz Motors deve tratar decisões produzidas ou apoiadas por regras automatizadas. O objetivo é permitir que a equipe identifique o que o sistema decide, por que decide, quais dados utiliza, quais pessoas podem ser afetadas e como uma decisão pode ser contestada e revisada.

**Limite documental:** a imagem recebida com a divisão de tarefas identifica a atividade como “Lei de Responsabilidade Algorítmica”, mas não contém o texto integral do enunciado, o número da norma, a jurisdição ou seus artigos. Por isso, este documento não inventa artigos nem atribui obrigações específicas a uma lei cujo texto não foi disponibilizado. A análise utiliza o direito brasileiro vigente e propostas legislativas oficiais relacionadas ao tema. Caso o enunciado completo apresente uma norma fictícia ou uma versão específica de outro país, seus dispositivos deverão ser confrontados com esta análise antes da entrega definitiva.

## 2. Identificação da referência regulatória

### 2.1 Brasil: LGPD como base jurídica vigente

A Lei nº 13.709/2018 (Lei Geral de Proteção de Dados Pessoais — LGPD) é a principal referência brasileira já vigente para decisões automatizadas que utilizam dados pessoais.

O art. 6º exige que o tratamento observe, entre outros, os princípios de finalidade, adequação, necessidade, qualidade dos dados, transparência, segurança, prevenção, não discriminação e responsabilização/prestação de contas. Para o AutoBotz, isso significa que uma regra automatizada não deve usar dados sem finalidade definida, nem produzir resultados sem possibilidade de demonstrar como a organização controla seus riscos.

O art. 20 garante ao titular o direito de solicitar revisão de decisões tomadas unicamente com base em tratamento automatizado de dados pessoais que afetem seus interesses. O dispositivo também exige, quando solicitado, informações claras e adequadas sobre os critérios e procedimentos usados. Se essas informações não forem fornecidas sob justificativa de segredo comercial ou industrial, a ANPD poderá auditar aspectos discriminatórios do tratamento automatizado.

Há uma distinção importante: o texto vigente do art. 20 assegura o direito de solicitar revisão, mas não deve ser descrito como se determinasse, em todos os casos, que a revisão seja necessariamente feita por uma pessoa. A revisão humana é recomendada nesta proposta como salvaguarda de governança, sobretudo para decisões de maior impacto.

A LGPD não transforma toda validação de software em decisão automatizada sujeita automaticamente ao art. 20. É necessário avaliar se há tratamento de dados pessoais, se a decisão é tomada unicamente com base nesse tratamento e se afeta interesses do titular. Mesmo quando o art. 20 não incidir, os princípios do art. 6º continuam relevantes para o tratamento de dados pessoais.

### 2.2 Brasil: PL nº 2.338/2023 — proposta de marco legal de IA

O PL nº 2.338/2023, conhecido como proposta de Marco Legal da Inteligência Artificial, não deve ser apresentado como lei já vigente. Na consulta realizada em **17/11/2026**, a ficha oficial da Câmara dos Deputados ainda indicava a proposição aguardando parecer do relator na Comissão Especial sobre Inteligência Artificial.

O texto legislativo em discussão é relevante como referência de governança. A versão final aprovada em comissão consultada para esta análise prevê informação sobre a interação com sistemas de IA e não discriminação; para sistemas de alto risco, prevê explicação, contestação e revisão humana. Também prevê avaliação de impacto algorítmico quando o sistema ou seu uso for classificado como de alto risco. Esses pontos são tratados aqui como **conteúdo de proposta legislativa**, e não como obrigação já incorporada à legislação brasileira vigente.

### 2.3 Possível referência estrangeira: Algorithmic Accountability Act dos Estados Unidos

O título traduzido “Lei de Responsabilidade Algorítmica” também pode se referir ao *Algorithmic Accountability Act of 2025* (S. 2164), dos Estados Unidos. O registro oficial do Government Publishing Office identifica a medida como projeto apresentado no Senado em 25/06/2025, lido duas vezes e encaminhado à Comissão de Comércio, Ciência e Transporte. O registro consultado não a apresenta como lei promulgada.

Se essa for a referência utilizada pelo professor, o trabalho deve descrevê-la como projeto de lei norte-americano e analisar suas propostas de avaliação de impacto de sistemas automatizados, sem afirmar que ela se aplica automaticamente a uma empresa brasileira.

### 2.4 Referência comparativa: Regulamento Europeu de IA

O Regulamento (UE) 2024/1689 adota uma abordagem baseada em níveis de risco. Para sistemas de IA de alto risco, contém requisitos de registro automático de eventos, supervisão humana e conservação de logs sob determinadas condições. É uma referência útil caso o AutoBotz venha a ser oferecido ou utilizado em contexto sujeito ao direito da União Europeia. Sua incidência deve ser verificada pelo contexto territorial e comercial; não se deve importar automaticamente todos os seus prazos e obrigações para a operação brasileira.

## 3. Diagnóstico do AutoBotz Motors

A leitura do snapshot `autobotz-motors-main(10).zip` mostra que a versão atual **não contém modelo de aprendizado de máquina, sistema de IA generativa, classificador treinado ou mecanismo de perfilamento automatizado de clientes**. O que existe são regras de negócio determinísticas, validações de consistência e controles de acesso. Essa distinção deve permanecer explícita na documentação: regra de negócio automatizada não é sinônimo de inteligência artificial.

| Área / arquivo | Comportamento encontrado | Natureza da decisão | Relevância para D1 |
|---|---|---|---|
| `src/autobotz/service/ServicoService.java` | Calcula se a venda está dentro de 365 dias e concede desconto de mão de obra para serviços cujo nome é identificado como revisão. A identificação atual usa `nomeServico.toLowerCase().contains("revis")`. | Regra determinística que pode alterar o valor cobrado do cliente. | **Prioridade de auditoria:** erro na data da venda ou na classificação do serviço pode gerar cobrança ou desconto incorreto. A classificação por trecho do nome é frágil: renomear ou traduzir o serviço pode alterar a decisão sem alterar a regra de negócio pretendida. |
| `src/autobotz/dao/VendaDAO.java` | Verifica o estado do veículo e impede registrar venda quando ele não está disponível. | Validação automática de estoque/transação. | Baixo impacto direto sobre direitos pessoais, mas deve ter motivo claro e rastro técnico para diagnóstico. |
| `src/autobotz/service/ComprasService.java` e `src/autobotz/ui/swing/ComprasPanel.java` | Um usuário autorizado escolhe aprovar ou rejeitar uma solicitação. O serviço verifica permissão, saldo, status e transições válidas antes de gravar a operação. A tela envia `aprovador.getText()` como identificação de quem decidiu. | **Decisão humana apoiada por regras automáticas**, não aprovação autônoma por IA. | A identificação do aprovador vem de um campo de texto editável, não da sessão autenticada. Isso permite atribuição incorreta ou autodeclarada; o ator deve ser obtido da sessão e persistido com seu ID. |
| `src/autobotz/AutorizacaoService.java` | Permite ou nega operações conforme perfil e permissão. | Controle determinístico de acesso (RBAC). | Registrar negações relevantes e mudanças da matriz de permissões. Não confundir RBAC com perfil algorítmico de cliente. |
| `src/autobotz/service/AssinaturaService.java` | Calcula métricas agregadas, como churn, para análise de assinaturas. | Indicador analítico agregado. | Não deve ser usado para negar atendimento ou classificar individualmente clientes sem nova avaliação de impacto. |
| `src/autobotz/service/CRMService.java` | Reúne histórico de compras e total gasto pelo cliente. | Consulta e consolidação de dados. | Não há, no snapshot, recomendação automatizada ou decisão de elegibilidade baseada nesse histórico. |

### 3.1 Auditoria existente e lacunas

O projeto já possui uma base inicial de auditoria:

- `schema.sql` cria a tabela `logs_auditoria`, com usuário, ação, entidade, identificador, detalhes e data/hora;
- `src/autobotz/dao/AuditoriaDAO.java` permite registrar e consultar eventos;
- `src/autobotz/model/LogAuditoria.java` representa os registros;
- `src/autobotz/service/RelatorioService.java` e `src/autobotz/ui/RelatorioMenu.java` disponibilizam a consulta de logs recentes.

Entretanto, essa estrutura registra eventos genéricos. Ela não identifica de forma estruturada a regra executada, sua versão, os critérios considerados, a saída calculada, a justificativa padronizada, a versão do aplicativo ou eventual revisão humana. Além disso, no snapshot examinado, `AuditoriaDAO.registrarAcao` não aparece conectado aos fluxos de decisão acima. Portanto, a existência da tabela e da tela de consulta **não comprova que as decisões automatizadas já estejam sendo auditadas**.

Há rastros próprios em outros módulos: por exemplo, a tabela `solicitacoes_compra` mantém aprovador, motivo de rejeição e data da decisão; e `ResultadoOS` mantém detalhes do cálculo da ordem de serviço para apresentação na interface. Esses dados ajudam a explicar o resultado, mas ainda não constituem um registro completo e versionado de decisão automatizada. Além disso, em `ComprasPanel`, o nome do aprovador/rejeitador é enviado a partir de `aprovador.getText()`. Como esse campo é informado na interface, não comprova por si só a identidade da pessoa autenticada. A correção recomendada é obter o identificador do ator diretamente de `SessaoUsuario`/sessão autenticada e usar esse ID no registro da decisão, deixando o nome apenas como dado de exibição quando necessário.

### 3.2 Fragilidades específicas das regras encontradas

A leitura do método `ehRevisao` em `ServicoService` revelou que a classificação do serviço depende de o nome conter o trecho `revis`. Essa estratégia funciona para os rótulos atuais, mas acopla uma regra financeira a um texto de apresentação. Uma alteração de nome, tradução, abreviação ou erro de digitação pode mudar a cobertura da garantia sem uma alteração intencional da regra. Recomenda-se substituir essa inferência por um campo categórico persistido (por exemplo, `categoria_servico = REVISAO`) ou por um identificador de tipo de serviço controlado, com migração dos registros existentes e testes dos casos de revisão e não revisão. A regra também deve registrar sua versão e os valores efetivamente considerados.

Na aprovação de compras, o serviço aplica controles de autorização e consistência, mas a interface fornece a identificação do aprovador por um campo editável. Para fins de responsabilização, o identificador do usuário deve vir da sessão autenticada, nunca de texto livre digitado na tela. O evento deve distinguir `ator_humano` (usuário que tomou a decisão) e `validacoes_automaticas` (saldo, status e permissões verificadas), mantendo o motivo informado pelo responsável como justificativa separada.

## 4. Impactos regulatórios e operacionais no ERP

### 4.1 Dados e qualidade da entrada

Uma regra só pode ser explicada e auditada se os dados que a alimentaram forem confiáveis. O ERP deve registrar a origem operacional e a versão dos dados relevantes, controlar correções e evitar que campos desatualizados produzam resultado incorreto. Para o cálculo de garantia, por exemplo, é necessário preservar a referência da venda e a data considerada, sem copiar CPF, telefone ou e-mail para o log.

### 4.2 Transparência e explicação

Uma mensagem genérica como “operação não permitida” pode ser insuficiente para uma decisão que afeta valores ou acesso a um serviço. O sistema deve mostrar a regra aplicada, os dados relevantes utilizados e o motivo objetivo do resultado, em linguagem acessível. A explicação não precisa revelar código-fonte, segredo comercial ou detalhes exploráveis de segurança; deve, porém, ser suficiente para que a pessoa compreenda o resultado e possa contestar dados incorretos.

### 4.3 Não discriminação e tratamento proporcional

O sistema não deve usar atributos sensíveis ou substitutos injustificados para decidir acesso, preço, elegibilidade ou prioridade de atendimento. Quando uma decisão tiver potencial de afetar grupos de pessoas, a equipe deve avaliar a qualidade e a representatividade dos dados, procurar resultados desproporcionais e documentar as limitações do teste. A coleta de dados sensíveis apenas para medir viés não deve ocorrer sem base legal, necessidade e salvaguardas apropriadas.

### 4.4 Supervisão, contestação e correção

O titular deve dispor de um canal para questionar um resultado, corrigir dados e pedir reavaliação. Para decisões com impacto econômico relevante, a política proposta prevê revisão humana independente da regra original. Quando houver indício de erro sistêmico, a equipe deve poder suspender a regra e encaminhar o processo a um fluxo manual seguro.

### 4.5 Segurança e minimização dos logs

Logs de decisão podem conter dados pessoais e não devem virar uma segunda base de cadastro. O registro deve usar identificadores internos ou pseudonimizados, armazenar apenas os dados estritamente necessários e restringir a consulta por perfil. Senhas, tokens, códigos MFA, CPF completo, dados de pagamento e conteúdo pessoal desnecessário não devem ser registrados nos detalhes da decisão.

## 5. Proposta de auditoria das decisões automatizadas

A proposta aproveita a estrutura atual de `AuditoriaDAO` e `logs_auditoria`, mas recomenda um registro específico para decisões que possa ser correlacionado ao log geral. A tabela genérica pode continuar registrando ações do usuário; o novo registro deverá guardar a evidência técnica da regra que produziu o resultado.

### 5.1 Inventário obrigatório antes de ativar uma regra

Para cada regra ou sistema automatizado, manter uma ficha com:

1. identificador, nome, finalidade e módulo do ERP;
2. responsável de negócio e responsável técnico;
3. pessoas ou grupos potencialmente afetados;
4. entradas utilizadas e origem dos dados;
5. resultado possível e operação desencadeada;
6. versão da regra, data de entrada em vigor e histórico de alterações;
7. classificação de risco, impacto possível e salvaguardas;
8. existência de revisão humana, contestação e procedimento de contingência.

### 5.2 Campos mínimos de cada evento de decisão

| Campo proposto | Finalidade |
|---|---|
| `decision_id` e `correlation_id` | Identificar o evento e relacioná-lo à operação que o originou. |
| `occurred_at` com fuso/UTC | Reconstruir a sequência temporal sem ambiguidade. |
| `module` e `decision_type` | Identificar módulo e natureza da decisão (regra automática, validação ou decisão humana assistida). |
| `rule_id` e `rule_version` | Identificar exatamente a regra executada. |
| `subject_ref` pseudonimizado | Relacionar o evento ao titular sem repetir dados pessoais diretamente. |
| `resource_type` e `resource_id` | Identificar a venda, ordem de serviço, solicitação ou registro afetado. |
| `input_summary` minimizado | Preservar somente os valores necessários para explicar e reproduzir o resultado. |
| `outcome` e `reason_codes` | Registrar resultado e códigos padronizados de motivo. |
| `explanation` | Armazenar a explicação em linguagem de negócio apresentada ao usuário. |
| `actor_id`, `reviewer_id` e `override_reason` | Diferenciar a regra automática da intervenção humana. |
| `application_version` | Relacionar o resultado à versão do software em execução. |

Não armazenar a cópia integral do cadastro do cliente. Quando for indispensável consultar uma evidência maior, guardar uma referência controlada ao registro original, com autorização e prazo de retenção definidos. Um hash isolado não torna o log anônimo nem garante, sozinho, que ninguém possa adulterar o registro.

### 5.3 Fluxo operacional proposto

1. **Antes da implantação:** inventariar a regra, definir sua finalidade, avaliar riscos e documentar critérios, entradas e resultados esperados.
2. **Na execução:** gerar um evento de decisão junto com a operação, usando o mesmo identificador de correlação. A gravação do evento deve ocorrer na mesma transação quando a consistência exigir que resultado e trilha não se separem.
3. **Na explicação:** mostrar ao usuário o motivo objetivo e os fatores relevantes. Guardar o código de motivo e a versão da regra no evento.
4. **Na revisão:** registrar pedido, responsável, evidências consultadas, resultado da revisão e eventual substituição da decisão anterior, sem apagar o histórico original.
5. **Na auditoria periódica:** verificar amostras, decisões contestadas, exceções, mudanças de resultado, falhas e possíveis diferenças injustificadas entre grupos, quando essa análise for legítima e necessária.
6. **Na mudança de regra:** criar nova versão, executar testes de regressão e impacto antes da publicação e manter a versão anterior disponível para reconstrução histórica.
7. **No incidente:** suspender a regra quando houver dano ou erro sistêmico relevante, aplicar fluxo manual de contingência, preservar evidências e documentar correções.

### 5.4 Controles de integridade e acesso

- A escrita dos eventos deve ser restrita aos serviços responsáveis; operadores comuns não devem poder editar ou apagar registros históricos.
- A consulta de auditoria deve ser limitada a pessoas autorizadas, com registro das consultas administrativas relevantes.
- Exportações devem usar filtros, mascaramento e finalidade documentada.
- Backups e controles de integridade devem ser testados periodicamente. Se houver assinatura ou hash encadeado, a chave deve ficar fora do banco e do repositório; a verificação criptográfica complementa, mas não substitui, controle de acesso e cópias protegidas.
- O prazo de retenção deve ser definido por finalidade, risco, obrigação aplicável e necessidade de defesa de direitos. Não copiar automaticamente o prazo europeu de seis meses para todos os logs no Brasil.

### 5.5 Exemplo ilustrativo de evento

O exemplo abaixo é fictício e demonstra o nível de explicação desejado. Os valores de entrada são minimizados e não contêm dados pessoais diretos.

```json
{
  "decision_id": "d1-demo-000123",
  "correlation_id": "os-demo-000045",
  "occurred_at": "2026-11-17T14:30:00Z",
  "module": "OFICINA",
  "decision_type": "BUSINESS_RULE",
  "rule_id": "WARRANTY.REVISION_COVERAGE",
  "rule_version": "1.0.0",
  "subject_ref": "cliente-pseudo-8f21",
  "resource_type": "ordem_servico",
  "resource_id": 45,
  "input_summary": {
    "days_since_sale": 120,
    "service_category": "REVISAO",
    "warranty_limit_days": 365
  },
  "outcome": "COVERED",
  "reason_codes": ["WITHIN_WARRANTY_PERIOD", "SERVICE_IS_REVISION"],
  "explanation": "A revisão foi coberta porque a venda ocorreu há 120 dias e o prazo configurado é de 365 dias.",
  "human_review": "NOT_REQUESTED",
  "application_version": "d1-demo"
}
```

## 6. Política de explicabilidade

A política completa está em [`D1_POLITICA_EXPLICABILIDADE.md`](D1_POLITICA_EXPLICABILIDADE.md). Em resumo, toda funcionalidade que produza ou recomende um resultado com potencial de afetar uma pessoa deverá informar quando há automação, qual a finalidade, quais categorias de dados e critérios relevantes são utilizados, qual foi o resultado e como solicitar correção ou revisão.

A explicação deverá ser proporcional ao impacto. Uma validação simples de estoque pode usar uma mensagem curta com o motivo; uma decisão que afete preço, elegibilidade, acesso a serviço ou perfil de cliente exige explicação mais completa e canal de contestação. Quando a legislação ou a classificação de risco exigir revisão humana, ela deverá ser feita por pessoa com autoridade para alterar o resultado, e não apenas confirmar automaticamente a saída do sistema.

## 7. Plano de adequação para o AutoBotz

| Etapa | Ação proposta | Evidência de conclusão |
|---|---|---|
| 1 — Inventário | Listar regras automatizadas, validações, recomendações e decisões humanas apoiadas pelo ERP. | Catálogo com responsável, finalidade, dados, saída e risco por regra. |
| 2 — Classificação | Identificar decisões que afetam interesses de clientes ou funcionários e avaliar a incidência da LGPD art. 20. | Matriz de risco e justificativa documentada. |
| 3 — Registro | Integrar os fluxos selecionados à trilha estruturada de decisões. | Eventos de teste com regra, versão, resultado e motivo. |
| 4 — Explicação | Padronizar mensagens e criar canal de contestação e revisão. | Exemplos de explicação, formulário/canal e procedimento de revisão. |
| 5 — Testes | Testar entradas válidas, inválidas, limites, dados corrigidos, regras antigas e novas, revisão e integridade dos logs. | Relatório de testes com evidências reproduzíveis. |
| 6 — Monitoramento | Revisar amostras, reclamações, exceções, erros e alterações de desempenho. | Relatório periódico e plano de correção. |
| 7 — Comunicação | Informar clientes e usuários do ERP sobre automações relevantes e como pedir revisão. | Aviso de transparência, FAQ e material de orientação. |

### 7.1 Testes de aceitação propostos

- Uma decisão repetida com os mesmos dados e a mesma versão da regra deve gerar resultado consistente.
- Uma alteração de dado relevante deve refletir-se na explicação e no resultado, sem apagar o evento anterior.
- Cada decisão de garantia deve permitir reconstruir data de venda, categoria de serviço, prazo e motivo do resultado sem expor dados pessoais desnecessários.
- A venda de veículo indisponível deve registrar o motivo da recusa e o identificador do veículo.
- Aprovação e rejeição de compras devem identificar o aprovador por ID da sessão autenticada (não por campo de texto livre), além de motivo, data e validações automáticas aplicadas.
- A classificação de serviços para cobertura de garantia deve usar categoria/identificador estável, não palavras contidas no nome exibido; alterações de categoria devem ser versionadas e testadas.
- Um pedido de revisão deve gerar novo evento vinculado ao original, mantendo o histórico.
- Usuário sem permissão não deve consultar nem exportar trilhas detalhadas de decisão.
- Uma mudança de regra deve receber nova versão e testes de regressão antes de ser ativada.
- Quando uma falha sistemática for identificada, deve existir procedimento testado para suspender a regra e encaminhar operações a uma revisão manual.

## 8. Conclusão

O D1 não exige que o AutoBotz seja apresentado como um produto de inteligência artificial que ele não é. O diagnóstico do snapshot aponta regras determinísticas de negócio, controle de acesso e decisões humanas apoiadas pelo sistema. A responsabilidade regulatória começa por identificar corretamente essas funções, compreender quando elas afetam pessoas e manter evidências suficientes para explicar, contestar e revisar seus resultados.

A estrutura genérica de auditoria já existente é um ponto de partida, mas ainda não demonstra a auditoria de decisões automatizadas: faltam dados estruturados de regra, versão, motivo, resultado, vínculo com a operação e revisão. A recomendação é evoluir a trilha atual sem duplicar dados pessoais, priorizar a regra de garantia por seu efeito financeiro sobre o cliente e adotar uma política de explicabilidade com revisão humana proporcional ao risco.

Esta entrega é uma análise técnica-acadêmica, não um parecer jurídico nem uma declaração de conformidade. A versão final deve ser validada contra o texto integral do enunciado e, para implantação comercial, revisada por profissional jurídico e pelo responsável por proteção de dados.

## 9. Referências normativas e fontes oficiais

1. **Brasil. Lei nº 13.709/2018 — LGPD**, texto atualizado na Câmara dos Deputados. Especialmente arts. 3º, 6º, 18, 20 e 46.  
   https://www2.camara.leg.br/legin/fed/lei/2018/lei-13709-14-agosto-2018-787077-normaatualizada-pl.html
2. **Câmara dos Deputados. PL nº 2.338/2023 — ficha de tramitação.** Situação consultada em 17/11/2026: aguardando parecer do relator na Comissão Especial sobre Inteligência Artificial.  
   https://www.camara.leg.br/proposicoesWeb/fichadetramitacao?idProposicao=2487262
3. **Câmara dos Deputados. Texto final aprovado em comissão do PL nº 2.338/2023.** Referência de proposta legislativa, não de lei vigente; especialmente arts. 5º a 8º e 25.  
   https://www.camara.leg.br/proposicoesWeb/prop_mostrarintegra?codteor=2868197&filename=PL+2338%2F2023
4. **U.S. Government Publishing Office. Algorithmic Accountability Act of 2025, S. 2164 (IS).** Registro oficial de projeto apresentado e encaminhado à comissão em 25/06/2025.  
   https://www.govinfo.gov/app/details/BILLS-119s2164is
5. **União Europeia. Regulamento (UE) 2024/1689 — Artificial Intelligence Act.** Especialmente arts. 12, 14 e 19, relativos a registro de eventos, supervisão humana e conservação de logs em sistemas de alto risco.  
   https://eur-lex.europa.eu/eli/reg/2024/1689

**Data da consulta às fontes:** 17 de novembro de 2026.
