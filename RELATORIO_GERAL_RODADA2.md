# AutoBotz Motors — Relatório geral da segunda rodada (D11–D15)

**Disciplina:** Administração para Computação 2026.2 · **Equipe B — Liga do Mal** · **Data de consolidação:** 29/10/2026.

## 1. Síntese dos requisitos acadêmicos em dez pontos

1. O Grupo de Desafios 3 compreende D11 a D15.
2. D11 abrange sustentabilidade, compras, qualidade, testes e análise da eficiência.
3. D12 aborda crise econômica, planos de assinatura, churn, receita e simulações.
4. D13 aborda inovação, fornecedores, contratos e prova de conceito de integridade.
5. D14 cobre proteção de dados, login, bloqueio, MFA e resposta a incidente simulado.
6. D15 trata de treinamento, conteúdos, acesso, métricas e suporte.
7. O material acadêmico deve explicar o sistema, metodologia, resultados e limitações.
8. As simulações devem ser distinguidas de dados de clientes e operações reais.
9. O cronograma fornecido prevê vídeo de aproximadamente 30 minutos em 30/10/2026.
10. O cronograma também prevê artigo final e outro marco em 11/12/2026. Nos PDFs acadêmicos anteriormente analisados não foi identificada regra específica de proibição de IA; eventuais instruções adicionais do professor prevalecem.

**Fontes acadêmicas anteriormente fornecidas:** cronograma da disciplina; PDF da segunda rodada de desafios; PDF de divisão da Equipe B. A divisão nominal abaixo foi reunida da documentação interna do projeto e deve ser conferida com a equipe.

## 2. Projeto e autoria

O AutoBotz Motors é um ERP acadêmico em Java 21 com interface Swing, modo console, MariaDB/JDBC e portal de treinamento HTTP local. A base D6–D10 inclui clientes, veículos, oficina, vendas, CRM, autenticação, projetos, relatórios e internacionalização.

| Desafio | Integrantes por frente interna | Implementação consolidada |
|---|---|---|
| D11 | Alexandre (compras), Adryan (testes/CI) | fornecedores, orçamentos, solicitações, aprovação e pedidos |
| D12 | André | planos, módulos, assinaturas, cancelamentos, MRR/churn e simulação |
| D13 | Gabriel | contratos, avisos, ledger local SHA-256 e API simulada |
| D14 | João (autenticação/MFA), Arthur (dados pessoais/integração) | bloqueio, MFA, sessões, eventos de segurança, AES-GCM e índice HMAC |
| D15 | Mauro (portal), Higor (conteúdo/métricas), Arthur (integração) | portal HTTP, catálogo, eventos JDBC, métricas e entrada Swing |

A autoria efetiva está nos commits de Git. A PR #37 preservou o histórico; documentos de consolidação não substituem a autoria dos autores originais.

## 3. Arquitetura e integração

- **Fontes:** `src/autobotz`; **testes:** `tests/`; **portal:** `portal-treinamento/`; **migrações:** `db/migrations/`.
- **Conexão:** `ConexaoBanco` disponibiliza conexões independentes via `getDataSource()`; serviços que obtêm conexões devem fechá-las.
- **Persistência:** `schema.sql` define instalação nova; atualizações usam migrações 011→012→013→014→015→016. A migration 013 depende da 011.
- **Segurança:** MFA demonstrativo, sessão, acesso ADMIN ao endpoint de métricas, proteção de PII AES-GCM/HMAC; nenhuma chave deve constar do repositório.
- **PoC D13:** os hashes provam integridade na aplicação; não constituem blockchain pública, pagamento real ou auditoria financeira externa.
- **Indicadores D12 e D15:** dados de churn, tickets, pesquisa e projeção financeira são simulações quando não há base de observações reais.
- **Sustentabilidade D11:** automação tem valor operacional, mas economia de energia, comparação temporal manual e ganho ambiental não foram medidos em condições comparáveis; não há comprovação quantitativa dessas reduções.

## 4. Evidências verificadas na máquina do responsável (29/10/2026)

**Versão testada:** `codex/integration-final-20261029`, commit `718fe28b2354b6fff8a3687b650dc65b6d17fcda`. **Sistema:** CachyOS, OpenJDK 21.0.12.1, MariaDB 13.0.2.

- `javac -encoding UTF-8 -Xlint:all` sobre todas as fontes e testes: **aprovado**.
- Testes sem banco (10/10): `AssinaturaServiceTest`, `ConexaoBancoDataSourceTest`, `D11ComprasPermissaoTest`, `D13BlockchainApiTest`, `D13LedgerIntegrityTest`, `D14SegurancaTest`, `D14SessaoWebTest`, `D15AtalhoTest`, `D15ConteudoTest`, `D15PortalTest`: **aprovados**.
- Banco descartável `autobotz_d13_test`: importação de `schema.sql`, aplicação das migrações 011–016 e reaplicação completa: **sem erros**.
- Testes JDBC (8/8): `CoreRegression`, `D11ComprasTest`, `D11ComprasJdbcTest`, `AssinaturaDAOJdbcTest`, `D13JdbcIntegrationTest`, `D14JdbcIntegrationTest`, `D15PersistenciaTest` e `D15AuthHttpJdbcTest`: **aprovados**.
- Log da execução JDBC mantido localmente em `~/Downloads/autobotz-jdbc-20261029.log` (não contém credenciais exportadas pelo script).
- Workflows GitHub Actions no commit `718fe28`: `Integrated ERP Regression` e `D14 security regression`: ambos **success**.
- PR #37 consolidada por merge normal na `main`: commit `f56c8dd098708d7628d4223367b4adc605080aea`. Após o merge e a reconciliação, **zero PRs abertas**.

As verificações acima não equivalem à certificação de produção. A interface Swing foi compilada, mas o seu fluxo visual não foi ensaiado nesta verificação assistida. Os testes 401/403/200 do portal foram executados pelo teste HTTP/JDBC; não há demonstração gravada do navegador.

## 5. Matriz resumida requisito → implementação → evidência

| Requisito | Implementação principal | Evidência e estado |
|---|---|---|
| D6–D10 sem regressões principais | DAOs e serviços preexistentes | `CoreRegression` passou no MariaDB de teste |
| D11 compra e orçamento | `ComprasService`, `ComprasPanel` | testes D11 serviço/JDBC passaram; cliques Swing não ensaiados |
| D11 testes, CI, sustentabilidade | scripts/workflows, relatório D11 | CI passou; medição energética e tempo manual pendentes |
| D12 planos, assinatura, transações | `AssinaturaDAO`, `AssinaturaService` | testes offline/JDBC passaram; projeções simuladas |
| D13 contratos, hash-chain e PoC | `ContratoService`, `LedgerContratoDAO` | testes offline/JDBC passaram; API e pagamento simulados |
| D14 autenticação e MFA | `AuthService`, `SegurancaDAO` | testes de segurança e HTTP/JDBC passaram; fator demonstrativo |
| D14 dados pessoais | AES-GCM, HMAC e migration 016 | `CoreRegression` e validação de schema passaram |
| D15 materiais, eventos e métricas | `PortalLauncher`, `TreinamentoService` | portal offline e HTTP/JDBC passaram; efeito em chamados não medido |
| Entrega final | `README.md`, documentos e código `main` | gerar e conferir ZIP a partir do merge final |

## 6. Limitações e entrega

- Não confundir mock, valores de exemplo, cronograma futuro ou métricas simuladas com resultados medidos.
- Não afirmar que foram enviados avisos reais, efetuados pagamentos ou gravados webinars/tutoriais não produzidos.
- O vídeo será gravado pela equipe; o roteiro em `ROTEIRO_VIDEO.md` serve como guia.
- A demonstração visual do Swing, medição temporal manual, telemetria energética e comparação real de chamados continuam sem evidência.
- O pacote de entrega deve ser montado a partir do commit final da `main`, sem `.git`, arquivos de chaves, segredos e resíduos de build. O procedimento seguro está em `GUIA_GIT_ARTHUR.md`.
