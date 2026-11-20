# D1 — Política de explicabilidade de decisões automatizadas

**Projeto:** AutoBotz Motors — Equipe B  
**Responsável pela proposta:** João  
**Versão:** 1.1 — 19 de novembro de 2026  
**Status:** proposta para revisão e aprovação da equipe. Não é certificação jurídica nem declaração de conformidade com a lei hipotética do desafio.  
**Código de referência:** `main`, commit `b00bc55c267edaa893f39dee9a850fc8f0abc46f`.

Os códigos R1–R9 (regras automáticas) e H1–H3 (decisões de usuários) vêm da [análise do D1](D1_RESPONSABILIDADE_ALGORITMICA.md), que também descreve o modelo de registro de auditoria. Este documento trata do que a pessoa afetada deve receber e de como contestar.

## 1. Objetivo e abrangência

Definir como o AutoBotz deve informar o resultado de uma decisão automatizada, explicar seus motivos e permitir contestação e revisão, sem expor dados pessoais, credenciais, segredos comerciais ou detalhes que enfraqueçam a segurança.

Aplica-se a regras automáticas que alterem valores, disponibilidade, acesso ou atendimento (R1–R9), a decisões de usuários condicionadas por regras do sistema (H1–H3) e a qualquer módulo ou integração que venha a ser adicionado. A política distingue regra determinística, controle de acesso, decisão humana assistida e sistema de IA: um `if/else` ou um cálculo não deve ser descrito como inteligência artificial.

## 2. Princípios

1. **Transparência:** a pessoa afetada sabe quando um resultado relevante foi produzido ou condicionado por automação.
2. **Explicação compreensível:** resultado e motivos em linguagem simples, com os fatores realmente usados.
3. **Proporcionalidade:** quanto maior o impacto possível, mais completa a explicação e mais forte a revisão.
4. **Não discriminação:** não usar critérios proibidos ou substitutos injustificados.
5. **Qualidade dos dados:** permitir corrigir dado inexato e refazer o resultado.
6. **Supervisão responsável:** o revisor humano pode alterar a decisão.
7. **Minimização e segurança:** não repetir dados pessoais desnecessários em mensagens ou logs.
8. **Rastreabilidade:** cada decisão auditável indica regra, versão, motivo e operação. O responsável humano vem da **sessão autenticada**, nunca de nome digitado em formulário.

## 3. O que funciona hoje e o que é proposta

Esta tabela evita apresentar como pronto o que ainda é proposta.

| Item | Hoje no ERP (commit `b00bc55`) | Proposta |
|---|---|---|
| Mensagem de bloqueio de login com tempo restante (R3) | **Existe** (`login.bloqueado`, `gui.login_blocked`) | Informar que o motivo é o número de tentativas e orientar o contato com o administrador |
| Mensagem de veículo indisponível (R2) | **Existe**, texto genérico ("Veiculo nao esta disponivel.") | Mostrar o status usado e como corrigi-lo |
| Detalhe da garantia (R1) | **Parcial**: o item aparece com valor zerado e a marca "garantia" | Mostrar dias desde a venda, prazo e motivo |
| Registro da decisão com regra, versão e motivo | **Não existe** (`logs_auditoria` sem uso pelos serviços) | Registro estruturado (análise, seção 4) |
| Identidade do aprovador de compras pela sessão (H1) | **Não funciona como deveria**: campo de texto editável | Correção descrita na análise, seção 6, dependente do responsável pelo módulo |
| Texto de transparência para os usuários do ERP | **Não existe** | Seção 9 deste documento |
| Solicitação de explicação pela pessoa afetada | **Não existe** | Seção 7 |
| Contestação e revisão humana | **Não existe** | Seção 7 |
| Prazos de atendimento e revisão | **Não existem** | Sugestão interna da equipe (seção 7) |

## 4. Informação antes e durante o uso

Quando uma automação puder afetar interesses de uma pessoa, a interface, o aviso ou o material de suporte deve informar: que há regra automática no fluxo; a finalidade; as categorias de dados usadas; se o resultado é automático, uma recomendação ou uma decisão humana apoiada pelo sistema; e como pedir explicação, correção ou revisão. A informação deve estar em português claro e nos demais idiomas do ERP (EN-US), e não apenas em documento técnico.

## 5. Conteúdo mínimo da explicação

1. **Resultado:** o que foi permitido, negado, calculado ou alterado.
2. **Motivo principal:** a regra ou condição que levou ao resultado.
3. **Dados considerados:** categorias e valores pertinentes, com mascaramento quando necessário.
4. **Regra e versão:** quando ajudar a compreender o caso.
5. **Limitações:** dados ausentes ou exceções que possam mudar o resultado.
6. **Próxima ação:** o que a pessoa pode corrigir ou solicitar.
7. **Revisão:** canal e procedimento, quando existirem.

A explicação deve refletir o que realmente ocorreu. Não pode ser inventada depois nem afirmar que uma pessoa revisou algo que não foi revisado.

## 6. Exemplos de mensagens propostas

As mensagens abaixo são **sugestões** e ainda não estão nos arquivos `messages_*.properties`.

**R1 — Garantia da revisão.** "A revisão foi coberta pela garantia porque a venda ocorreu há 120 dias e o prazo desta regra é de 365 dias. Se a data da venda estiver incorreta, peça a conferência do registro."

**R2 — Veículo indisponível.** "A venda não foi registrada porque o veículo está com o status [status]. Confira o cadastro do veículo."

**R3 — Bloqueio de login.** "Por segurança, este usuário foi bloqueado temporariamente após 5 tentativas sem sucesso. Tente de novo em [tempo]. Se não foi você, avise o administrador." Não informar se o nome de usuário existe.

**R7 — Pagamento simulado (operador).** Quando liberado: "Liberado porque a nota fiscal foi marcada como validada, o vencimento chegou e o contrato não está encerrado." Quando não liberado: listar apenas as condições não atendidas entre as quatro que a regra exige.

**H1 — Compra aprovada.** "Solicitação aprovada por [usuário da sessão]. O sistema verificou o saldo e a situação pendente; a aprovação foi decisão do usuário." Na rejeição, mostrar o motivo informado. O relatório não deve atribuir ao algoritmo uma escolha feita por uma pessoa.

**R9 — Acesso negado.** "Seu perfil não tem permissão para esta ação. Fale com o administrador se acredita que deveria ter."

## 7. Solicitação de explicação, contestação e revisão (proposta)

**Nada deste fluxo existe no ERP.** É uma proposta de funcionamento.

A pessoa afetada ou o usuário autorizado deveria poder: identificar a operação questionada; informar o motivo; corrigir ou apresentar evidência de dado inexato; receber confirmação do recebimento; e obter resposta fundamentada, com o resultado da revisão registrado e ligado à decisão original, sem apagar o histórico.

Para decisões de impacto econômico relevante, a revisão deveria ser feita por pessoa que possa **manter, corrigir ou reverter** o resultado, e que não seja quem operou a decisão contestada. Falhas sistêmicas vão para a equipe técnica.

**Prazos sugeridos pela equipe (não são obrigações legais).** Confirmar o recebimento em até 2 dias úteis e concluir a análise em até 15 dias úteis, com prazo adicional justificado e comunicado. Foram escolhidos como meta interna para o protótipo, sem base em norma. Prazos legais ou regulamentares prevalecem sempre. Um exemplo: o regulamento do Cadastro Positivo consultado (art. 19) fixa sete dias úteis para o resultado da revisão de decisão exclusivamente automatizada baseada em consulta a esse cadastro; o ERP não tem módulo de crédito, mas o exemplo mostra que um módulo desse tipo teria prazo próprio, a conferir no texto vigente.

## 8. Limites de divulgação

A explicação deve bastar para a pessoa entender o resultado, mas não precisa revelar: código-fonte, credenciais, chaves ou tokens; dados pessoais de terceiros; parâmetros cuja divulgação permita fraude; segredos comerciais protegidos por lei. Segredo comercial não deve servir como resposta automática para negar toda explicação; se houver conflito entre transparência, proteção de dados e segredo, a decisão exige revisão jurídica.

## 9. Texto curto de transparência (modelo)

> **Sobre decisões automatizadas no AutoBotz Motors**  
> Alguns fluxos do ERP usam regras automáticas para validar operações e calcular resultados, como a disponibilidade de veículos e a cobertura de garantia de revisões. Essas regras têm finalidade operacional e seguem critérios definidos pela equipe. Quando um resultado puder afetar seus interesses, você poderá consultar os motivos, pedir correção de dados e solicitar revisão pelo canal indicado pela organização.

O modelo deve ser ajustado aos fluxos ativos e aos canais reais antes de ser publicado. Enquanto o canal da seção 7 não existir, o texto não deve prometer revisão.

## 10. Registro das explicações

Cada decisão auditável deve gerar registro com regra, versão, data e hora, módulo, resultado, código de motivo e referência da operação, mais a versão da explicação exibida. Os campos e os exemplos completos (garantia e compras) estão na [análise, seção 4](D1_RESPONSABILIDADE_ALGORITMICA.md). Regras gerais: referência pseudonimizada ao titular quando possível; sem senhas, tokens, códigos MFA, chaves ou cópia de cadastros; edição e exclusão proibidas a usuários comuns; consulta e exportação restritas; retenção justificada pela finalidade e pela obrigação aplicável, a definir pela equipe.

## 11. Responsabilidades

| Papel | Responsabilidade |
|---|---|
| Responsável de negócio do módulo | Definir finalidade e critérios da regra e aprovar o texto da explicação |
| Desenvolvimento | Versionar regras, usar categorias estáveis, gravar o responsável a partir da sessão e testar |
| Segurança | Controlar acesso aos registros e proteger a integridade |
| Responsável por privacidade | Avaliar direitos dos titulares, minimização e retenção |
| Revisor humano designado | Analisar contestações e registrar decisão fundamentada |
| Auditoria e gestão | Acompanhar indicadores, exceções e correções |

Em um projeto acadêmico, uma pessoa pode acumular papéis, desde que declare isso e que a revisão de decisões relevantes não seja mera confirmação automática.

## 12. Revisão da política

Revisar antes de ativar nova decisão automatizada; quando mudarem regra, dados de entrada ou finalidade; quando entrar integração ou modelo de terceiros; após incidente, reclamação recorrente ou indício de resultado discriminatório; e ao menos uma vez por ano, como controle interno proposto. Indicadores sugeridos: decisões por tipo, contestações recebidas, tempo de revisão, percentual de resultados alterados, erros e versões de regra em uso. Indicadores por grupo de pessoas só com base legal, necessidade e proteção adequada.

## 13. Aprovação e vigência

Esta é uma proposta para discussão. Antes de virar política operacional, precisa de responsável formal, canal real de contestação, prazos aprovados, definição de retenção e validação jurídica conforme o contexto de uso.
