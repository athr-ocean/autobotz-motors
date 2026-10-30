# D13 — Roteiro da prova de conceito e plano de testes

## Objetivo

Demonstrar que o ERP forma uma trilha local de eventos de contrato, envia um retrato mínimo a uma API de blockchain simulada, recebe uma decisão do contrato inteligente simulado e registra o retorno vinculado à versão enviada.

## Demonstração sem banco

Na raiz do projeto, com JDK 21 ou superior:

```powershell
$sources = Get-ChildItem src\autobotz -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -Xlint:all -cp src -d build/classes $sources
javac -encoding UTF-8 -Xlint:all -cp "build/classes;src" -d build/tests tests\D13BlockchainApiTest.java tests\D13LedgerIntegrityTest.java
java -cp "build/tests;build/classes;src" D13BlockchainApiTest
java -cp "build/tests;build/classes;src" D13LedgerIntegrityTest
```

`D13BlockchainApiTest` inicia um endpoint HTTP efêmero em `127.0.0.1`, envia contratos fictícios e encerra o servidor ao terminar. Não usa banco, não chama rede externa e não representa teste JDBC. Espera-se que cada cenário imprima `PASS` e que a última linha seja `D13_BLOCKCHAIN_API_OK checks=9`.

## Cenário de apresentação

1. Enviar contrato sem nota validada e observar `AGUARDANDO_NOTA_FISCAL`.
2. Enviar contrato com nota validada, mas vencimento futuro, e observar `AGUARDANDO_VENCIMENTO`.
3. Enviar contrato ativo, vencido e com nota validada; observar `LIBERACAO_SIMULADA_ELEGIVEL` e a atualização transacional do indicador local de pagamento simulado.
4. Mostrar que contrato cancelado/finalizado recebe `CONTRATO_ENCERRADO` e que estado já liberado não é repetido.
5. Demonstrar que chave incorreta resulta em HTTP 401, destino externo é recusado e campo fora da allowlist é rejeitado.
6. Mostrar que o protocolo da resposta referencia o ID e o hash enviado. A integração do serviço grava o status recebido como evento do ledger.
7. Alterar o payload persistido num banco de teste e executar a verificação de integridade para demonstrar a detecção da adulteração.

## Teste JDBC integrado

As migrations 011–016 foram aplicadas em sequência e reaplicadas no banco descartável `autobotz_d13_test`, sem erros. O `D13JdbcIntegrationTest` foi executado e aprovado no CachyOS em 29/10/2026. As rotinas de integração também foram executadas pelo GitHub Actions nas revisões seguintes, incluindo a PR #44.

O teste nunca deve ser executado contra o banco com dados reais: configure `AUTOBOTZ_DB_URL` e as credenciais para um banco de testes isolado.

## Evidências e limitações

Na revisão local de 28/10/2026, a compilação das fontes e dos três testes D13 com `javac -Xlint:all` passou; `D13LedgerIntegrityTest` terminou com `D13_LEDGER_OK checks=22` e `D13BlockchainApiTest` terminou com `D13_BLOCKCHAIN_API_OK checks=9`. O teste da API foi executado com HTTP de loopback permitido e usou apenas contratos fictícios em memória.

O teste JDBC foi executado e aprovado após a integração das migrations 011–016, conforme a validação da segunda rodada. Pagamento e plataforma blockchain permanecem simulados.
