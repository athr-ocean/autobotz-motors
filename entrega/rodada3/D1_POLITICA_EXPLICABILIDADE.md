# D1 — Política de explicabilidade de decisões automatizadas

**Projeto:** AutoBotz Motors — Equipe B  
**Responsável pela proposta:** João  
**Versão:** 1.0 — 17 de novembro de 2026  
**Status:** proposta de política para revisão e aprovação da equipe; não representa certificação jurídica ou funcional.

## 1. Objetivo

Definir como o AutoBotz Motors deverá informar, registrar e permitir a contestação de resultados produzidos por regras automatizadas ou apoiados por sistemas computacionais. A política procura tornar compreensível o motivo de um resultado sem expor dados pessoais, credenciais, segredos comerciais ou detalhes que comprometam a segurança do ERP.

## 2. Abrangência

Aplica-se a:

- regras automáticas que alterem valores, elegibilidade, disponibilidade, acesso ou atendimento;
- sistemas que recomendem ou classifiquem pessoas, clientes, funcionários, fornecedores ou operações;
- decisões humanas cuja execução seja condicionada por regras automatizadas do ERP;
- novas integrações ou modelos de terceiros que venham a ser adicionados ao AutoBotz.

A política diferencia regra de negócio determinística, controle de acesso, decisão humana assistida e sistema de IA. Não se deve descrever uma simples condição `if/else`, cálculo ou validação como inteligência artificial.

## 3. Princípios

1. **Transparência:** a pessoa afetada deve saber quando um resultado relevante é produzido ou apoiado por automação.
2. **Explicação compreensível:** informar o resultado e seus motivos em linguagem simples, com os principais fatores efetivamente utilizados.
3. **Proporcionalidade:** quanto maior o possível impacto sobre direitos ou interesses, mais completa deve ser a explicação e mais forte a revisão.
4. **Não discriminação:** não usar critérios proibidos, abusivos ou substitutos injustificados; investigar resultados desproporcionais.
5. **Qualidade dos dados:** permitir correção de dados inexatos e reavaliar o resultado quando a correção alterar os critérios de entrada.
6. **Supervisão responsável:** a revisão humana deve ser feita por pessoa capacitada e autorizada a alterar a decisão.
7. **Minimização e segurança:** não repetir dados pessoais desnecessários em mensagens ou logs.
8. **Rastreabilidade:** cada decisão auditável deve indicar a versão da regra, o motivo e a operação relacionada. A identidade do responsável humano deve vir da sessão autenticada, e não de um nome digitado livremente em formulário.

## 4. Informação antes e durante o uso

Quando uma automação tiver potencial de afetar interesses de uma pessoa, a interface, contrato, aviso ou material de suporte deverá informar:

- que existe uma regra ou sistema automatizado no fluxo;
- a finalidade da automação;
- as categorias de dados relevantes utilizadas;
- se o resultado é automático, recomendação ou decisão humana apoiada pelo sistema;
- como consultar uma explicação, corrigir dados ou solicitar revisão.

A informação deverá ser fornecida em português claro e, quando aplicável ao público atendido, também nos demais idiomas suportados pelo ERP. Não basta colocar a informação apenas em um documento técnico inacessível ao usuário final.

## 5. Conteúdo mínimo da explicação individual

Quando solicitada ou apresentada em uma decisão relevante, a explicação deverá conter:

1. **Resultado:** o que foi permitido, negado, calculado, recomendado ou alterado.
2. **Motivo principal:** a regra ou condição que levou ao resultado.
3. **Dados considerados:** as categorias de dados e os valores pertinentes, com mascaramento quando necessário.
4. **Critério e versão:** regra, limite ou versão aplicada, quando isso for útil para compreender o caso.
5. **Limitações:** informação sobre dados ausentes, incerteza ou exceções que possam alterar o resultado.
6. **Próxima ação:** o que a pessoa pode corrigir, apresentar ou solicitar.
7. **Revisão:** canal e procedimento para contestar, inclusive revisão humana quando aplicável.

A explicação deve representar o que realmente ocorreu. Não pode inventar uma justificativa posterior nem afirmar que uma pessoa revisou uma decisão quando isso não aconteceu.

## 6. Exemplos no AutoBotz Motors

### 6.1 Regra de garantia da Oficina

**Exemplo de mensagem ao cliente:**

> O serviço de revisão foi considerado coberto pela garantia porque a venda registrada ocorreu há 120 dias e o prazo configurado para esta regra é de 365 dias. A cobertura depende da data e dos dados da venda associados ao veículo. Caso essas informações estejam incorretas, solicite a conferência do registro.

A interface deverá mostrar os dados relevantes da operação, sem revelar informações pessoais de terceiros. A trilha de auditoria deverá guardar a versão da regra, a quantidade de dias calculada, a categoria do serviço, o resultado e os códigos de motivo — não o cadastro completo do cliente.

### 6.2 Veículo indisponível para venda

**Exemplo de mensagem ao operador:**

> A venda não foi registrada porque o veículo selecionado não está marcado como disponível. Confira o status do veículo e atualize o cadastro, caso esteja incorreto.

O sistema deve apresentar o status utilizado na validação e permitir que um usuário autorizado corrija o cadastro, mantendo o histórico da mudança.

### 6.3 Aprovação de solicitação de compra

**Exemplo de mensagem interna:**

> A solicitação foi aprovada por [usuário autorizado] após a validação do saldo disponível e da situação pendente da solicitação. A aprovação foi uma decisão do usuário; o sistema realizou as verificações e a gravação transacional.

Em caso de rejeição, registrar o motivo informado pelo aprovador. A interface e os relatórios não devem atribuir ao algoritmo uma escolha que foi feita por uma pessoa.

### 6.4 Negação de permissão (RBAC)

**Exemplo de mensagem ao usuário:**

> Seu perfil atual não possui permissão para acessar este módulo. Solicite ao administrador responsável a verificação do seu perfil ou da matriz de permissões.

A explicação deve apontar a permissão ausente sem revelar dados de outros usuários, credenciais ou detalhes internos desnecessários.

## 7. Contestação e revisão

O ERP deverá oferecer um canal acessível para que a pessoa afetada ou o usuário autorizado:

- identifique a operação ou decisão questionada;
- informe o motivo da contestação;
- corrija ou apresente evidência sobre dados inexatos;
- receba confirmação do recebimento e orientação sobre as próximas etapas;
- obtenha uma resposta fundamentada e o registro do resultado da revisão.

Para decisões com impacto econômico relevante ou potencial de restringir direitos, recomenda-se revisão humana por pessoa que não esteja limitada a confirmar a saída automática. O revisor deverá poder manter, corrigir ou reverter o resultado, registrar a justificativa e encaminhar falhas sistêmicas à equipe técnica.

**Prazo interno proposto:** confirmar o recebimento em até 2 dias úteis e concluir a análise em até 15 dias úteis, salvo necessidade justificada de prazo adicional, comunicada à pessoa solicitante. Esses prazos são uma meta interna sugerida para o protótipo; não são apresentados como prazo legal geral. Prazos legais ou regulamentares aplicáveis sempre prevalecem.

## 8. Limites de divulgação

A explicação deverá ser suficiente para compreender o resultado, mas não precisa revelar:

- código-fonte integral, credenciais, chaves, tokens ou segredos de segurança;
- dados pessoais de terceiros;
- parâmetros cuja divulgação permita fraude ou exploração do sistema;
- segredos comerciais e industriais protegidos por lei.

A existência de segredo comercial não deve servir como resposta automática para negar toda explicação. A equipe deverá fornecer os critérios e procedimentos em nível adequado, com revisão jurídica quando houver conflito entre transparência, proteção de dados e segredo empresarial.

## 9. Registro e proteção das explicações

Toda decisão classificada para auditoria deverá produzir um registro com identificador da regra, versão, data/hora, módulo, resultado, código de motivo e referência da operação. Quando houver explicação exibida ao usuário, deve ser possível recuperar a versão efetivamente apresentada.

Os registros deverão:

- usar referência pseudonimizada ao titular, sempre que possível;
- limitar a entrada a dados necessários para explicar a decisão;
- impedir edição e exclusão por usuários comuns;
- ter acesso e exportação restritos;
- possuir prazo de retenção justificado pela finalidade e pelas obrigações aplicáveis;
- permitir relacionar a decisão original à revisão, sem apagar o histórico.

Não registrar senhas, tokens, códigos MFA, chaves criptográficas ou cópias integrais de cadastros pessoais em mensagens de explicação ou detalhes livres de auditoria.

## 10. Responsabilidades

| Papel | Responsabilidade |
|---|---|
| Responsável de negócio pelo módulo | Definir a finalidade da regra, confirmar os critérios e aprovar o texto da explicação. |
| Desenvolvimento | Versionar regras, manter logs estruturados, executar testes e corrigir falhas. Garantir que regras de negócio usem identificadores/categorias estáveis e que o ator venha da sessão autenticada. |
| Segurança | Controlar acesso aos registros, proteger integridade e tratar incidentes. |
| Encarregado/responsável por privacidade | Avaliar direitos dos titulares, minimização, retenção e atendimento a pedidos. |
| Revisor humano designado | Analisar contestação, verificar evidências e registrar decisão fundamentada. |
| Auditoria/gestão | Acompanhar indicadores, exceções, reclamações e correções. |

Em um projeto acadêmico, uma pessoa pode acumular mais de um papel, desde que isso seja declarado e que a revisão de decisões relevantes não seja tratada como mera confirmação automática.

## 11. Monitoramento e atualização

A política deverá ser revisada:

- antes da ativação de nova decisão automatizada;
- quando a regra, seus dados de entrada ou sua finalidade forem alterados;
- quando houver novo fornecedor, API ou modelo de terceiros;
- após incidente, reclamação recorrente ou indício de resultado discriminatório;
- ao menos uma vez por ano, como controle interno proposto.

Indicadores recomendados: quantidade de decisões automatizadas por tipo, contestações recebidas, tempo de revisão, percentual de resultados alterados, erros identificados, versões de regra em uso e incidentes relacionados. Indicadores por grupos de pessoas só devem ser produzidos quando houver base legal, necessidade e proteção adequada dos dados utilizados.

## 12. Texto curto de transparência para os usuários do ERP

> **Sobre decisões automatizadas no AutoBotz Motors**  
> Alguns fluxos do ERP utilizam regras automáticas para validar operações e calcular resultados, como disponibilidade de veículos e cobertura de garantia. Essas regras têm finalidade operacional e seguem critérios definidos pela equipe responsável. Quando um resultado puder afetar seus interesses, você poderá consultar os motivos relevantes, solicitar correção de dados e pedir revisão pelo canal de atendimento indicado pela organização. O AutoBotz Motors não deve utilizar dados pessoais para produzir decisões discriminatórias ou sem finalidade informada.

Este texto é um modelo e deve ser ajustado aos fluxos efetivamente ativados, aos canais reais de atendimento e à configuração de cada organização cliente antes de ser publicado.

## 13. Aprovação e vigência

Esta é uma proposta para discussão e aprovação pela equipe do AutoBotz Motors. Antes de se tornar política operacional, deverá receber responsável formal, canal de contestação real, prazo aprovado, definição de retenção e validação jurídica conforme o contexto de uso.

**Documento relacionado:** [`D1_RESPONSABILIDADE_ALGORITMICA.md`](D1_RESPONSABILIDADE_ALGORITMICA.md).
