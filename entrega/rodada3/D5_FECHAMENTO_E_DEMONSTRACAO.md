# D5 — Fechamento técnico, evidências e demonstração

AutoBotz Motors — Equipe B, 3ª rodada (novembro de 2026).  
Implementação inicial e integração: Arthur. Revisão complementar prevista: Adryan (não declarada como realizada sem registro).  
Referência técnica: `entrega/rodada3/D5_PLANO_RECUPERACAO.md`.

## 1. Situação da entrega em 17/11/2026

A implementação **didática** do D5 está integrada à `main` desde 15/11/2026. O escopo implementado e a regressão automatizada foram verificados. A proposta de continuidade entre regiões de nuvem não foi implantada e não deve ser apresentada como funcionalidade em operação.

O desafio descreve uma atualização defeituosa que afeta instâncias do ERP e a disponibilidade de logs. A solução separa **detecção e recuperação demonstráveis em ambiente isolado** das medidas de infraestrutura que exigiriam servidores, replicação de banco e testes em produção controlada.

## 2. Entregas verificáveis

| Requisito / situação | Implementação e evidência | Limite |
| --- | --- | --- |
| Capturar e conferir registros | `MonitorIntegridadeD5` cria duas cópias de arquivos `.log`, com manifesto SHA-256; verifica alteração, ausência e arquivo inesperado | Duas pastas não são duas regiões geográficas |
| Identificar corrupção sem apagar provas | `D5IntegridadeTest` testa cópias, adulteração, recuperação em novo destino, caminhos perigosos e links simbólicos | O hash sozinho não impede adulteração simultânea do arquivo e do manifesto |
| Observar falhas de forma periódica | Comando `MonitorIntegridadeD5 monitorar` encerra com erro ao detectar corrupção | Não existe integração automática com um serviço externo de alertas |
| Recuperar logs de transação em banco de teste | `D5RecuperacaoJdbcTest` simula indisponibilidade, falha SQL, rollback, perda e restauração de registros da tabela de auditoria | Apenas `autobotz_d5_test`; não representa restauração física da instância |
| Conferir backup e restauração SQL | `scripts/test-d5-backup-ci.sh` produz dump, confere SHA-256 e restaura em `autobotz_d5_restore_test` no MariaDB descartável do CI | Recusa uso fora do ambiente de teste configurado |
| Reagir a publicação defeituosa | `D5RollbackPortalTest` inicia o portal estável, simula publicação sem `index.html`, verifica HTTP 404 e retorna aos arquivos estáveis, conferindo catálogo e logs | Não reverte binário Java nem executa failover real |
| Orientar equipe/cliente | `D5_PLANO_RECUPERACAO.md` documenta isolamento, verificação, restauração, comunicação e metas propostas | RTO de 60 min e RPO de 15 min são objetivos, **não medições** |

## 3. Evidências já registradas no GitHub

- **PR #47**, integridade de logs e recuperação, integrado em 15/11/2026: https://github.com/athr-ocean/autobotz-motors/pull/47
- **PR #48**, atualização defeituosa e rollback do portal, integrado em 15/11/2026: https://github.com/athr-ocean/autobotz-motors/pull/48
- Commit de integração de referência: `4dd760b091b7effc9670164b459e51d26df020d4`.
- **Regressão integrada no commit de referência — sucesso**, inclusive etapas `Run D5 failure and database log recovery` e `Check D5 MariaDB backup and restore`: https://github.com/athr-ocean/autobotz-motors/actions/runs/43727273726
- **Regressão D12 — sucesso:** https://github.com/athr-ocean/autobotz-motors/actions/runs/43727273743
- **Regressão de segurança D14 — sucesso:** https://github.com/athr-ocean/autobotz-motors/actions/runs/43727273806

Os workflows acima validam o **commit indicado**, não substituem uma nova execução caso código compartilhado seja alterado posteriormente. Em uma **contingência isolada**, diferente da `main`, a validação local no Windows em 15/11 compilou 132 fontes e aprovou 17 suítes selecionadas sem banco local. Esse número inclui código adicional da contingência e **não** deve ser atribuído à versão publicada na `main`. Os ensaios JDBC e de backup/restauração do D5 integrado foram aprovados no CI do commit de referência.

## 4. Demonstração curta e reproduzível (sem acessar banco)

Executar **na raiz de uma cópia íntegra do repositório** com Git Bash e JDK instalados. Não usar o diretório original se ele contiver arquivos excluídos ou modificações não examinadas.

```bash
bash scripts/run.sh build
mkdir -p build/d5-tests
javac -encoding UTF-8 -cp "build/classes:src:lib/*" -d build/d5-tests \
  tests/D5IntegridadeTest.java tests/D5RollbackPortalTest.java
java -cp "build/classes:build/d5-tests:src:lib/*" D5IntegridadeTest
java -cp "build/classes:build/d5-tests:src:lib/*" D5RollbackPortalTest
```

Resultados esperados nos testes existentes:

```text
D5_INTEGRIDADE_OK checks=12
D5_ROLLBACK_PORTAL_OK checks=11
```

Esses números são **referências históricas**, não substituem observar o resultado da demonstração atual. Em Windows, o teste de links simbólicos depende da permissão de criação de symlink (por exemplo, Modo de Desenvolvedor corretamente habilitado).

**Sequência sugerida para explicar o ensaio (2–3 minutos):**

1. Apresentar o cenário de falha e o objetivo de não perder ou sobrescrever logs.
2. Mostrar `D5IntegridadeTest`: duas cópias, manifesto, detecção de adulteração e recuperação em nova pasta.
3. Mostrar `D5RollbackPortalTest`: versão estável responde 200; atualização defeituosa é detectada com 404; volta à versão estável e verifica página, catálogo e logs.
4. Abrir o workflow da regressão integrada e destacar os dois passos MariaDB, sem executar restauração em banco do computador ou da equipe.
5. Encerrar distinguindo **prova de conceito local** de **implantação com redundância geográfica**.

### Fala de apresentação

> No D5, simulamos uma atualização com falha e a indisponibilidade de registros. Primeiro, protegemos os arquivos de log com duas cópias e verificações SHA-256, para detectar alterações antes de recuperar qualquer arquivo. Depois, testamos um cenário em que uma atualização quebra a página do portal: o teste identifica o erro e retorna aos arquivos estáveis, preservando os logs. Também validamos rollback de operações SQL e restauração de backup em bancos descartáveis pelo GitHub Actions. Isso demonstra os mecanismos de recuperação em ambiente acadêmico; não estamos afirmando que o sistema já opera em duas regiões de nuvem.

## 5. Critérios de encerramento e limites

- [x] Implementação de integridade e recuperação integrada à `main`.
- [x] Simulação automatizada de rollback dos arquivos publicados do portal.
- [x] Teste JDBC de recuperação e ensaio de backup/restauração SQL aprovados no CI do commit de referência.
- [x] Plano técnico, procedimento de recuperação, modelo de comunicado e limitações documentados.
- [x] Registros de PRs e workflow vinculados para auditoria.
- [ ] Revisão individual de Adryan documentada (não há confirmação aqui).
- [ ] Demonstração oral/visual em aula (ainda não aconteceu).
- [ ] Nuvem multi-região, replicação e failover físico (fora do escopo implementado).
- [ ] Medição real de RTO/RPO e restauração por binlog (infraestrutura não disponível).

**Conclusão:** a **parte técnica implementável do D5 está pronta para a entrega acadêmica**. Itens não verificados permanecem explicitamente identificados. Nenhum dado real deve ser apagado ou restaurado para reproduzir a prova de conceito.

## 6. Isolamento em relação ao trabalho dos demais

Este fechamento não introduz mudanças nos desafios D1–D4. O D4 tem um desenvolvimento independente em andamento; qualquer mudança posterior nos arquivos do portal exige reexecutar o `D5RollbackPortalTest` e a regressão integrada antes de afirmar que a nova combinação está aprovada.
