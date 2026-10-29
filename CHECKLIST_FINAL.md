# Checklist final — AutoBotz Motors (Rodada 2, 29/10/2026)

**Critério:** FEITO = implementação e verificação no escopo disponível; COM RESSALVA = funcionalidade simulada ou sem demonstração externa; NÃO COMPROVADO = ausência de evidência suficiente.

| Item | Estado | Evidência |
|---|---|---|
| D6–D10: regressão principal | FEITO | `CoreRegression` JDBC passou |
| D11: compras/fornecedores, orçamento, aprovação/rollback | FEITO | `D11ComprasTest`, `D11ComprasJdbcTest` passaram |
| D11: permissão e testes automatizados | FEITO | `D11ComprasPermissaoTest` e workflow integrado passaram |
| D11: medição de tempo manual x automatizado | NÃO COMPROVADO | sem medição manual comparável |
| D11: economia de energia comprovada | NÃO COMPROVADO | sem telemetria de consumo |
| D12: assinaturas, cancelamento, indicadores, cenários | FEITO COM RESSALVA | `AssinaturaServiceTest` e `AssinaturaDAOJdbcTest` passaram; dados simulados |
| D13: contratos, hash-chain, regras de vencimento | FEITO COM RESSALVA | `D13LedgerIntegrityTest`, `D13JdbcIntegrationTest`; ledger/API simulados |
| D14: bloqueio, MFA e trilha de segurança | FEITO COM RESSALVA | `D14SegurancaTest`, `D14JdbcIntegrationTest`, `D15AuthHttpJdbcTest`; MFA demonstrativo |
| D14: PII AES-GCM/HMAC e migration 016 | FEITO | `CoreRegression` e migrations aplicadas |
| D14: resposta ao incidente | FEITO COM RESSALVA | `docs/D14_RELATORIO_INCIDENTE_SIMULADO.md`; cenário hipotético |
| D15: conteúdo e portal integrado com JDBC | FEITO | `D15PortalTest`, `D15PersistenciaTest`, `D15AuthHttpJdbcTest` passaram |
| D15: métricas ADMIN 401/403/200 | FEITO | teste HTTP/JDBC passou |
| D15: redução real de chamados | NÃO COMPROVADO | inexistência de série histórica real |
| Instalação e migrations 011–016 repetíveis | FEITO | banco descartável `autobotz_d13_test`, duas execuções sem erro |
| Compilação Java 21 com todos os testes | FEITO | `javac -Xlint:all` passou |
| 10 testes offline e 8 JDBC | FEITO | execução em 29/10/2026, log JDBC local |
| CI integrado e segurança | FEITO | dois workflows GitHub success no commit `718fe28` |
| Integração das contribuições Git | FEITO | PR #37 / merge `f56c8dd` |
| PRs antigas | FEITO | zero abertas após reconciliação (fechamentos registram motivo) |
| Execução visual do Swing | NÃO COMPROVADO | código compila; demonstração manual necessária |
| Vídeo de 30 minutos | NÃO FEITO | gravação é responsabilidade do grupo |
| ZIP final limpo e extraível | A VALIDAR | gerar e verificar no CachyOS a partir da `main` atual |

**Dados de exemplo e simulação:** não apresentá-los como evidência de clientes, pagamentos, pesquisas ou métricas reais. **Arquivos finais:** `RELATORIO_GERAL_RODADA2.md`, `ROTEIRO_VIDEO.md`, `CHECKLIST_FINAL.md`, `GUIA_GIT_ARTHUR.md` e ZIP do projeto.
