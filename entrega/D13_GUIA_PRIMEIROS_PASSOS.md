# Primeiros passos — contratos com fornecedor (D13)

Este guia descreve o fluxo didático de contratos no AutoBotz Motors. A implementação atual é uma prova de conceito local; não envia dados para uma blockchain pública e não realiza pagamentos.

## 1. Criar e acompanhar um contrato

O operador seleciona um fornecedor existente pelo ID interno, informa valor e vencimento, e cria o contrato em `RASCUNHO`. As mudanças permitidas são:

`RASCUNHO → EM_NEGOCIACAO → ASSINADO → EM_EXECUCAO → FINALIZADO`

O contrato pode ser cancelado antes de chegar a um estado terminal. Contratos finalizados ou cancelados não reabrem pelo fluxo normal.

## 2. Validar a nota e acompanhar o vencimento

Na etapa de execução, um usuário autorizado valida a nota fiscal no ERP. O conteúdo do documento não é enviado à API simulada. O processamento de vencimentos gera uma notificação única quando faltam de zero a dez dias.

## 3. Consultar a decisão simulada

O cliente do ERP envia para o endpoint de teste local somente IDs, valor, data, estado, indicadores operacionais e hash do ledger. A chave de demonstração vai no cabeçalho HTTP; não coloque segredos em URL, código versionado ou logs.

O retorno pode indicar:

- `AGUARDANDO_NOTA_FISCAL`: ainda falta validação no ERP;
- `AGUARDANDO_VENCIMENTO`: a data ainda não chegou;
- `LIBERACAO_SIMULADA_ELEGIVEL`: as condições da regra didática foram atendidas;
- `PAGAMENTO_SIMULADO_JA_LIBERADO`: o ERP já registrou a liberação simulada;
- `CONTRATO_ENCERRADO`: o contrato foi cancelado ou finalizado.

O status e o protocolo do retorno são acrescentados ao ledger. Se o retorno indicar elegibilidade, o serviço confere novamente nota fiscal, vencimento e estado do contrato e registra a liberação simulada em seguida. A elegibilidade não movimenta dinheiro; essa liberação é apenas um estado demonstrativo.

## 4. O que a trilha de integridade prova

Cada evento é ligado ao hash do evento anterior e a âncora da última sequência fica no registro do contrato. A verificação compara o histórico e os dados atuais. Uma adulteração, remoção ou divergência é detectável quando a âncora original permanece preservada.

Esse recurso não é uma blockchain distribuída: ele roda na base local do ERP e não protege contra alguém que consiga alterar simultaneamente os eventos e a âncora. Para a demonstração, descreva-o como ledger local/PoC.

## 5. Antes de uma validação JDBC

Use somente `autobotz_d13_test`, preparado com as migrations combinadas pela equipe. Nunca aplique migrations de teste sobre a base normal do ERP. Se não tiver certeza da URL ativa, interrompa antes de executar comandos de banco e peça conferência ao responsável pela integração.
