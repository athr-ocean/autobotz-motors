# Checklist final — AutoBotz Motors (Rodada 2, 29/10/2026)

**Critério:** FEITO = implementação e verificação no escopo disponível; COM RESSALVA = funcionalidade simulada ou sem demonstração externa; NÃO COMPROVADO = ausência de evidência suficiente.

| Item | Estado | Evidência |
|---|---|---|
| D6–D10: regressão principal | FEITO | `CoreRegression` JDBC passou |
| D11: compras/fornecedores, orçamento, aprovação/rollback | FEITO | `D11ComprasTest`, `D11ComprasJdbcTest` passaram |
| D11: permissão e testes automatizados | FEITO | `D11ComprasPermissaoTest` e workflow integrado passaram |
| D11: medição de tempo manual x automatizado | NÃO COMPROVADO | sem medição manual comparável |
| D11: economia de energia comprovada | NÃO COMPROVADO | sem telemetria de consumo |
| D12: assinaturas, cancelamento, indicadores, cenários | FEITO COM RESSALVA | `AssinaturaServiceTest`, `AssinaturaDAOJdbcTest` e `docs/D12_PLANOS_SIMULACAO.md`; dados simulados |
| D12: simulador de planos no Swing | FEITO COM RESSALVA | `SimuladorPlanosPanel`, preços/módulos editáveis, `D12SimuladorUITest` (10 verificações headless); sem teste visual manual |
| D13: contratos, hash-chain, regras de vencimento | FEITO COM RESSALVA | `D13LedgerIntegrityTest`, `D13JdbcIntegrationTest`; ledger/API simulados |
| D14: bloqueio, MFA e trilha de segurança | FEITO COM RESSALVA | `D14SegurancaTest`, `D14JdbcIntegrationTest`, `D15AuthHttpJdbcTest`; MFA demonstrativo |
| D14: PII AES-GCM/HMAC e migration 016 | FEITO | `CoreRegression` e migrations aplicadas |
| D14: resposta ao incidente | FEITO COM RESSALVA | `docs/D14_RELATORIO_INCIDENTE_SIMULADO.md`; cenário hipotético |
| D15: conteúdo e portal integrado com JDBC | FEITO | `D15PortalTest`, `D15PersistenciaTest`, `D15AuthHttpJdbcTest` passaram |
| D15: métricas ADMIN 401/403/200 | FEITO | teste HTTP/JDBC passou |
| D15: relatório de tickets e pesquisa simulada | FEITO COM RESSALVA | `docs/D15_ANALISE_TICKETS_PESQUISA.md`; exemplos fictícios e não dados coletados |
| D15: redução real de chamados | NÃO COMPROVADO | inexistência de série histórica real |
| Instalação e migrations 011–016 repetíveis | FEITO | banco descartável `autobotz_d13_test`, duas execuções sem erro |
| Compilação Java 21 com todos os testes | FEITO | `javac -Xlint:all` passou |
| 11 testes offline no ZIP final | FEITO | execução no CachyOS em 29/10/2026, commit `fa580cde` |
| 8 testes JDBC no fechamento anterior | FEITO | MariaDB 13.0.2; CI da PR #39 também aprovado após a simulação D12 |
| CI integrado e segurança | FEITO | integração anterior e pipeline da PR #39 concluídos com sucesso |
| Integração das contribuições Git | FEITO | PR #37 / merge `f56c8dd` |
| PRs antigas | FEITO | zero abertas após reconciliação (fechamentos registram motivo) |
| Execução visual do Swing | NÃO COMPROVADO | código compila; demonstração manual necessária |
| Vídeo de 30 minutos | NÃO FEITO | gravação é responsabilidade do grupo |
| ZIP final limpo e extraível | FEITO | pacote do commit `fa580cde` extraído e recompilado no CachyOS; 11/11 suítes offline aprovadas |

**Dados de exemplo e simulação:** não apresentá-los como evidência de clientes, pagamentos, pesquisas ou métricas reais. **Arquivos finais:** `RELATORIO_GERAL_RODADA2.md`, `ROTEIRO_VIDEO.md`, `CHECKLIST_FINAL.md`, relatórios D12/D15 e ZIP do projeto.
