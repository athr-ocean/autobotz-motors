# D2 — Implementação demonstrativa GovConnect (Alexandre)

## Objetivo

Demonstrar como um pedido de compra do AutoBotz Motors poderia ser convertido em uma requisição REST/JSON e enviado a um serviço externo com autenticação OAuth 2.0.

## Limite da implementação

O enunciado acadêmico não forneceu especificação oficial do GovConnect, OpenAPI, URL de homologação, issuer, token endpoint, scopes, credenciais ou regras definitivas. Por isso, o contrato, as rotas e os payloads desta prova de conceito são **provisórios**. O provedor de identidade e o servidor de pedidos usados pelo teste são mocks locais vinculados a `127.0.0.1`. Nenhuma conexão com o GovConnect governamental real é realizada.

## Decisões técnicas

- **Protocolo proposto: REST/JSON.** É uma opção simples para operações de envio e consulta de pedidos. A decisão depende de confirmação do órgão; se a especificação oficial exigir SOAP ou outro padrão, o adaptador deverá ser ajustado.
- **Autenticação demonstrada: OAuth 2.0 Client Credentials.** É um fluxo máquina-a-máquina. Não substitui a autenticação de pessoas no ERP. O ambiente real precisa confirmar o provedor federado, issuer, audience, scopes e método de validação do token.
- **Operações provisórias:** `POST /api/v1/pedidos` para envio e `GET /api/v1/pedidos/{protocolo}` para consulta.
- **Idempotência:** a referência estável do pedido é enviada em `Idempotency-Key`; a API simulada devolve o mesmo protocolo em reenvios com a mesma chave.
- **Correlação:** o cliente envia `X-Correlation-ID` para rastreabilidade.
- **Transações:** a chamada externa não deve ocorrer dentro da transação JDBC de aprovação da compra. Para operação resiliente, recomenda-se Outbox transacional e worker separado, ainda não implementados.

## Arquivos

- `src/autobotz/integracao/govconnect/GovConnectIntegracao.java`: DTOs, cliente HTTP, erros tipados, validações e servidor simulado.
- `src/autobotz/integracao/govconnect/OAuth2ClientCredentialsTokenProvider.java`: requisição e cache de token OAuth 2.0.
- `src/autobotz/integracao/govconnect/GovConnectPedidoService.java`: ponte entre consultas do D11 e o cliente GovConnect.
- `tests/GovConnectIntegracaoTest.java`: testes automatizados de protocolo e segurança, sem banco.
- `tests/GovConnectComprasJdbcTest.java`: integração D11 + D2 em MariaDB descartável.
- `scripts/test-d2-govconnect.sh`: compilação e execução isolada dos testes sem banco e sem internet.

O mapeamento `PedidoRequest.de(PedidoCompra, SolicitacaoCompra, Fornecedor)` exige uma solicitação no estado `PEDIDO_GERADO`, confere os vínculos entre pedido, solicitação e fornecedor, usa a descrição da solicitação e transmite o CNPJ cadastral quando disponível. O ID numérico interno do fornecedor é usado apenas para validação local e não é enviado como identificador da contraparte externa. A moeda BRL é uma hipótese exclusiva do protótipo, pois o modelo D11 não possui campo de moeda.

A classe `GovConnectPedidoService` exige primeiro a ação `ComprasService.Acao.ENVIAR_PEDIDO_GOVCONNECT`; só depois consulta a solicitação, o pedido persistido e o fornecedor, cada consulta ainda protegida por `CONSULTAR`. A matriz de autorização atual concede `AutorizacaoService.Permissao.ENVIAR_PEDIDO_GOVCONNECT` apenas ao perfil ADMIN; VENDEDOR e perfil ausente são recusados. O chamador do serviço deve mapear a ação de envio para essa permissão específica, nunca reutilizar `ACESSAR_COMPRAS` como autorização de envio. A chamada HTTP ocorre somente após as consultas encerrarem suas transações JDBC. A integração não altera aprovação, saldo ou status de compras. O teste `GovConnectComprasJdbcTest` demonstra o fluxo em MariaDB descartável `autobotz_*_test`, com limpeza dos registros criados ao final. O protocolo externo não é persistido e não há Outbox ou recuperação durável nesta entrega.

## Como executar

Na raiz do repositório, com JDK 21 ou superior e Bash (Linux, macOS ou Git Bash no Windows):

```bash
bash scripts/test-d2-govconnect.sh
```

O script compila somente as classes de domínio necessárias e a integração D2 em uma pasta temporária. Não precisa configurar `AUTOBOTZ_DB_URL`, usuário, senha, MariaDB ou acesso à internet.

## Resultado esperado

O teste unitário mostra cada verificação com `[PASS]` ou `[FAIL]`, apresenta um resumo do pedido fictício e termina com algo como:

```text
RESULTADO: 12/12 verificações aprovadas
STATUS: SUCESSO
ATENÇÃO: o resultado comprova somente o protótipo com mocks locais;
         não comprova comunicação com o GovConnect governamental real.
```

O protocolo e o horário exibidos são gerados dinamicamente pelo servidor simulado; portanto, mudam a cada execução.

## Verificações realizadas

O teste sem banco executa 12 verificações:

1. Mapeamento de `PedidoCompra`, `SolicitacaoCompra` e `Fornecedor` para o DTO.
2. OAuth 2.0 Client Credentials e emissão do token pelo IdP local.
3. Cache e reutilização do token.
4. Envio do pedido e recebimento de protocolo simulado.
5. Idempotência ao repetir o envio.
6. Consulta pelo protocolo.
7. Rejeição de token inválido (HTTP 401).
8. Validação de dados obrigatórios e valor.
9. Correspondência entre pedido e fornecedor.
10. Bloqueio de HTTP para API externa.
11. Bloqueio de HTTP para token endpoint externo.
12. Bloqueio de envio antes do status `PEDIDO_GERADO`.

O teste JDBC adicional executa 12 verificações: duas de autorização (envio negado sem a ação específica e negação antes de qualquer consulta), conexão com banco descartável, criação de fornecedor/orçamento/solicitação, aprovação D11, envio e reenvio idempotente ao GovConnect simulado, recebimento de protocolo e preservação do saldo/status local.

## Alinhamento com a análise de Gabriel

A escolha REST/JSON permanece provisória, condicionada ao contrato oficial. O OAuth 2.0 Client Credentials implementado cobre somente comunicação máquina-a-máquina, se esse grant for aceito pelo provedor; não implementa login federado de usuário. Para autenticação delegada de pessoas, a análise de Gabriel recomenda avaliar OpenID Connect com Authorization Code + PKCE, caso suportado pelo órgão. Issuer, audience, scopes, assinatura/introspecção e credenciais continuam pendentes de confirmação. O CNPJ é o identificador cadastral enviado quando disponível; o ID numérico interno do fornecedor não é presumido como ID externo. A moeda BRL é apenas um dado de teste, não um requisito confirmado.

## Autorização do envio externo

O envio é uma ação de negócio independente da consulta. `ComprasService.Acao.ENVIAR_PEDIDO_GOVCONNECT` é verificada antes de consultar solicitação, pedido ou fornecedor. O mapeamento desktop concede a permissão correspondente apenas ao perfil ADMIN na matriz atual; VENDEDOR e perfil ausente não podem enviar. As consultas subsequentes continuam exigindo `CONSULTAR`. O teste JDBC inclui uma tentativa sem a permissão de envio e verifica que nenhuma consulta é iniciada.

## Segurança e próximos passos

O cliente exige HTTPS fora de loopback, utiliza timeouts de conexão/requisição, não imprime token ou segredo, mantém cache com renovação antecipada e evita retry automático cego. O mock apenas compara um token fictício; ele não valida assinatura JWT, issuer, audience ou introspecção.

Antes de qualquer homologação real, é necessário obter a documentação do órgão, confirmar contrato, identidade federada, scopes, campos obrigatórios, limites, política de idempotência e códigos de erro. Depois disso, deverão ser implementados persistência de outbox/auditoria, worker, retentativas limitadas, circuit breaker e conciliação, com testes em ambiente oficialmente autorizado.
