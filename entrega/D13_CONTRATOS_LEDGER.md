# D13 — Gestão de contratos e prova de conceito de ledger

Este documento registra o escopo técnico do D13 e as limitações da implementação. O enunciado acadêmico pede um piloto de integração com blockchain simulada. O ERP implementa um ledger local encadeado por SHA-256 e uma API HTTP local de simulação; não há blockchain pública, consenso distribuído ou pagamento real.

## Ciclo de vida

1. **Criação:** contrato em `RASCUNHO`, associado ao ID estável do fornecedor. O ledger recebe o evento `CONTRATO_CRIADO`.
2. **Negociação:** `RASCUNHO → EM_NEGOCIACAO`; a mudança é registrada como novo evento.
3. **Assinatura:** `EM_NEGOCIACAO → ASSINADO`; o evento preserva o retrato dos dados e o hash anterior.
4. **Execução:** `ASSINADO → EM_EXECUCAO`; a nota fiscal pode ser validada e gera evento próprio.
5. **Vencimento:** o ERP registra uma notificação quando faltam de zero a dez dias para o vencimento. O evento não se duplica.
6. **Avaliação simulada:** o cliente HTTP local envia os campos autorizados à API de teste. A resposta do contrato inteligente simulado é gravada como `STATUS_API_SIMULADA_RECEBIDO` no ledger, vinculada ao hash da versão enviada.
7. **Liberação simulada:** se a API retorna elegibilidade, o serviço repete localmente as condições (nota fiscal validada, vencimento atingido e contrato não terminal) e registra o estado `PAGAMENTO_SIMULADO_LIBERADO`. Nenhuma transferência financeira ocorre.
8. **Finalização ou cancelamento:** a transição terminal impede novas mudanças de estado.

Transições válidas e regras de pagamento são definidas em `StatusContrato` e `ContratoService`.

## Dados e integridade

`contratos_fornecedor` guarda o ID do fornecedor, valor, vencimento, estado, validação de nota, estado de pagamento simulado e âncora do ledger (sequência e hash final). `ledger_contratos` guarda eventos sequenciais, retrato do contrato, hash do conteúdo, hash anterior, hash do registro e data/hora.

O hash de cada evento encadeia seu conteúdo ao evento anterior. A âncora no contrato permite detectar alteração, lacuna ou remoção de eventos, inclusive do último. Na leitura, a aplicação também compara o retrato do evento mais recente aos campos atuais do contrato. O mecanismo detecta alterações incompatíveis com o histórico, mas não é imutável contra alguém que altere simultaneamente ledger e âncora no banco.

## API simulada e segurança dos dados

`ApiBlockchainSimulada` disponibiliza cliente HTTP e servidor de teste em `127.0.0.1`. O cliente recusa endpoint externo, exige uma chave em cabeçalho e envia somente: ID do contrato, ID do fornecedor, valor, vencimento, estado, indicadores de nota/pagamento, sequência e hash do ledger. Nome, CNPJ, conta bancária e documento contratual não são enviados.

O servidor aceita apenas `POST` no caminho `/api/v1/contratos`, exige os campos de uma lista permitida, aplica limites ao corpo e devolve um protocolo SHA-256 local junto da decisão simulada. A chave serve como proteção didática do endpoint local; esta implementação HTTP não substitui TLS, gestão de segredos, autenticação corporativa ou uma API de produção.

O status `LIBERACAO_SIMULADA_ELEGIVEL` é uma decisão de elegibilidade da simulação, não uma ordem bancária. O ERP grava o retorno no ledger e mantém o processamento de pagamento como estado explicitamente simulado.

`ContratoService` exige a ação `INTEGRAR_API_SIMULADA` para esse fluxo. A interface que consumir o serviço deve mapear essa ação à permissão RBAC aprovada pela equipe de integração; este PR não liga a ação a uma tela Swing.

## Banco e dependências

A migration `013_d13_contratos_ledger.sql` referencia `fornecedores(id)`. A validação JDBC deve ocorrer em banco descartável, com a migration D11 `011_d11_compras.sql`, depois a migration D12 `012_d12_assinaturas.sql`, e então a 013, conforme o plano de integração da equipe. A branch de trabalho não cria nem substitui as migrations 011/012, que pertencem a outros integrantes.

Na integração da segunda rodada, as migrations **011–016 foram incluídas na `main`**, aplicadas e reaplicadas num banco descartável; `D13JdbcIntegrationTest` e os testes determinísticos de ledger/API passaram. A API e os pagamentos continuam simulados.

## Testes

- `D13LedgerIntegrityTest`: testes determinísticos de encadeamento, alteração/remoção, âncora, estados, janela de dez dias e condições de pagamento simulado.
- `D13BlockchainApiTest`: troca HTTP local real com a API simulada; verifica credencial, allowlist, status, protocolo e recusa de destinos externos. Usa contratos fictícios em memória.
- `D13JdbcIntegrationTest`: valida criação persistida, NF, aviso, transições, pagamento simulado, retorno da API gravado no ledger e detecção de alteração persistida. Só pode ser executado em `autobotz_d13_test` depois das migrations 011–013.

Resultados de execução devem ser registrados somente depois de rodar os comandos no ambiente indicado. Teste em memória ou HTTP local não comprova persistência JDBC.
