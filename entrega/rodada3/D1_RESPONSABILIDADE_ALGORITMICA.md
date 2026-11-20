# D1 — Mudanças regulatórias: responsabilidade algorítmica

**Projeto:** AutoBotz Motors — Equipe B  
**Responsável:** João  
**Rodada:** 3ª rodada — novembro de 2026  
**Versão:** 1.1 — 19 de novembro de 2026  
**Tipo de entrega:** análise (A) — enquadramento regulatório, impacto no ERP, proposta de registros auditáveis e política de explicabilidade.  
**Código analisado:** branch `main`, commit `b00bc55c267edaa893f39dee9a850fc8f0abc46f`. Qualquer integrante confere os mesmos arquivos com `git checkout b00bc55`.

Documento relacionado: [`D1_POLITICA_EXPLICABILIDADE.md`](D1_POLITICA_EXPLICABILIDADE.md).

## 1. Enquadramento

O professor apresenta a **Lei de Responsabilidade Algorítmica como parte do cenário hipotético do desafio**. Esta análise parte dessa situação: pergunta o que uma lei desse tipo mudaria no AutoBotz, sem procurar uma lei real equivalente.

Dois limites desta versão:

- O enunciado da terceira rodada descreve a Lei de Responsabilidade Algorítmica como uma situação hipotética, sem apresentar artigos, prazos ou penalidades. A análise usa os objetivos indicados pelo professor — transparência e rastreabilidade das decisões, sobretudo nas áreas fiscal e de crédito — para propor registros auditáveis e política de explicabilidade. Não há artigos numerados a mapear.
- O enunciado destaca os módulos **fiscal** e **de crédito**. Eles são tratados na seção 5.

A LGPD e outras normas aparecem apenas como **referências complementares** (seção 2). Este documento é uma análise acadêmica: não é parecer jurídico e **não afirma conformidade regulatória do AutoBotz**.

## 2. Referências complementares e seu status

Fontes consultadas em 19/11/2026. A coluna "Status" separa norma vigente de projeto.

| Referência | Status | Uso neste trabalho |
|---|---|---|
| Lei de Responsabilidade Algorítmica (enunciado) | **Cenário hipotético do desafio**; não é norma vigente nem projeto real identificado | Referência principal do D1 |
| Lei nº 13.709/2018 (LGPD), arts. 6º e 20 | **Vigente** | Princípios de tratamento (art. 6º) e direito de solicitar revisão de decisão tomada unicamente por tratamento automatizado de dados pessoais, com informações claras sobre critérios e procedimentos (art. 20). O texto vigente não exige que a revisão seja feita por pessoa natural; essa exigência foi vetada em 2019. A revisão humana é adotada aqui como boa prática |
| Lei nº 12.414/2011 (Cadastro Positivo), art. 5º, incisos IV e VI | **Vigente** | Só relevante se houver análise de crédito com consulta a cadastro positivo: conhecer os principais elementos e critérios da análise de risco e solicitar revisão de decisão exclusivamente automatizada. Não se aplica ao ERP atual, que não tem módulo de crédito |
| PL nº 2.338/2023 (marco legal de IA) | **Projeto de lei.** Aprovado pelo Senado, em análise em comissão especial da Câmara dos Deputados | Comparação de governança (explicação, contestação, avaliação de impacto para alto risco). Nenhuma de suas obrigações é tratada como lei em vigor |
| Algorithmic Accountability Act of 2025 (S. 2164 no Senado e H.R. 5511 na Câmara dos EUA) | **Projetos de lei dos EUA**, em comissão, sem aprovação | Comparação sobre avaliação de impacto de sistemas automatizados. Não é a lei do enunciado |
| Regulamento (UE) 2024/1689 (AI Act) | **Em vigor na União Europeia**, com aplicação escalonada. As obrigações de sistemas de alto risco foram adiadas pela alteração conhecida como Digital Omnibus: 2/12/2027 para os sistemas do Anexo III e 2/8/2028 para os embutidos em produtos regulados | Comparação: crédito de pessoas naturais é um dos usos classificados como alto risco. Incidência sobre o AutoBotz exigiria contexto territorial e comercial na UE |

Links:

1. LGPD: https://www2.camara.leg.br/legin/fed/lei/2018/lei-13709-14-agosto-2018-787077-normaatualizada-pl.html
2. PL 2.338/2023, tramitação no Senado: https://www25.senado.leg.br/web/atividade/materias/-/materia/157233
3. S. 2164 (GovInfo): https://www.govinfo.gov/app/details/BILLS-119s2164is
4. AI Act: https://eur-lex.europa.eu/eli/reg/2024/1689

Os links 2 e 3 foram localizados por busca em 19/11/2026; os links 1 e 4 vêm da versão anterior e devem ser abertos antes da entrega final. O status de projetos muda; a data da consulta acompanha esta tabela.

## 3. O que o ERP decide e o que as pessoas decidem

A leitura do código no commit `b00bc55` não encontrou modelo de aprendizado de máquina, IA generativa, classificador treinado ou perfilamento de clientes. Tudo o que existe são **regras determinísticas**, **controles de acesso** e **decisões de usuários apoiadas por validações**. Regra automática não é sinônimo de IA, e a documentação deve manter essa distinção.

### 3.1 Regras automáticas (o sistema decide sozinho)

| Código | Regra | Onde está | Efeito |
|---|---|---|---|
| R1 | Cobertura de garantia da revisão | `ServicoService.calcular`, `estaDentroDaGarantia`, `ehRevisao` | Se a venda tem de 0 a 364 dias (`DIAS_GARANTIA = 365`) e o nome do serviço contém `revis`, o valor do item vira desconto. Sem data de venda, não há garantia. O cálculo usa `LocalDate.now()` |
| R2 | Veículo indisponível para venda | `VendaDAO` (`disponivel`) | Recusa a venda e desfaz a transação quando o status não é "DISPONIVEL" |
| R3 | Bloqueio de login | `AuthService` | 5 falhas em janela móvel de 10 minutos bloqueiam o nome de usuário |
| R4 | Segundo fator | `MfaService` | Código de 6 dígitos, validade de 5 minutos, 3 tentativas (MFA demonstrativo) |
| R5 | Expiração da sessão web | `SessaoWebService` | 30 minutos de ociosidade ou 8 horas de duração |
| R6 | Alerta de vencimento de contrato | `ContratoService.registrarAlertasVencimento` | Alerta único para contratos não encerrados que vencem em até 10 dias |
| R7 | Liberação de pagamento simulado | `ContratoService.podeLiberarPagamento` | Libera se a nota fiscal foi marcada como validada, o vencimento chegou, o pagamento ainda não foi liberado e o contrato não está encerrado. A API de simulação só libera se essa regra local também permitir |
| R8 | Validações da aprovação de compras | `ComprasService.aprovar` | Exige solicitação pendente e saldo suficiente; baixa de saldo, pedido e mudança de status ocorrem em uma única transação |
| R9 | Controle de acesso por perfil | `AutorizacaoService`, `SessaoWebService.autorizar` | Nega a operação sem a permissão do perfil e registra `ACESSO_NEGADO` na trilha de segurança |

Indicadores e simulações (MRR, churn e simulador 30/50/20 em `AssinaturaService`) não decidem sobre uma pessoa; o simulador usa dados fictícios.

### 3.2 Decisões de usuários (a pessoa decide; o sistema valida e grava)

| Código | Decisão | Quem pode | O que o sistema faz |
|---|---|---|---|
| H1 | Aprovar ou rejeitar solicitação de compra | Perfil com `ACESSAR_COMPRAS` (hoje, ADMIN) | Aplica R8 e grava aprovador, motivo e data (ver 6) |
| H2 | Marcar a nota fiscal de um contrato como validada | Usuário autorizado em `ContratoService` | Grava um indicador booleano e um evento no ledger; **não confere o documento fiscal** (ver 5.1) |
| H3 | Alterar o estado de um contrato | Usuário autorizado | Valida a transição permitida e grava evento no ledger |

A tela e os relatórios não devem atribuir ao algoritmo uma escolha feita por uma pessoa, nem esconder que a validação automática precedeu a decisão.

### 3.3 Fragilidades encontradas

- **R1 depende do texto do nome do serviço.** `nomeServico.toLowerCase().contains("revis")` liga uma regra financeira a um rótulo de tela. Renomear, traduzir ou errar a grafia muda a cobertura sem mudar a regra pretendida. Recomenda-se uma categoria persistida (por exemplo, `categoria_servico = REVISAO`), com migração e testes.
- **R1 não deixa rastro do cálculo.** `calcularTotalOS` devolve um `ResultadoOS` exibido na tela; não encontrei gravação dos dias contados, do prazo aplicado ou do resultado.
- **A identidade do aprovador em H1 vem de um campo de texto** (seção 6).

## 4. Auditoria das decisões

### 4.1 O que existe hoje e o que falta

| Mecanismo | Evidência no código | Cobre decisões automatizadas? |
|---|---|---|
| `logs_auditoria` com usuário, ação, entidade, detalhes e data | `schema.sql`, `AuditoriaDAO` | **Não.** `registrarAcao` não é chamado por nenhum serviço; só há consulta (`listarRecentes`) no relatório e na tela. A tabela existe, mas nada grava nela |
| `eventos_seguranca` | `AuthService`, `MfaService`, `SessaoWebService` | **Parcialmente**, para R3, R4 e R9 (tipo, sucesso, causa, usuário, origem). A expiração da sessão (R5) não gera evento |
| Ledger de contratos com hash SHA-256 | `ContratoService`, `LedgerContratoHash` | **Parcialmente**, para R6, R7 e H2/H3. Registra eventos e integridade, mas não o motivo padronizado nem a versão da regra. Uma recusa de liberação (R7) não é registrada |
| `solicitacoes_compra` com `aprovador`, `motivo_rejeicao`, `decidido_em` | `schema.sql`, `ComprasService` | **Parcialmente**, para H1. O aprovador é texto, não identificador de usuário |
| `ResultadoOS.detalhes` | `ServicoService` | **Não.** É apresentação em tela; não é gravada |

Conclusão: a tabela e a tela de consulta de auditoria **não comprovam** que as decisões estejam auditadas. Hoje há rastros isolados por módulo, nenhum com regra, versão, motivo padronizado e vínculo com a operação ao mesmo tempo.

### 4.2 Informações que cada registro deve conter

| Informação | Como seria preenchida | Situação hoje |
|---|---|---|
| Responsável | ID do usuário da **sessão autenticada**; para regra automática, o usuário que disparou a operação e o responsável pela regra | Parcial: `logs_auditoria.id_usuario` existe e `AuditoriaDAO.registrarAcao` já lê a sessão, mas não é usado. Em compras, o responsável é texto digitado |
| Regra aplicada | Código estável (`R1`, `R7`...) | Não existe |
| Versão da regra | Constante versionada, alterada a cada mudança de limite ou condição | Não existe |
| Dados utilizados | Resumo mínimo dos valores que entraram na regra, sem CPF, telefone ou e-mail | Não existe (R1); parcial (R7, no campo de detalhes do ledger) |
| Resultado | Valor padronizado (`COBERTA`, `NAO_COBERTA`, `LIBERADO`...) | Parcial (estados em contratos e compras) |
| Motivo | Código de motivo e texto curto | Parcial: mensagens de erro e `motivo_rejeicao`; sem padronização |
| Vínculo com a operação | `resource_type` e `resource_id` (ordem de serviço, venda, solicitação, contrato) | Parcial: `entidade` e `entidade_id` em `logs_auditoria`; nos demais, a própria tabela da operação |
| Contestação e revisão | Pedido, revisor, desfecho, ligação com a decisão original | **Não existe** |

Proposta de implementação: um registro estruturado de decisões, relacionável a `logs_auditoria` (tabela nova ou colunas novas, a definir pela equipe), gravado **na mesma transação** da operação quando resultado e trilha não puderem se separar. Evitar duplicar dados pessoais. Um hash isolado não impede adulteração por quem altera dado e hash; o encadeamento do ledger de contratos pode servir de modelo.

### 4.3 Exemplo completo A — garantia da revisão (regra automática R1)

Cenário fictício: ordem de serviço 45, com um item "Revisão 10 mil km" de R$ 300,00, para um veículo vendido há 120 dias.

| Campo | Valor no exemplo | Existe hoje? |
|---|---|---|
| Dados utilizados | data da venda (há 120 dias), nome do serviço, preço e quantidade do item | **Sim**, lidos de `vendas` e `servicos`; **não gravados** como evidência do cálculo |
| Regra aplicada | `R1 — cobertura de garantia da revisão` | Existe a lógica em `ServicoService`; **não há código de regra** |
| Versão | `1.0.0` (prazo de 365 dias; serviço identificado por categoria) | **Não existe** |
| Resultado | `COBERTA`, desconto de R$ 300,00 | Calculado e exibido; **não gravado** |
| Motivo | `DENTRO_DO_PRAZO` e `SERVICO_E_REVISAO` | Parcial: a tela mostra o item com a marca "garantia"; sem dias nem prazo |
| Responsável | usuário da sessão que abriu o cálculo; responsável pela regra: a equipe da Oficina | **Não existe** |
| Vínculo com a operação | `ordem_servico` 45 | O ID existe; o vínculo com um registro de decisão não |

O que precisaria ser acrescentado: constante de versão da regra; categoria de serviço persistida no lugar do `contains("revis")`; gravação do registro de decisão ao calcular; e mensagem com os dias e o prazo. Registro ilustrativo (formato proposto, não implementado):

```json
{
  "correlation_id": "os-45",
  "occurred_at": "2026-11-19T14:30:00Z",
  "rule_id": "R1",
  "rule_version": "1.0.0",
  "actor_id": 3,
  "resource_type": "ordem_servico",
  "resource_id": 45,
  "input_summary": { "days_since_sale": 120, "warranty_limit_days": 365, "service_category": "REVISAO" },
  "outcome": "COBERTA",
  "reason_codes": ["DENTRO_DO_PRAZO", "SERVICO_E_REVISAO"],
  "human_review": "NAO_SOLICITADA"
}
```

### 4.4 Exemplo completo B — aprovação de compra (decisão de usuário H1 com validações R8)

| Campo | Valor no exemplo | Existe hoje? |
|---|---|---|
| Dados utilizados | valor da solicitação, saldo do orçamento, status da solicitação | Sim, lidos dentro da transação; o saldo anterior à baixa não é guardado |
| Regra aplicada | `R8 — saldo suficiente e solicitação pendente` | Existe a lógica; sem código de regra |
| Versão | `1.0.0` | Não existe |
| Resultado | `APROVADA` e pedido gerado, ou `REJEITADA` com motivo | **Sim**: status, `decidido_em`, `motivo_rejeicao` e `pedidos_compra` |
| Motivo | Na rejeição, o texto do aprovador; na aprovação, nenhum (opcional) | Parcial |
| Responsável | ID e nome do usuário **autenticado** que decidiu | **Parcial, com falha:** `aprovador` é texto digitado (seção 6) |
| Vínculo com a operação | `solicitacao_id` → `pedido_compra` | Sim |
| Separação | Decisão humana (H1) distinta das validações automáticas (R8) | Não registrada de forma distinta |

## 5. Módulos fiscal e de crédito

O enunciado destaca essas duas áreas. A busca no código (`src`, `schema.sql`, `db/migrations`, portal) por termos como imposto, tributo, ICMS, NF-e, CFOP, crédito, financiamento, parcela, score, juros e inadimplência **não encontrou regra automatizada em nenhuma das duas**. O que existe é descrito a seguir; nada foi inventado para preencher a lacuna.

### 5.1 Fiscal: ausente

- Não há cálculo de tributos, emissão ou consulta de documentos fiscais, nem integração com órgão fazendário.
- O único elemento relacionado é `nota_fiscal_validada`, um indicador booleano em `contratos_fornecedor`. Ele é marcado por um usuário (H2) e alimenta R7. O sistema não lê o documento, não confere valores nem chave de acesso; apenas guarda que alguém marcou a validação.
- Consequência para a análise: R7 libera pagamento (simulado) com base em uma validação humana que o ERP não verifica.

Se um módulo fiscal fosse implementado, seriam necessários, no mínimo:

| Controle | Registro |
|---|---|
| Parâmetros fiscais versionados com vigência | Versão e período de vigência usados em cada cálculo |
| Memória de cálculo por documento | Base, alíquota, regra e resultado de cada tributo |
| Imutabilidade do documento emitido | Correção e cancelamento por evento novo, sem sobrescrever o original |
| Separação de funções | Quem altera parâmetros fiscais não é quem emite documentos |
| Responsável técnico e fiscal | Usuário e papel de cada alteração de regra |
| Retenção conforme a obrigação aplicável | Prazo definido por norma fiscal, não por padrão do sistema |

### 5.2 Crédito: ausente

- Não há concessão de crédito, limite, financiamento, parcelamento, pontuação de risco nem consulta a bureau.
- Os "eventos financeiros" do D12 são indicadores de assinatura com dados fictícios; o simulador não decide sobre clientes reais. O pagamento do D13 é simulado.

Se um módulo de crédito fosse implementado, a decisão de conceder, negar ou limitar crédito seria de alto impacto. Seriam necessários, no mínimo:

| Controle | Registro |
|---|---|
| Finalidade e base de tratamento dos dados | Documentadas antes da ativação (LGPD art. 6º) |
| Dados proibidos e substitutos injustificados | Lista de atributos que a regra não pode usar, com teste automatizado |
| Regra ou modelo versionado | Versão, critérios e limites efetivamente aplicados |
| Explicação ao solicitante | Principais fatores e resultado, sem expor dado de terceiros |
| Revisão por pessoa com autoridade para reverter | Pedido, revisor, evidências e desfecho vinculados à decisão original |
| Registro de consulta a fontes externas | Fonte, data e finalidade |
| Avaliação de impacto antes da ativação | Riscos, grupos afetados e salvaguardas |
| Alçada e separação de funções | Quem aprova exceções não é quem configura a regra |

Estes controles são **requisitos para uma eventual implementação**. Não fazem parte do ERP atual, e o D1 (análise) não exige criá-los.

## 6. Ponto técnico: identidade do aprovador em compras

**Achado.** A identidade de quem aprova ou rejeita uma compra vem de texto digitado.

- `ComprasService.aprovar(int solicitacaoId, String aprovador)` e `rejeitar(int, String aprovador, String motivo)` recebem o nome como parâmetro. O serviço só confere que o texto não está vazio e tem até 60 caracteres, e o grava em `solicitacoes_compra.aprovador`.
- Em `ComprasPanel`, o campo `aprovador` é preenchido com o usuário da sessão, mas continua editável, e os botões enviam `aprovador.getText()`. O campo `solicitante` segue o mesmo padrão.
- A permissão é verificada, mas ela vale para qualquer ação de compras: `AppFrame` passa `acao -> allowed(ACESSAR_COMPRAS)`. A pessoa autorizada pode registrar a decisão em nome de outra.
- O banco guarda só o nome (`VARCHAR(60)`), sem ID de usuário. Decisões antigas não podem ser atribuídas com segurança.

**Correção proposta (não aplicada neste trabalho).**

1. `ComprasService` obtém o ator da sessão autenticada (por exemplo, um fornecedor de identidade injetado pelo construtor, como já ocorre com o autorizador), e os métodos deixam de receber o nome.
2. Nova coluna `aprovador_id` com chave estrangeira para `usuarios`, por migração repetível e aplicada também ao `schema.sql`; o nome fica como cópia para exibição.
3. `ComprasPanel` mostra o aprovador como rótulo, sem campo editável; o mesmo vale para o solicitante.
4. Atualizar `D11ComprasTest`, `D11ComprasJdbcTest` e `D11ComprasSwingTest`, que hoje passam o nome como argumento, e incluir um teste: uma tentativa de informar outro nome não altera o responsável gravado.
5. Se criar uma migração nova, incluí-la nas listas do workflow `integration-regression.yml` e em `db/migrations/README.md`, que hoje cobrem 011 a 016.

**Pendência com o responsável.** O módulo de compras é do Alexandre (D11, conforme o README). A correção precisa ser alinhada com ele antes de qualquer alteração de código; este PR altera apenas documentação.

**Observação.** `solicitante` e `aprovador` partem do mesmo usuário e nada impede que a mesma pessoa solicite e aprove. Isso é uma decisão de governança a discutir (separação de funções), não um defeito de implementação.

## 7. Contestação e revisão humana

Nada disso existe no ERP. Não há canal para a pessoa afetada questionar uma decisão, registro de pedido de revisão nem papel de revisor. A aprovação de compras (H1) é uma decisão tomada por uma pessoa; **não deve ser apresentada como revisão humana de uma decisão automatizada**. O fluxo de contestação e revisão está descrito como proposta em [`D1_POLITICA_EXPLICABILIDADE.md`](D1_POLITICA_EXPLICABILIDADE.md), seção 7.

## 8. Resumo: o que funciona e o que é proposta

| Item | Situação |
|---|---|
| Regras R1–R9 identificadas no código | **Funciona hoje** (código na `main`) |
| Trilha de segurança (R3, R4, R9) | **Funciona hoje**, sem versão de regra |
| Ledger de contratos (R6, R7, H2, H3) | **Funciona hoje**, sem motivo padronizado nem registro de recusa |
| Registro do aprovador de compras | **Funciona com falha**: nome digitado (seção 6) |
| Gravação em `logs_auditoria` pelos fluxos de decisão | **Não existe**; a tabela está sem uso pelos serviços |
| Registro estruturado de decisões (regra, versão, motivo, resultado, vínculo) | **Proposta** |
| Categoria persistida para a regra de garantia | **Proposta** |
| Identidade do aprovador pela sessão | **Proposta**, dependente do responsável pelo módulo |
| Contestação e revisão humana | **Proposta** |
| Módulos fiscal e de crédito | **Não existem**; controles descritos na seção 5 são requisitos futuros |

Esta análise **não comprova conformidade** do AutoBotz com a lei hipotética nem com qualquer norma citada.

## 9. Validação realizada e pendências

Conferido em 19/11/2026 sobre o commit `b00bc55`:

- as afirmações sobre o código foram checadas por leitura dos arquivos citados e por busca textual; os comandos estão abaixo;
- `python3 scripts/check-doc-links.py` valida os links relativos entre documentos;
- o status das normas externas foi consultado por busca (seção 2); links externos não são verificados pelo script.

```bash
grep -n "DIAS_GARANTIA\|contains(\"revis\")" src/autobotz/service/ServicoService.java
grep -rn "registrarAcao" src --include=*.java
grep -n "aprovador" src/autobotz/service/ComprasService.java src/autobotz/ui/swing/ComprasPanel.java
grep -n "ACESSAR_COMPRAS" src/autobotz/ui/swing/AppFrame.java
grep -rniE "fiscal|imposto|tribut|credito|financiamento|score" src schema.sql db
```

Questões registradas para eventual continuidade:

1. A identidade do aprovador de compras ainda precisa ser vinculada à sessão autenticada caso a proposta de auditoria seja implementada no produto (seção 6).
2. As referências externas devem ser conferidas na data de utilização, pois a tramitação de projetos de lei pode mudar.
3. Uma implantação comercial exigiria revisão jurídica e avaliação do responsável pela proteção de dados.

O commit indicado no cabeçalho é a referência histórica usada na análise, não a versão final do repositório.
