# D2 — Implementação demonstrativa GovConnect (Alexandre)

## Objetivo

Demonstrar como um pedido de compra do AutoBotz Motors poderia ser convertido em uma requisição REST/JSON e enviado a um serviço externo com autenticação OAuth 2.0.

## Limite da implementação

O enunciado acadêmico não forneceu especificação oficial do GovConnect, OpenAPI, URL de homologação, issuer, token endpoint, scopes, credenciais ou regras definitivas. Por isso, o contrato, as rotas e os payloads desta prova de conceito são **provisórios**. O provedor de identidade e o servidor de pedidos usados pelo teste são mocks locais vinculados a \`127.0.0.1\`. Nenhuma conexão com o GovConnect governamental real é realizada.

## Decisões técnicas

- **Protocolo proposto: REST/JSON.** É uma opção simples para operações de envio e consulta de pedidos. A decisão depende de confirmação do órgão; se a especificação oficial exigir SOAP ou outro padrão, o adaptador deverá ser ajustado.
- **Autenticação demonstrada: OAuth 2.0 Client Credentials.** É um fluxo máquina-a-máquina. Não substitui a autenticação de pessoas no ERP. O ambiente real precisa confirmar o provedor federado, issuer, audience, scopes e método de validação do token.
- **Operações provisórias:** \`POST /api/v1/pedidos\` para envio e \`GET /api/v1/pedidos/{protocolo}\` para consulta.
- **Idempotência:** a referência estável do pedido é enviada em \`Idempotency-Key\`; a API simulada devolve o mesmo protocolo em reenvios com a mesma chave.
- **Correlação:** o cliente envia \`X-Correlation-ID\` para rastreabilidade.
- **Transações:** a chamada externa não deve ocorrer dentro da transação JDBC de aprovação da compra. Para operação resiliente, recomenda-se Outbox transacional e worker separado, ainda não implementados.

## Arquivos

- \`src/autobotz/integracao/govconnect/GovConnectIntegracao.java\`: DTOs, cliente HTTP, erros tipados, validações e servidor simulado.
- \`src/autobotz/integracao/govconnect/OAuth2ClientCredentialsTokenProvider.java\`: requisição e cache de token OAuth 2.0.
- \`tests/GovConnectIntegracaoTest.java\`: testes automatizados e relatório de execução no terminal.
- \`scripts/test-d2-govconnect.sh\`: compilação e execução isolada, sem banco e sem internet.

O mapeamento \`PedidoRequest.de(PedidoCompra, Fornecedor, descricao)\` valida que o pedido já possui ID e que o fornecedor corresponde ao ID registrado no pedido. A prova de conceito ainda não consulta o MariaDB, não altera \`ComprasService\` e não persiste protocolo ou histórico externo.

## Como executar

Na raiz do repositório, com JDK 21 ou superior e Bash (Linux, macOS ou Git Bash no Windows):

\`\`\`bash
bash scripts/test-d2-govconnect.sh
\`\`\`

O script compila somente as classes de domínio necessárias e a integração D2 em uma pasta temporária. Não precisa configurar \`AUTOBOTZ_DB_URL\`, usuário, senha, MariaDB ou acesso à internet.

## Resultado esperado

O teste mostra cada verificação com \`[PASS]\` ou \`[FAIL]\`, apresenta um resumo do pedido fictício e termina com algo como:

\`\`\`text
RESULTADO: 11/11 verificações aprovadas
STATUS: SUCESSO
ATENÇÃO: o resultado comprova somente o protótipo com mocks locais;
         não comprova comunicação com o GovConnect governamental real.
\`\`\`

O protocolo e o horário exibidos são gerados dinamicamente pelo servidor simulado; portanto, mudam a cada execução.

## Verificações realizadas

1. Mapeamento de \`PedidoCompra\` e \`Fornecedor\` para o DTO.
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

## Segurança e próximos passos

O cliente exige HTTPS fora de loopback, utiliza timeouts de conexão/requisição, não imprime token ou segredo, mantém cache com renovação antecipada e evita retry automático cego. O mock apenas compara um token fictício; ele não valida assinatura JWT, issuer, audience ou introspecção.

Antes de qualquer homologação real, é necessário obter a documentação do órgão, confirmar contrato, identidade federada, scopes, campos obrigatórios, limites, política de idempotência e códigos de erro. Depois disso, deverão ser implementados persistência de outbox/auditoria, worker, retentativas limitadas, circuit breaker e conciliação, com testes em ambiente oficialmente autorizado.
