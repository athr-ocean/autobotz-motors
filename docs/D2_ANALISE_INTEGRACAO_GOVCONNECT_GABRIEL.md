# D2 — Análise da integração GovConnect
## Responsabilidade de Gabriel — Equipe B

**Tipo de entrega:** análise e proposta técnica.  
**Base:** atribuição da equipe recebida em 09/11/2026 e versão de main consultada em 16/11/2026.

> Limite importante: não recebemos a documentação oficial do serviço GovConnect (endpoints, contrato, sandbox, autenticação, escopos ou regras de negócio). O mapeamento abaixo é uma proposta para discussão com Alexandre, não uma especificação confirmada. Não foi feita chamada ou integração real.

## 1. Escopo

A atribuição de Gabriel no D2 — Demanda do Cliente é comparar REST, GraphQL e SOAP, propor autenticação federada, mapear os dados de compras do AutoBotz para o sistema externo e documentar a segurança da integração. A implementação do protótipo GovConnect é responsabilidade de Alexandre.

## 2. Situação do ERP

Na versão de main consultada, D11 já possui:

- fornecedores: id, nome e CNPJ;
- orcamentos_compra: id, nome, saldo inicial e saldo atual;
- solicitacoes_compra: orçamento, fornecedor, descrição, valor, status, solicitante, aprovador, motivo de rejeição e datas;
- pedidos_compra: solicitação, orçamento, fornecedor, valor e data de criação.

FornecedorDAO oferece busca por ID; ComprasService mantém aprovação, baixa de orçamento e geração do pedido na transação local. Esse é o modelo interno verificado. Não se presume que GovConnect use os mesmos campos.

## 3. Comparação de protocolos

| Aspecto | REST | GraphQL | SOAP |
|---|---|---|---|
| Modelo | Recursos e semântica HTTP; JSON é comum, não obrigatório. | Consultas e mutações definidas por schema tipado; cliente escolhe os campos. | Envelope XML e regras de processamento; pode ser descrito por WSDL/XSD. |
| Compras | Adequado a recursos e operações explícitas de pedido. | Útil para consultas compostas; pode ser excessivo para poucas operações transacionais. | Adequado quando o provedor já exige contrato formal ou WS-*. |
| Contrato | Pode ser documentado por OpenAPI. | Schema GraphQL define tipos e operações. | WSDL/XSD descrevem operações e mensagens. |
| Atenções | Semântica HTTP, idempotência, versões, limites, timeout. | Autorização por campo, custo e profundidade da consulta, erros parciais. | Tamanho/complexidade XML, compatibilidade do contrato e políticas do serviço. |
| Recomendação | Preferência provisória para o protótipo se o provedor suportar. | Não é primeira escolha para comandos de compra simples, salvo se for a interface oficial. | Usar se for o contrato oficial de GovConnect. |

A escolha final depende da documentação real do provedor. REST não significa apenas enviar JSON por HTTP; o cliente deve seguir métodos, status e contrato publicados. Não se devem inventar rotas e apresentá-las como reais.

## 4. Arquitetura proposta

Fluxo: ComprasService → interface GovConnectPort → adaptador GovConnectAdapter → transporte definido pelo contrato real.

O adaptador traduz entre DTOs externos e o domínio local. Não deve passar JSON/XML diretamente aos DAOs. ComprasService permanece responsável pelas regras de negócio e autorização.

A transação local não deve ficar aberta durante uma chamada de rede. Após confirmar o pedido local, registrar estado de integração e enviar a solicitação. Usar correlação e idempotência para evitar pedidos duplicados. Se o escopo do protótipo não incluir uma outbox, documentar a janela de inconsistência e não fazer retentativas cegas.

## 5. Mapeamento conceitual

Os nomes do lado GovConnect são semânticos e precisam ser substituídos pelos nomes do contrato oficial.

| AutoBotz | Uso externo proposto | Regras |
|---|---|---|
| pedidos_compra.id | referência local/idempotency key | Estável; nunca reutilizar em outro pedido. |
| pedidos_compra.solicitacao_id | referência da solicitação | Para correlação quando necessária. |
| pedidos_compra.fornecedor_id | referência da contraparte | Resolver por vínculo entre ID interno e ID externo; não assumir que são iguais. |
| fornecedores.nome | nome da contraparte | Enviar somente se requerido. |
| fornecedores.cnpj | identificador cadastral | Transmitir apenas se obrigatório, por canal protegido; não é credencial. |
| pedidos_compra.valor | valor do pedido | Decimal exato; moeda deve ser acordada, pois não há campo de moeda no modelo atual. |
| solicitacoes_compra.descricao | descrição/justificativa | Revisar para remover dados pessoais ou segredos desnecessários. |
| solicitacoes_compra.status | elegibilidade local | Enviar só após aprovação local; mapear estados explicitamente. |
| solicitante/aprovador e timestamps | auditoria mínima | Evitar nomes de usuários; enviar apenas se o contrato exigir. |
| orcamentos_compra.saldo_atual | controle financeiro interno | Não enviar salvo requisito expresso do provedor. |
| motivo_rejeicao | diagnóstico interno | Não transmitir por padrão. |

### Fluxo de referência

1. Usuário autorizado cria e aprova solicitação no ERP.
2. ERP confirma pedido e baixa de orçamento localmente.
3. Adaptador constrói DTO usando allowlist.
4. Cliente envia com chave de idempotência e correlation ID.
5. ERP valida resposta e armazena estado mínimo: ID externo, instante, status e código não secreto.
6. Timeout não é sucesso. Retentativa limitada somente se segura/idempotente; erro funcional fica pendente de ação humana.

## 6. Autenticação federada

Separar identidade de autorização. Login do AutoBotz continua identificando e autorizando usuários locais; uma identidade GovConnect, se houver, não concede automaticamente permissões de compras no ERP.

- Para login delegado de usuário, preferir OpenID Connect sobre OAuth 2.0, com Authorization Code e PKCE quando suportado. Validar assinatura, issuer, audience, expiração, state e nonce.
- Para comunicação serviço-a-serviço, avaliar credencial de cliente apenas se o provedor oferecer esse fluxo; restringir escopos e ambiente.
- OAuth é autorização delegada e, sozinho, não padroniza identidade do usuário; OIDC acrescenta essa camada.
- Não persistir access/refresh tokens em Git, logs ou tabelas de compras. Segredos vêm de configuração protegida do ambiente, com rotação e privilégio mínimo.
- Se GovConnect não suportar OIDC, seguir o mecanismo oficial e registrar a decisão; não improvisar federação com bearer token estático compartilhado.

Issuer, endpoints, cadastro, scopes, audience e expiração ainda precisam ser confirmados. Esta seção é proposta, não autenticação implementada.

## 7. Segurança e evidências

| Risco | Controle proposto | Verificação para o protótipo |
|---|---|---|
| Token ou segredo em código/log | Segredo fora do Git; redação de cabeçalhos | Diff e logs sem credenciais. |
| Acesso sem autorização | Validar principal/escopo e exigir permissão no serviço | Testar token inválido, escopo ausente e perfil negado. |
| Excesso de dados | DTO com allowlist e minimização | Confirmar que campos não permitidos não são serializados. |
| Duplicidade | Idempotency key e consulta de estado | Repetir operação sem duplicar pedido. |
| Indisponibilidade | Timeout finito, estado pendente, retentativa limitada | Timeout não marca operação como concluída. |
| Resposta malformada | Validar schema, tamanho e status | Testar resposta inválida. |
| Destino indevido/interceptação | HTTPS com validação TLS e host permitido | Rejeitar host não configurado. |
| Falta de auditoria | Correlation ID e logs estruturados mínimos | Rastrear tentativa sem token ou PII desnecessária. |
| Estado divergente | Estados explícitos e reconciliação | Testar falha antes/depois do envio. |

Usar somente credenciais e dados fictícios em sandbox. Não desativar validação TLS, não aceitar URL arbitrária fornecida pela interface e não registrar payload completo sem necessidade e retenção definida.

## 8. Alinhamentos necessários com Alexandre

Antes de fechar a implementação, registrar: documentação/endereço do sandbox; protocolo e contrato (OpenAPI, schema GraphQL ou WSDL/XSD); autenticação e escopos; operação remota e estado que a habilita; campos obrigatórios, moeda e limites; idempotência; erros e retentativas; retenção de logs; casos de teste e evidências do PR.

## 9. Validação desta contribuição

Esta contribuição é documental. O mapeamento foi confrontado com as tabelas e classes de compras disponíveis em main. Não foram executadas chamadas ao GovConnect, autenticação federada real, testes externos ou medições de segurança. Esses resultados só poderão ser declarados depois que Alexandre disponibilizar o contrato e ambiente de teste.

## Referências

- Fielding, R. T. REST, dissertação, cap. 5: https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm
- IETF RFC 9110, HTTP Semantics: https://www.rfc-editor.org/rfc/rfc9110
- GraphQL Foundation, Introduction to GraphQL: https://graphql.org/learn/
- W3C SOAP 1.2: https://www.w3.org/TR/soap12/
- IETF RFC 6749, OAuth 2.0: https://www.rfc-editor.org/rfc/rfc6749
- IETF RFC 9700, OAuth 2.0 Security BCP: https://www.rfc-editor.org/rfc/rfc9700
- OpenID Foundation, OpenID Connect Core 1.0: https://openid.net/specs/openid-connect-core-1_0.html
