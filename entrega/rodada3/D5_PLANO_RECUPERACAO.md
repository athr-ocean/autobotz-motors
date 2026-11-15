# D5 — Problemas técnicos: integridade e recuperação

Equipe B — AutoBotz Motors  
Responsável pela implementação inicial: Arthur  
3ª rodada, novembro de 2026

## 1. Problema

No cenário passado pelo professor, uma atualização incorreta deixa parte das instâncias do ERP indisponíveis e alguns logs de transação temporariamente inacessíveis. A prioridade é recuperar o serviço sem agravar a falha e sem restaurar dados que não foram conferidos.

O ERP atual usa MariaDB. O histórico de auditoria fica na tabela logs_auditoria; não existe, nesta versão, uma operação real em duas regiões de nuvem. Por isso, separamos a proposta de infraestrutura do mecanismo que conseguimos executar e testar localmente.

## 2. O que foi implementado nesta etapa

A classe MonitorIntegridadeD5 trabalha sobre uma pasta com arquivos .log previamente exportados ou gerados para teste. Ela:

- cria duas cópias independentes dos mesmos arquivos, cada uma com manifesto SHA-256;
- verifica se faltou um log, se o conteúdo mudou ou se apareceu um arquivo inesperado na cópia;
- compara uma pasta de origem com o estado registrado na captura;
- recupera uma cópia íntegra em uma pasta nova, sem apagar nem substituir os arquivos anteriores;
- oferece um modo de monitoramento periódico que termina com erro se detectar corrupção.

As cópias são gravadas primeiro em diretórios temporários e depois movidas para os destinos. A ferramenta recusa caminhos de saída dentro da origem, destinos existentes e links simbólicos nos arquivos analisados.

A ferramenta de arquivos não substitui um backup completo do MariaDB. O teste JDBC descrito adiante exporta e recupera registros específicos de auditoria num banco descartável; ele não faz restauração física do banco nem reinicia instâncias da aplicação. O manifesto SHA-256 detecta alterações em relação à captura, mas, sozinho, não protege contra alguém que consiga modificar os arquivos e o manifesto ao mesmo tempo.

## 3. Como testar

Na raiz do repositório, execute:

    bash scripts/run.sh build

Em seguida, crie uma pasta de exemplo fora do projeto:

    mkdir -p "$HOME/AutoBotz-D5/origem"
    printf 'pedido=100;valor=250\n' > "$HOME/AutoBotz-D5/origem/pedidos.log"
    printf 'evento=compra_aprovada\n' > "$HOME/AutoBotz-D5/origem/auditoria.log"

Capture duas cópias, representando locais diferentes:

    java -cp build/classes autobotz.util.MonitorIntegridadeD5 capturar "$HOME/AutoBotz-D5/origem" "$HOME/AutoBotz-D5/regiao-a/copia-01" "$HOME/AutoBotz-D5/regiao-b/copia-01"

Verifique e compare:

    java -cp build/classes autobotz.util.MonitorIntegridadeD5 verificar "$HOME/AutoBotz-D5/regiao-a/copia-01"
    java -cp build/classes autobotz.util.MonitorIntegridadeD5 comparar "$HOME/AutoBotz-D5/origem" "$HOME/AutoBotz-D5/regiao-b/copia-01"

Para acompanhar a integridade a cada dez segundos:

    java -cp build/classes autobotz.util.MonitorIntegridadeD5 monitorar "$HOME/AutoBotz-D5/regiao-b/copia-01" 10

Interrompa com Ctrl+C. Em outro terminal, altere uma das cópias para conferir o alerta. Para recuperar os logs ainda íntegros da outra réplica:

    java -cp build/classes autobotz.util.MonitorIntegridadeD5 restaurar "$HOME/AutoBotz-D5/regiao-a/copia-01" "$HOME/AutoBotz-D5/recuperado"

O destino de recuperação precisa ser novo. Para repetir o teste, escolha um outro nome para a pasta. Nunca faça esses ensaios usando logs de clientes ou um banco de produção.

Teste automatizado:

    javac -encoding UTF-8 -cp build/classes -d build/classes tests/D5IntegridadeTest.java
    java -cp build/classes D5IntegridadeTest

O teste verifica as duas cópias, adulteração, log ausente, restauração, bloqueio de sobrescrita, captura vazia, caminhos inseguros e links simbólicos. O workflow de integração também executa essa classe.

### Teste com MariaDB

O segundo teste usa apenas `autobotz_d5_test` em `localhost:3306`. Ele registra dois eventos de auditoria, simula uma operação SQL inválida e confere o rollback, verifica a resposta a uma conexão indisponível, exporta os dois logs com SHA-256, apaga um dos registros de teste e o recupera da cópia íntegra.

O workflow também executa `scripts/test-d5-backup-ci.sh`: faz um dump SQL completo da base descartável, confere o SHA-256 do arquivo, altera dados de teste de uma tabela e restaura a cópia em outra base (`autobotz_d5_restore_test`). Ao final, remove a tabela e a base auxiliares. Esse ensaio não utiliza nem modifica bancos de clientes e recusa execução fora do GitHub Actions.

Para reproduzir localmente, crie **uma base descartável** `autobotz_d5_test`, importe `schema.sql` e aplique as migrações 011–016 conforme `db/migrations/README.md`. Nunca faça isso em um banco com dados reais. Depois, no terminal com Java e MariaDB configurados:

```bash
export AUTOBOTZ_DB_URL='jdbc:mariadb://127.0.0.1:3306/autobotz_d5_test?sslMode=disable'
export AUTOBOTZ_DB_USER='SEU_USUARIO_DE_TESTE'
# Defina AUTOBOTZ_DB_PASSWORD no terminal sem gravá-la no repositório.
bash scripts/test-d5-recuperacao.sh
```

O script recusa nomes de banco diferentes de `autobotz_d5_test`. O teste cria apenas os próprios registros e os remove ao terminar; não derruba o serviço de banco. No GitHub Actions, essa base é criada automaticamente e descartada após a execução.

### Simulação de atualização defeituosa do portal

O teste `D5RollbackPortalTest` usa o servidor HTTP já existente no AutoBotz. Ele guarda os arquivos atuais do portal em uma pasta temporária, inicia a versão estável e confere a página inicial, o JavaScript e a API do catálogo. Depois, publica uma cópia de teste sem `index.html`, verifica o erro HTTP 404 e volta aos arquivos da versão estável.

Ao retornar, o teste confere novamente a interface, a API e as cópias dos logs com SHA-256. Os arquivos reais do projeto não são modificados. Para executar após compilar o ERP e os testes, use:

```bash
java -cp "build/classes:build/d5-tests:src:lib/*" D5RollbackPortalTest
```

Se o teste ainda não estiver compilado, use o procedimento padrão de compilação dos testes do projeto ou acompanhe a execução no GitHub Actions. Esse ensaio cobre o rollback dos **arquivos publicados do portal**, não uma reversão de binários Java, nem a troca de servidores entre regiões reais.

## 4. Plano de recuperação para uma implantação real

A proposta é manter a aplicação em uma região principal e uma instalação de espera em outra região. O banco precisaria de replicação própria, com acompanhamento do atraso entre as regiões. Backups completos e logs de transação do banco (binlogs) teriam de ser guardados em armazenamento separado, com retenção, controle de acesso e proteção contra alterações.

O procedimento previsto em caso de falha é:

1. Suspender a publicação da atualização problemática e registrar o horário do primeiro erro.
2. Avaliar quais instâncias respondem e se o banco principal está consistente. Preservar os registros ainda disponíveis.
3. Isolar a versão com problema e decidir entre voltar à versão anterior ou ativar a instalação de espera.
4. Conferir a integridade do backup, o ponto de restauração e o atraso da replicação antes de promover uma cópia.
5. Evitar duas instâncias gravando como principal ao mesmo tempo.
6. Restabelecer o acesso, testar login, consulta, venda e gravação de logs, e só então liberar o serviço.
7. Registrar o incidente, o tempo de indisponibilidade, as transações que precisaram de conferência e as ações preventivas.

Como objetivo inicial de planejamento, podemos trabalhar com recuperação em até 60 minutos (RTO) e perda de dados limitada a até 15 minutos (RPO). São metas propostas para discussão, não valores medidos ou garantidos. O tipo de replicação, o orçamento e os ensaios periódicos definiriam metas viáveis.

Uma cópia local em duas pastas não é redundância geográfica. A execução em regiões distintas, o failover de rede, a consistência do MariaDB e a restauração por binlog ainda exigem infraestrutura e testes específicos.

## 5. Testes que a equipe deverá registrar

| Ensaio | O que conferir | Situação inicial |
| --- | --- | --- |
| Cópia normal | Duas réplicas com o mesmo manifesto | Coberto pelo teste D5 |
| Arquivo alterado | Falha de hash e alerta | Coberto pelo teste D5 |
| Arquivo ausente | Falha de integridade | Coberto pelo teste D5 |
| Recuperação local | Cópia íntegra recuperada em novo caminho | Coberto pelo teste D5 |
| Conexão indisponível | Falha de conexão em porta fechada e novo acesso à base | Incluído no teste JDBC; não interrompe o servidor |
| Operação SQL com erro | Rollback e preservação do log de auditoria | Incluído no teste JDBC; não equivale a rollback de versão do ERP |
| Recuperação do log no MariaDB | Apagar e restaurar um registro de teste da cópia verificada | Incluído no teste JDBC, somente banco descartável |
| Backup e restauração SQL | Gerar dump, alterar tabela de teste e restaurar em outra base | Incluído no workflow, somente MariaDB descartável |
| Atualização defeituosa do portal | Detectar falha HTTP, reverter os arquivos e conferir a interface e os logs | Teste automatizado com servidor HTTP local e pastas temporárias |
| Falha de região e troca de infraestrutura | Continuidade do serviço e RPO/RTO | Pendente de ambiente em nuvem |
| Restauração de binlogs | Reconstrução de transações sem divergências | Pendente de infraestrutura de backup |

Os testes automatizados cobrem falha de conexão, rollback de transação, recuperação de registros de auditoria, restauração de backup SQL e retorno da versão dos arquivos do portal depois de uma publicação defeituosa. Não houve reversão de um executável Java em produção, interrupção física do MariaDB ou comutação entre regiões. As metas de RTO e RPO não foram medidas.

## 6. Modelo de comunicado técnico ao cliente

**Assunto: Instabilidade no AutoBotz Motors**

Identificamos uma instabilidade após uma atualização do sistema, com impacto no acesso a algumas instâncias do ERP. A equipe está verificando a integridade dos registros e preparando a recuperação do serviço.

Neste momento, ainda não há confirmação sobre eventual perda permanente de dados. Por segurança, recomendamos evitar repetir operações que tenham apresentado erro até a confirmação da normalização.

Publicaremos uma nova atualização em [horário]. Em caso de dúvida, entre em contato pelo canal de suporte informado em seu contrato.

Equipe técnica — AutoBotz Motors

Este texto é um modelo para o exercício. Não representa um incidente ocorrido nem uma comunicação enviada a clientes.

## 7. Próximos passos

Arthur: manter o teste JDBC na regressão e avaliar como operacionalizar exportações periódicas de logs em um ambiente de teste. A recuperação de banco completo exige uma estratégia específica de backup.

Adryan: revisar os cenários automatizados, registrar os resultados e conferir o comportamento da interface durante uma demonstração manual. Não foram realizados testes de failover em nuvem.

A equipe deve apresentar o que efetivamente executou no ambiente didático, sem afirmar que possui duas regiões de nuvem em operação.
