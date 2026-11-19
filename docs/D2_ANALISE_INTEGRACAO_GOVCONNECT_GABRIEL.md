# D2 — Análise da integração GovConnect
## Responsabilidade de Gabriel — Equipe B

**Tipo de entrega:** análise e proposta técnica.  
**Atualizado:** 18/11/2026, após comparar as informações de Alexandre com o código integrado em main.

> **Limite confirmado:** o projeto não recebeu documentação oficial, endereço de homologação, issuer, token endpoint ou contrato do GovConnect governamental. O serviço, provedor de token e dados usados nos testes são simulados em loopback (127.0.0.1). A implementação demonstra um possível fluxo e não comprova comunicação com o serviço real.

## 1. Escopo

A responsabilidade de Gabriel no D2 — Demanda do Cliente é comparar REST, GraphQL e SOAP, propor autenticação federada, mapear dados de compras do AutoBotz e documentar a segurança da integração. Alexandre implementou o protótipo. Esta análise agora distingue o que seu código realmente faz das opções que continuam dependendo do contrato oficial.

## 2. Situação do ERP e do protótipo integrado

O modelo de compras existente possui fornecedores, orçamentos, solicitações e pedidos. O pedido se relaciona a fornecedor, orçamento e solicitação. FornecedorDAO permite buscar fornecedor por ID; ComprasService mantém aprovação, baixa do orçamento e geração de pedido na transação local.

O protótipo integrado em main oferece:

- REST com JSON: POST /api/v1/pedidos e GET /api/v1/pedidos/{protocolo};
- autenticação OAuth 2.0 Client Credentials e Bearer token;
- cache de token com renovação antecipada;
- Idempotency-Key no envio do pedido e X-Correlation-ID para correlação;
- timeout de conexão de 3 segundos e de requisição de 8 segundos;
- HTTPS exigido para serviços externos; HTTP somente para endereços de loopback em testes;
- nenhuma repetição automática nesta versão.

**Esclarecimento sobre as mensagens de Alexandre:** a descrição mais recente, confirmada pela implementação da main, substitui a referência anterior a X-API-Key. O código atual usa OAuth 2.0 Client Credentials; não implementa X-API-Key.

O OAuth 2.0 Client Credentials é autenticação/autorização entre sistemas, sem usuário humano presente. Portanto, demonstra uma credencial de serviço, mas não comprova SSO ou federação de identidade de um usuário do ERP. Se o enunciado exigir login federado de pessoas, OpenID Connect deve ser avaliado como fluxo separado.

## 3. REST, GraphQL e SOAP

| Aspecto | REST | GraphQL | SOAP |
|---|---|---|---|
| Modelo | Recursos e semântica HTTP; JSON é comum, não obrigatório. | Consultas e mutações contra schema tipado; cliente escolhe os campos. | Mensagens XML segundo envelope e regras de processamento; pode usar WSDL/XSD. |
| Adequação a pedidos | Bom para operações explícitas de envio e consulta. | Útil em consultas compostas; pode ser excessivo para poucas operações transacionais. | Adequado se o provedor oferecer contrato formal ou exigir WS-*. |
| Contrato | Pode ser documentado em OpenAPI. | Schema GraphQL define tipos e operações. | WSDL/XSD define operações e mensagens. |
| Atenções | Semântica HTTP, idempotência, versões, limites e timeout. | Autorização por campo, custo/profundidade de consulta e erros parciais. | Compatibilidade do contrato e políticas do serviço. |
| Decisão atual | REST/JSON é a proposta provisória usada no protótipo local. | Não selecionado. | Não selecionado; usar se o contrato real assim exigir. |

A escolha final depende da especificação oficial. REST não significa apenas transmitir JSON por HTTP; devem ser respeitados o contrato, métodos, respostas e códigos HTTP publicados. Não há evidência de que o GovConnect real aceite REST.

## 4. Mapeamento de dados

O código integrado mapeia o pedido para um DTO com referência ERP, nome do fornecedor, CNPJ cadastral quando disponível, valor, moeda BRL e descrição. O ID numérico local do fornecedor é usado somente para validar os vínculos internos e não é enviado como identificador externo. A referência ERP é derivada do ID persistido do pedido. O mapeamento exige solicitação no estado `PEDIDO_GERADO`, confere que pedido, solicitação e fornecedor correspondem e que o valor do pedido é igual ao valor aprovado. Valida também referência, nome, formato do CNPJ quando informado, valor positivo com até duas casas decimais, moeda BRL e descrição.

| Dado interno | Situação no DTO atual | Decisão proposta para integração real |
|---|---|---|
| ID do pedido | Enviado como referência ERP, por exemplo AUTOBOTZ-PEDIDO-42 | Confirmar formato aceito e usar também como chave idempotente se o provedor concordar. |
| ID do fornecedor | Não enviado; usado apenas na validação dos vínculos locais | Não presumir que ID interno seja ID GovConnect; acordar tabela de correspondência se o contrato exigir. |
| Nome do fornecedor | Enviado | Confirmar necessidade e campo no contrato oficial. |
| CNPJ | Enviado quando disponível, com validação local de 14 dígitos | Confirmar necessidade e formato no contrato externo; nunca tratar como segredo de autenticação. |
| Valor | Enviado | Hoje o protótipo fixa BRL. Confirmar moeda, escala, arredondamento e limites oficiais. |
| Descrição | Enviada com validação local de tamanho | Reduzir ao mínimo necessário e impedir inclusão de PII, credenciais ou dados sigilosos. |
| ID da solicitação e do orçamento | Não são enviados no DTO atual | Incluir somente se o contrato externo exigir e houver necessidade de negócio. |
| Solicitante, aprovador e saldo do orçamento | Não são enviados | Permanecem internos por padrão; não transmitir sem requisito explícito. |

A resposta de sucesso simulada contém protocolo, referência ERP, status, instante de recebimento e indicação de que o retorno é simulado. Erros incluem status HTTP e código/mensagem quando disponíveis. O teste não confirma que o GovConnect real use esse formato ou os mesmos status.

## 5. Arquitetura e fluxo

O adaptador GovConnect deve traduzir entre o domínio de compras e os DTOs externos; os DAOs não devem conhecer JSON/XML. ComprasService permanece responsável por regras e autorização.

Fluxo proposto:

1. Usuário autorizado aprova solicitação e o AutoBotz cria o pedido local.
2. O adaptador monta o DTO com allowlist de campos.
3. O cliente envia a operação com Bearer token, Idempotency-Key e X-Correlation-ID.
4. O resultado é validado e associado à referência local.
5. O protocolo e estado externo só devem ser persistidos quando houver uma decisão de integração aprovada para o projeto.

O protótipo atual consulta dados reais do D11 por `ComprasService` e envia somente após essas consultas encerrarem suas transações JDBC; a chamada externa não integra a transação de aprovação. `GovConnectPedidoService` exige `ComprasService.Acao.ENVIAR_PEDIDO_GOVCONNECT` antes de qualquer consulta, e as leituras continuam exigindo `CONSULTAR`. Na matriz desktop atual, a permissão específica `ENVIAR_PEDIDO_GOVCONNECT` é concedida somente a ADMIN; VENDEDOR e perfil ausente são recusados. O protocolo externo não é persistido e não há Outbox, worker nem recuperação durável. Para operação resiliente, considerar Outbox transacional e reconciliação como evolução, não como funcionalidade implementada. Timeout não deve ser tratado como sucesso. Como não há retry automático, uma política futura precisa respeitar a idempotência oficial e distinguir erros temporários de erros funcionais.

## 6. Autenticação federada

A implementação atual usa Client Credentials para o serviço simulado. O provedor local de teste emite token fictício; o servidor simulado apenas compara o Bearer token. Não valida assinatura JWT, issuer, audience nem introspecção. Isso não equivale à validação completa de um provedor real.

Para integração real:

- usar Client Credentials somente se GovConnect documentar um fluxo de máquina para máquina; restringir scopes, ambiente e vida da credencial;
- validar a forma de emitir e validar token indicada pelo provedor oficial; não assumir que o token seja JWT;
- manter segredo fora do Git e dos logs, carregar por configuração protegida e rotacioná-lo;
- se a demanda envolver login federado de usuário, avaliar OpenID Connect separado, com Authorization Code e PKCE se suportado. OAuth 2.0, sozinho, não padroniza autenticação do usuário;
- nunca reutilizar a identidade de serviço como se fosse a identidade da pessoa que aprovou o pedido; a autorização local continua necessária.

Issuer, token endpoint, scopes, audience, client registration, claims e expiração seguem pendentes de confirmação oficial.

## 7. Segurança e evidências

| Risco | Controle proposto | Evidência necessária |
|---|---|---|
| Segredo/token exposto | Segredo fora do repositório; nunca registrar cabeçalho Authorization | Revisar diff e logs. |
| Token ou escopo inválido | Rejeição por padrão; validar conforme contrato oficial | Testar token ausente/inválido e escopo insuficiente. |
| Dados além do necessário | DTO allowlist e minimização | Confirmar campos serializados e rejeitar extras. |
| Pedido duplicado | Idempotency-Key estável no POST | Reenvio retorna mesma operação no mock; confirmar semântica no serviço real. |
| Destino ou transporte indevido | HTTPS e validação de certificado fora de loopback; endpoint em configuração confiável | Teste recusa HTTP/destino externo sem TLS. |
| Indisponibilidade | Timeouts finitos; não marcar falha de rede como sucesso | Teste de timeout e estado pendente. |
| Falta de rastreabilidade | X-Correlation-ID e registro mínimo sem credenciais | Rastrear sucesso/erro sem token ou payload excessivo. |
| Divergência entre sistemas | Reconciliar estado externo e local; avaliar outbox | Testar falha antes/depois do envio e reprocessamento. |

Não desabilitar validação TLS. Usar somente dados fictícios no protótipo; qualquer escrita real exige sandbox oficialmente autorizado.

## 8. Testes reportados pelo autor do protótipo

O teste `scripts/test-d2-govconnect.sh` executa 12 verificações sem banco de dados e sem internet, usando servidores locais em 127.0.0.1. Os casos incluem mapeamento, emissão/cache de token, envio, reenvio idempotente, consulta, token inválido e validações. O teste JDBC `GovConnectComprasJdbcTest` executa 11 verificações, incluindo recusa de envio sem a permissão específica e antes de consultas, integração com MariaDB descartável, aprovação D11, envio, reenvio idempotente e preservação do saldo/status local. Esses resultados comprovam somente os mocks e o banco de testes, não uma integração com GovConnect real.

## 9. Alinhamento pendente com Alexandre/provedor

Antes de homologação real, confirmar documentação e endereço oficiais, protocolo, campos obrigatórios, formato de moeda/CNPJ, fluxo de autenticação, issuer/token endpoint/scopes, limites, semântica de idempotência, status/códigos de erro, retentativas e política de retenção/auditoria.

## Referências

- Fielding, R. T. REST, dissertação, capítulo 5: https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm
- IETF RFC 9110, HTTP Semantics: https://www.rfc-editor.org/rfc/rfc9110
- GraphQL Foundation, Introduction to GraphQL: https://graphql.org/learn/
- W3C SOAP 1.2: https://www.w3.org/TR/soap12/
- IETF RFC 6749, OAuth 2.0: https://www.rfc-editor.org/rfc/rfc6749
- IETF RFC 9700, OAuth 2.0 Security BCP: https://www.rfc-editor.org/rfc/rfc9700
- OpenID Foundation, OpenID Connect Core 1.0: https://openid.net/specs/openid-connect-core-1_0.html
