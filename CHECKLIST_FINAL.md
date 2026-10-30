# Checklist final — AutoBotz Motors (Rodada 2, 29/10/2026)

**Critério:** FEITO = implementação e verificação no escopo disponível; COM RESSALVA = funcionalidade simulada ou sem demonstração externa; NÃO COMPROVADO = ausência de evidência suficiente.

| Item | Estado | Evidência |
|---|---|---|
| D6–D10: regressão principal | FEITO | `CoreRegression` JDBC passou |
| D11: compras/fornecedores, orçamento, aprovação/rollback | FEITO | `D11ComprasTest`, `D11ComprasJdbcTest` passaram |
| D11: permissão e testes automatizados | FEITO — CI APROVADO | teste de formulário `D11ComprasSwingTest` adicionado ao pipeline, além da regressão JDBC |
| D11: comparação de eficiência | FEITO COM RESSALVA | modelo matemático simulado 120 min vs 5 min no relatório D11; sem medição real equivalente |
| D11: sustentabilidade | FEITO COM RESSALVA | cálculo simulado sob 60 W; consumo real não medido |
| D12: assinaturas, cancelamento, indicadores, cenários | FEITO COM RESSALVA | `AssinaturaServiceTest`, `AssinaturaDAOJdbcTest` e `entrega/D12_PLANOS_SIMULACAO.md`; dados simulados |
| D12: simulador de planos no Swing | FEITO COM RESSALVA | `SimuladorPlanosPanel`, preços/módulos editáveis, `D12SimuladorUITest` (10 verificações headless); sem teste visual manual |
| D13: contratos, hash-chain, regras de vencimento | FEITO COM RESSALVA | `D13LedgerIntegrityTest`, `D13JdbcIntegrationTest`; ledger/API simulados |
| D14: bloqueio, MFA e trilha de segurança | FEITO COM RESSALVA | `D14SegurancaTest`, `D14JdbcIntegrationTest`, `D15AuthHttpJdbcTest`; MFA demonstrativo |
| D14: PII AES-GCM/HMAC e migration 016 | FEITO | `CoreRegression` e migrations aplicadas |
| D14: resposta ao incidente | FEITO COM RESSALVA | `entrega/D14_RELATORIO_INCIDENTE_SIMULADO.md`; cenário hipotético |
| D15: conteúdo e portal integrado com JDBC | FEITO COM ACEITE CI PENDENTE | adicionados dashboard visual ADMIN, aviso Swing e roteiro de tutorial curto; testes HTTP/JDBC existentes |
| D15: métricas ADMIN 401/403/200 | FEITO | teste HTTP/JDBC passou |
| D15: relatório de tickets e pesquisa simulada | FEITO COM RESSALVA | `entrega/D15_ANALISE_TICKETS_PESQUISA.md`; exemplos fictícios e não dados coletados |
| D15: redução real de chamados | NÃO COMPROVADO | inexistência de série histórica real |
| Instalação e migrations 011–016 repetíveis | FEITO | banco descartável `autobotz_d13_test`, duas execuções sem erro |
| Compilação Java 21 com todos os testes | FEITO | `javac -Xlint:all` passou |
| 11 testes offline no ZIP final | FEITO | execução no CachyOS em 29/10/2026, commit `fa580cde` |
| 8 testes JDBC no fechamento anterior | FEITO | MariaDB 13.0.2; CI da PR #39 também aprovado após a simulação D12 |
| CI integrado e segurança | FEITO | integração anterior e pipeline da PR #39 concluídos com sucesso |
| Integração das contribuições Git | FEITO | PR #37 / merge `f56c8dd` |
| PRs antigas | FEITO | zero abertas após reconciliação (fechamentos registram motivo) |
| Execução visual do Swing | NÃO COMPROVADO | código compila; demonstração manual necessária |
| Vídeo de 30 minutos | NÃO FEITO | Arthur grava individualmente, somente rosto e fala; prazo acadêmico 30/10/2026 |
| ZIP final limpo e extraível | REGERAR NO COMPUTADOR | ZIP anteriormente verificado pertence a `fa580cde`; publicar pacote da `main` atual após este aceite |

| D15: pop-up e dashboard de métricas | FEITO — CI APROVADO | interface HTML/JS e aviso Swing adicionados; validar visualmente antes da gravação |
| D15: lançamento externo / webinar / fórum | NÃO REALIZADO | campanha preparada, mas sem envio real ou canais externos publicados; cenários didáticos indicados |
| Envio do ZIP e vídeo ao professor | NÃO CONFIRMADO | comprovar submissão na plataforma indicada hoje, 30/10/2026 |\n\n## Estado do aceite em 30/10/2026

O merge da PR #41 foi incorporado à `main` (`94a8f462`). O workflow `Integrated ERP Regression` terminou com sucesso para a versão da PR, incluindo validação do portal HTML/JS, compilação, testes determinísticos, teste funcional Swing do D11 e as suítes de integração JDBC D11–D15. A [matriz de aceite do professor](entrega/MATRIZ_ACEITE_PROFESSOR.md) apresenta cada produto final exigido e a localização da evidência.

**Fora da execução automática do GitHub:** gravar e enviar o vídeo de cerca de 30 minutos hoje; apresentar o PDF do relatório se exigido pela plataforma, anexar o ZIP gerado do último commit e guardar comprovante da entrega. As métricas reais de tempo/energia, os chamados reais, o webinar e o fórum operacional não foram produzidos. Os modelos permitidos pelo desafio foram identificados como `SIMULADO`. O artigo final consta para 11/12/2026, não para o marco de hoje.

**Dados de exemplo e simulação:** não apresentá-los como evidência de clientes, pagamentos, pesquisas ou métricas reais. **Arquivos finais:** `RELATORIO_GERAL_RODADA2.md`, `ROTEIRO_VIDEO.md`, `CHECKLIST_FINAL.md`, relatórios D12/D15 e ZIP do projeto.
