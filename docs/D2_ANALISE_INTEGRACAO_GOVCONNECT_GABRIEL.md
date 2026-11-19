# D2 — Análise da integração GovConnect
## Responsabilidade de Gabriel — Equipe B

**Revisão:** 19/11/2026, comparada com a implementação e evidências do PR #57 de Alexandre.  
**Estado de referência:** PR #57 aberto, head a5637c98f3fed500620c852c2ff81a48a0dae1d9; check integrado concluído com sucesso. As mudanças descritas nesta revisão estão no candidato do PR #57 e só devem ser tratadas como parte da main após a integração desse PR.

> **Limite do sistema:** não há especificação oficial, endpoint de homologação, issuer, token endpoint, scopes, credenciais ou regras definitivas do GovConnect governamental. A API e o provedor usados nos testes são mocks locais. Não houve conexão com o serviço real.

## 1. Escopo

A responsabilidade de Gabriel no D2 — Demanda do Cliente é comparar REST, GraphQL e SOAP, propor autenticação federada, mapear os dados de compras e documentar a segurança da integração. Alexandre implementou o protótipo. Esta revisão descreve o código proposto no PR #57 e distingue suas evidências locais dos requisitos ainda desconhecidos do provedor real.

## 2. Comparação de protocolos

| Aspecto | REST | GraphQL | SOAP |
|---|---|---|---|
| Modelo | Recursos e semântica HTTP; JSON é comum, não obrigatório. | Consultas e mutações contra schema tipado; cliente escolhe os campos. | Mensagens XML, normalmente com contrato WSDL/XSD. |
| Adequação a pedidos | Bom para operações explícitas de envio e consulta. | Útil para consultas compostas; pode ser excessivo para poucas operações transacionais. | Adequado se o provedor oferecer contrato formal ou exigir WS-*. |
| Contrato | Pode ser documentado por OpenAPI. | Schema GraphQL define tipos e operações. | WSDL/XSD define operações e mensagens. |
| Atenções | Semântica HTTP, idempotência, versões, limites e timeout. | Autorização por campo, custo/profundidade e erros parciais. | Compatibilidade do contrato e políticas do serviço. |
| Decisão do protótipo | REST/JSON é a escolha provisória implementada no PR #57. | Não selecionado. | Não selecionado; deve ser usado se o contrato oficial exigir. |

A decisão final depende da documentação oficial de GovConnect. REST não é apenas enviar JSON por HTTP: métodos, respostas e semântica precisam seguir o contrato real. Não há evidência de que o órgão aceite as rotas implementadas.

## 3. Integração com o modelo de compras

### 3.1 Consultas e pré-condições

A classe GovConnectPedidoService recebe um ComprasService e um cliente HTTP. Para enviar por ID da solicitação, ela:

1. consulta a solicitação no serviço D11;
2. exige o status PEDIDO_GERADO;
3. consulta o pedido persistido da solicitação;
4. consulta o fornecedor pelo ID local associado ao pedido;
5. cria o DTO e só então chama o cliente HTTP.

As consultas concluem suas próprias transações JDBC antes da chamada externa. A operação HTTP não ocorre dentro da transação de aprovação de compras. O envio não altera a aprovação, o saldo do orçamento ou o status local do pedido.

### 3.2 Dados efetivamente enviados

O DTO do PR #57 contém referência ERP, nome do fornecedor, CNPJ opcional, valor, moeda e descrição. A construção valida que o pedido existe, que a solicitação está em PEDIDO_GERADO, que pedido/solicitação/fornecedor correspondem e que o valor do pedido coincide com o valor aprovado.

| Dado interno | Presença no DTO do PR #57 | Regra/limite |
|---|---|---|
| ID do pedido | Convertido em referência ERP AUTOBOTZ-PEDIDO-{id} | Não é o ID numérico do fornecedor nem protocolo externo. |
| ID do fornecedor | Usado só para validar vínculos localmente | Não é transmitido como identificador externo. |
| CNPJ do fornecedor | Enviado quando disponível; pode ser nulo | Validado como 14 dígitos. A aceitação pelo GovConnect real não está confirmada. |
| Nome do fornecedor | Enviado | Confirmar se o contrato real exige o campo. |
| Valor | Enviado como decimal | Positivo, até duas casas decimais. |
| Moeda | Fixa em BRL | Hipótese demonstrativa; o modelo D11 não possui moeda. |
| Descrição | Vem da solicitação D11 | Validada localmente até 200 caracteres; revisar minimização para uso real. |
| ID da solicitação e do orçamento | Não enviados | Permanecem internos na versão atual. |
| CNPJ não cadastrado / dados do solicitante e aprovador / saldo do orçamento | Não enviados no DTO | Manter fora do intercâmbio salvo necessidade e contrato explícitos. |

A referência do pedido no ERP é derivada do ID local do pedido. O protocolo externo é outra informação, devolvida pelo servidor remoto/simulado.

## 4. Protocolo, idempotência e resposta

Rotas provisórias no cliente:

- POST /api/v1/pedidos para enviar o DTO;
- GET /api/v1/pedidos/{protocolo} para consultar pelo protocolo.

O POST exige Idempotency-Key igual à referência ERP e envia X-Correlation-ID. A consulta GET não envia Idempotency-Key. A resposta simulada contém protocolo UUID, referência ERP, status RECEBIDO, instante de recebimento e indicador simulado. Respostas HTTP fora dos códigos de sucesso são convertidas em erro tipado com status, código e mensagem.

No servidor de teste, a idempotência é mantida em memória por chave e faz reenvio com a mesma chave retornar o mesmo protocolo enquanto o processo estiver ativo. Isso demonstra deduplicação no mock, mas não persistência, duração garantida nem comportamento do GovConnect real. O mock não vincula a chave a um hash do payload para detectar conteúdo diferente sob a mesma referência. A semântica de idempotência deve ser confirmada com o órgão.

O cliente não persiste o protocolo ou estado externo no MariaDB. Embora haja método GET no cliente HTTP, o fluxo JDBC de envio não salva o protocolo nem automatiza consulta/reconciliação posterior.

## 5. Autenticação e autorização

O OAuth 2.0 Client Credentials fornece token para comunicação entre sistemas sem usuário humano presente. O provider busca e mantém token em cache e renova antecipadamente. O cliente envia Bearer token no POST e GET. Em teste, o provedor de tokens e o servidor são locais; o mock aceita um token fictício e não valida assinatura JWT, issuer, audience ou introspecção.

Este grant não representa login federado/SSO de uma pessoa. Se a exigência envolver autenticação delegada de usuário, OpenID Connect deve ser avaliado separadamente (Authorization Code + PKCE, se o provedor suportar). A autenticação oficial, o issuer, os scopes, a audience e as credenciais seguem pendentes.

**Autorização local observada:** GovConnectPedidoService obtém os dados através de consultas do ComprasService, cujos métodos exigem a ação CONSULTAR pelo autorizador injetado. Não foi identificada nesta mudança uma permissão separada para enviar dados a GovConnect, nem integração com tela/launcher. O teste JDBC usa autorizador permissivo (todas as ações autorizadas); por isso, não comprova bloqueio de um usuário sem permissão. Antes de expor o envio no produto, definir e testar autorização específica para essa ação. Não confundir o Bearer token de serviço com autorização do usuário no AutoBotz.

## 6. Falhas de rede e recuperação

O cliente impõe timeout de conexão de 3 segundos e timeout de requisição de 8 segundos; exige HTTPS para destino externo e aceita HTTP apenas em loopback de teste. Não há repetição automática.

Erros HTTP retornam GovConnectException; falhas de transporte e interrupção propagam IOException/InterruptedException ao chamador. O teste de PR #57 não demonstra um cenário de timeout ou conexão interrompida. Se a conexão cair depois de o serviço remoto aceitar o POST, o resultado pode ficar indeterminado.

Não existe Outbox, worker, gravação local de estado de envio ou recuperação durável. Como o protocolo também não é persistido, uma reinicialização perde o histórico local do intercâmbio. A chave idempotente estável pode ajudar numa repetição controlada se o provedor real garantir a mesma semântica, mas o mock em memória não garante deduplicação após reinício. Para operação real, planejar persistência/outbox, reconciliação e política de reenvio, sem tratar falha de rede como sucesso.

## 7. Testes e evidências do PR #57

O check do GitHub Actions integrated-regression para o head a5637c98f3fed500620c852c2ff81a48a0dae1d9 terminou com conclusão success em 18/11/2026. Os logs do job registram:

- teste sem banco GovConnectIntegracaoTest: 12/12 verificações aprovadas;
- integração D11 + GovConnect em MariaDB descartável: GOVCONNECT_COMPRAS_JDBC_OK=10;
- serviço simulado em loopback, sem chamada à rede governamental;
- suite D11 reportou 10 testes aprovados.

As verificações cobrem mapeamento, OAuth2 de máquina a máquina no mock, cache do token, POST, GET por protocolo, reenvio idempotente, token inválido, validações de campos/status/vínculos, leitura JDBC, protocolo simulado e preservação do saldo/status local. O teste de banco usa autobotz_d11_test, remove apenas os IDs criados na execução e a integração de rede ocorre depois das consultas JDBC.

Não há, nessa suíte, teste de autorização negada, falha/timeout de rede, reenvio após reiniciar o mock ou persistência/reconciliação de protocolo. Os resultados comprovam o protótipo integrado em CI com mocks e banco descartável; não comprovam GovConnect real.

## 8. Segurança e itens pendentes

| Risco | Controle/evidência atual | Limite que permanece |
|---|---|---|
| ID interno exposto como identidade externa | PR #57 não serializa o ID numérico do fornecedor; envia CNPJ quando existe | Confirmar o identificador e formato oficiais. |
| Segredo/token em log | Cliente documenta não expor token em URL/log; usar somente dados de teste | Confirmar armazenamento e rotação do segredo em configuração real; nenhum launcher real é exercitado. |
| Transporte sem proteção | HTTPS exigido fora de loopback; teste cobre recusa de HTTP externo | Ainda não há endpoint real com certificado validado em homologação. |
| Pedido duplicado | Idempotency-Key baseada na referência ERP; mock repete protocolo em memória | Garantia durável/semântica real depende do provedor. |
| Ação sem autorização | Consultas chamam autorizador CONSULTAR | Falta permissão específica de envio e teste de negação antes de disponibilizar a ação. |
| Indisponibilidade/resultado incerto | Timeouts e exceções explícitas; sem retry automático | Sem Outbox, estado persistido, worker ou reconciliação durável. |
| Dados excessivos | DTO omite ID do fornecedor, ID de orçamento/solicitação e dados de usuário | Revisar descrição/CNPJ e necessidade de cada campo com contrato real. |

## 9. Confirmações necessárias antes de integração real

Obter contrato e sandbox oficiais; confirmar protocolo/rotas, autenticação e scopes, campos obrigatórios, identificação do fornecedor, moeda, formato de resposta, idempotência, códigos de erro, limites, retentativas, persistência/reconciliação, autorização de envio e retenção de logs.

## Referências

- Fielding, R. T. REST, dissertação, capítulo 5: https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm
- IETF RFC 9110, HTTP Semantics: https://www.rfc-editor.org/rfc/rfc9110
- GraphQL Foundation, Introduction to GraphQL: https://graphql.org/learn/
- W3C SOAP 1.2: https://www.w3.org/TR/soap12/
- IETF RFC 6749, OAuth 2.0: https://www.rfc-editor.org/rfc/rfc6749
- IETF RFC 9700, OAuth 2.0 Security BCP: https://www.rfc-editor.org/rfc/rfc9700
- OpenID Foundation, OpenID Connect Core 1.0: https://openid.net/specs/openid-connect-core-1_0.html
