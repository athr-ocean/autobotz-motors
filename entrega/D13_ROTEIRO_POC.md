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

## Teste JDBC pendente de ambiente

O teste de integração `D13JdbcIntegrationTest` está preparado para `autobotz_d13_test`. Antes de executá-lo, a equipe precisa disponibilizar as migrations 011 (D11) e 012 (D12); aplique-as e depois a 013 em uma base descartável. Configure `AUTOBOTZ_DB_URL`, `AUTOBOTZ_DB_USER` e `AUTOBOTZ_DB_PASSWORD` somente para essa base e inclua MariaDB Connector/J no classpath.

Com as dependências preparadas, compile e execute:

```powershell
$jar = "$env:USERPROFILE\.m2\repository\org\mariadb\jdbc\mariadb-java-client\3.5.10\mariadb-java-client-3.5.10.jar"
javac -encoding UTF-8 -Xlint:all -cp "build/classes;src;$jar" -d build/tests tests\D13JdbcIntegrationTest.java
java -cp "build/classes;build/tests;src;$jar" D13JdbcIntegrationTest
```

O teste recusa uma URL que não identifique `autobotz_d13_test`. Ele cria e remove somente suas próprias linhas marcadas com prefixo `d13it_`. Não configure a URL do banco normal ou de produção.

## Evidências e limitações

Na revisão local de 28/10/2026, a compilação das fontes e dos três testes D13 com `javac -Xlint:all` passou; `D13LedgerIntegrityTest` terminou com `D13_LEDGER_OK checks=22` e `D13BlockchainApiTest` terminou com `D13_BLOCKCHAIN_API_OK checks=9`. O teste da API foi executado com HTTP de loopback permitido e usou apenas contratos fictícios em memória.

`D13JdbcIntegrationTest` foi compilado, mas não executado: ainda faltam as migrations 011/012 e não há MariaDB local acessível nesta sessão. Até essa execução real, a persistência do contrato, a gravação do status da API no banco e a detecção de adulteração no MariaDB permanecem sem comprovação de integração. Pagamento e plataforma blockchain permanecem simulados.
