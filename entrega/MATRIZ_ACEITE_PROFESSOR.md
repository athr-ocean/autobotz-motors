# Matriz de aceite — enunciado do professor, D11 a D15

**Referência:** *Adm2026-2 — Desafios da 2ª Rodada*, Grupo 3, Equipe B, período de 02/10 a 30/10/2026. O enunciado solicita vídeo de aproximadamente 30 minutos em 30/10/2026 e artigo apenas no marco de 11/12/2026.

Critério: **IMPLEMENTADO** é recurso ou documento que existe; **SIMULAÇÃO** é entrega acadêmica com premissas fictícias declaradas; **EXTERNO** depende de gravação, medição humana ou envio fora do repositório. A existência do código não substitui teste visual, avaliação do professor ou protocolo de envio.

| Desafio | Produto final exigido | Material presente | Estado / ressalva |
| --- | --- | --- | --- |
| D11 | Análise e planejamento de automação | `entrega/D11_COMPRAS_RELATORIO.md`; `entrega/D11_QUALIDADE_SUSTENTABILIDADE.md` | IMPLEMENTADO |
| D11 | Casos de teste automatizados (API e interface funcional) | `tests/D11ComprasTest.java`, `tests/D11ComprasJdbcTest.java`, `tests/D11ComprasSwingTest.java`, CI | IMPLEMENTADO; testar no CI antes de declarar aprovado |
| D11 | Relatório de comparação de eficiência | `entrega/D11_QUALIDADE_SUSTENTABILIDADE.md` | SIMULAÇÃO: 120 vs 5 min; medição real NÃO COMPROVADA |
| D11 | Proposta de valor e sustentabilidade | `entrega/D11_QUALIDADE_SUSTENTABILIDADE.md` | SIMULAÇÃO de energia com potência assumida; não observação de consumo |
| D12 | Análise da crise, churn e cancelamentos | `entrega/D12_PLANOS_SIMULACAO.md` | SIMULAÇÃO explicitamente fictícia |
| D12 | Novos planos e módulos documentados | `AssinaturaService`, DAO, migration 012, `SimuladorPlanosPanel` | IMPLEMENTADO em protótipo |
| D12 | Simulação financeira e viabilidade | `entrega/D12_PLANOS_SIMULACAO.md`, `D12SimuladorUITest` | SIMULAÇÃO de MRR; viabilidade completa requer custos, impostos e retenção |
| D12 | Comunicação de lançamento e suporte | seção de e-mail e guia de suporte em `entrega/D12_PLANOS_SIMULACAO.md` | Documento pronto; não houve envio real |
| D13 | Relatório blockchain com processos/modelos | `entrega/D13_MAPA_E_MODELAGEM.md`, `entrega/D13_CONTRATOS_LEDGER.md` | IMPLEMENTADO como ledger local |
| D13 | Apresentação da prova de conceito | `entrega/D13_ROTEIRO_POC.md`, `ApiBlockchainSimulada` | SIMULAÇÃO demonstrável; gravação real NÃO COMPROVADA |
| D13 | Plano de testes e integridade | `D13LedgerIntegrityTest`, `D13BlockchainApiTest`, `D13JdbcIntegrationTest` | IMPLEMENTADO/testes anteriores passaram |
| D13 | Plano de treinamento dos usuários | `entrega/D13_GUIA_PRIMEIROS_PASSOS.md` | IMPLEMENTADO |
| D14 | Relatório forense do incidente simulado | `entrega/D14_RELATORIO_INCIDENTE_SIMULADO.md` | SIMULAÇÃO; IPs reais não disponíveis, não inventados |
| D14 | Plano e configuração de segurança | `entrega/D14_LOGIN_MFA.md`, `entrega/D14_CIFRAGEM_DADOS_PESSOAIS.md`; `014`/`016` | IMPLEMENTADO; MFA é didático, chave externa |
| D14 | Plano de resposta a incidentes | `entrega/D14_RELATORIO_INCIDENTE_SIMULADO.md` | IMPLEMENTADO como plano |
| D14 | Comunicação de crise | comunicado-modelo em `entrega/D14_RELATORIO_INCIDENTE_SIMULADO.md` | Documento simulado; não enviado |
| D15 | Relatório de tickets e pesquisa | `entrega/D15_ANALISE_TICKETS_PESQUISA.md` | SIMULAÇÃO, dados explicitamente inventados |
| D15 | Portal, guias e conteúdo | `portal-treinamento/`, `entrega/D15_GUIA_RAPIDO.md`, FAQ e roteiro tutorial | IMPLEMENTADO como serviço local; vídeo de 3 min não gravado |
| D15 | Campanha de lançamento | `entrega/D15_LANCAMENTO_E_MATERIAIS.md`, aviso pop-up no Swing | Documento e aviso implementados; e-mail real não enviado |
| D15 | Dashboard e monitoramento | painel do portal e `GET /api/treinamentos/metricas`, autenticação ADMIN | IMPLEMENTADO; análise de redução real de chamados depende de base real |
| Integração | Construção contínua dos desafios | `main`, migrations 011–016, testes, CI, README | IMPLEM. / aceite CI final pendente |
| Apresentação | Vídeo de 30 minutos da trajetória | `ROTEIRO_VIDEO.md` | EXTERNO: Arthur precisa gravar, conferir duração e submeter hoje |
| Submissão | Documentos e código conforme orientação professor | `RELATORIO_GERAL_RODADA2.md`, `CHECKLIST_FINAL.md`, ZIP gerado por `git archive` | EXTERNO: gerar ZIP do **último commit**, anexar e obter comprovante |
| Marco futuro | Artigo final previsto para 11/12/2026 | documentação atual pode servir de base | NÃO EXIGÍVEL no marco de 30/10/2026; etapa futura |

## Conclusão de aceite

O escopo **implementável em código e documentação** está organizado e vinculado às suas evidências. Ainda **não há como declarar todos os requisitos concluídos**: o vídeo de 30 minutos e o envio dependem de Arthur; uma comparação medida de tempo/energia e uma redução real de tickets requerem observações que não foram fornecidas. O enunciado autoriza modelos simulados para energia, crise, incidente e pesquisa. Apresentá-los como reais seria inadequado.

O aceite final deve registrar o hash do commit, a conclusão do CI, a inspeção visual local do Swing/portal e o comprovante da entrega à plataforma acadêmica.
