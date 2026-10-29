# Auditoria de organização e limpeza — AutoBotz Motors

**Referência:** código publicado no GitHub em 29/10/2026, após a integração D11–D15 (#37). **Escopo:** 249 arquivos versionados na base da revisão, examinados por caminho e categoria; busca de comentários/artefatos, revisão dirigida e reexecução dos testes. Este inventário não é uma declaração de revisão humana individual de cada linha.

## Critério da limpeza

Removemos somente itens cuja função não era necessária à compilação ou aos requisitos atuais:
- 35 arquivos compilados e recursos duplicados em `bin/`;
- 21 arquivos de uma cópia antiga e paralela do projeto em `autobotz-motors-main/`;
- 3 arquivos TXT de dados locais/legados, potencialmente inadequados para publicação;
- 1 roteiro de apresentação antigo com afirmações já desatualizadas.

**Total:** 60 arquivos removidos do estado atual do Git. A remoção não apaga o histórico do Git. Se algum TXT contiver informações pessoais reais, os responsáveis precisam avaliar a exposição histórica separadamente, sem reescrever commits por conta própria.

Também foram eliminados apenas dois cabeçalhos decorativos redundantes de `src/autobotz/Main.java`, sem alterar lógica, nomes públicos, modelos ou testes. A configuração de exclusões do Git foi ampliada para evitar novas inclusões acidentais de dados locais, logs e arquivos de backup. O README foi ajustado para refletir a remoção.

**Comentários mantidos intencionalmente:** contratos de segurança, semântica do AES-GCM/HMAC, ownership de conexões JDBC, concorrência e rollback, descrição de mocks acadêmicos e pré-condições de bancos `_test`. Remover essas explicações comprometeria manutenção ou entendimento técnico.

## Validação após a limpeza

- Compilação de fontes e testes em Java 21 com `javac -Xlint:all`: aprovada, sem warnings.
- Dez suítes offline: `AssinaturaServiceTest`, `ConexaoBancoDataSourceTest`, `D11ComprasPermissaoTest`, `D13BlockchainApiTest`, `D13LedgerIntegrityTest`, `D14SegurancaTest`, `D14SessaoWebTest`, `D15AtalhoTest`, `D15ConteudoTest`, `D15PortalTest`: 10/10 aprovadas na cópia de ensaio.
- `git diff --check`: aprovado.
- A suite JDBC de oito testes foi aprovada pelo responsável antes da limpeza, no commit `718fe28`. Esta revisão removeu arquivos legados e dois cabeçalhos, sem alterar lógica Java nem migrations; os testes JDBC não foram reexecutados *após* essa mudança no ambiente de ensaio.

## Inventário de arquivos rastreados antes da limpeza

| Caminho versionado | Ação/justificativa |
|---|---|
| `.classpath` | preservado: configuração de projeto |
| `.github/workflows/d12-regression.yml` | preservado: workflow |
| `.github/workflows/d14-security-regression.yml` | preservado: workflow |
| `.github/workflows/integration-regression.yml` | preservado: workflow |
| `.gitignore` | preservado: configuração de projeto |
| `.project` | preservado: configuração de projeto |
| `.vscode/settings.json` | preservado: configuração de projeto |
| `CHECKLIST_FINAL.md` | preservado: documentação pertinente |
| `GUIA_GIT_ARTHUR.md` | preservado: documentação pertinente |
| `README.md` | preservado: documentação pertinente |
| `README_D15_INTEGRACAO.md` | preservado: documentação pertinente |
| `RELATORIO_GERAL_RODADA2.md` | preservado: documentação pertinente |
| `autobotz-motors-main/.classpath` | removido: duplicata legada |
| `autobotz-motors-main/.gitignore` | removido: duplicata legada |
| `autobotz-motors-main/.project` | removido: duplicata legada |
| `autobotz-motors-main/README.md` | removido: duplicata legada |
| `autobotz-motors-main/clientes.txt` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/Cliente.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/ClienteDAO.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/ItemOS.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/Main.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/OrdemServico.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/OrdemServicoDAO.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/Servico.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/ServicoDAO.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/TesteOficina.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/Veiculo.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/VeiculoDAO.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/Venda.java` | removido: duplicata legada |
| `autobotz-motors-main/src/autobotz/VendaDAO.java` | removido: duplicata legada |
| `autobotz-motors-main/src/conexao/Conexao.java` | removido: duplicata legada |
| `autobotz-motors-main/src/messages_en_US.properties` | removido: duplicata legada |
| `autobotz-motors-main/src/messages_pt_BR.properties` | removido: duplicata legada |
| `bin/autobotz/AuthService.class` | removido: bytecode recompilável |
| `bin/autobotz/AutorizacaoService$Permissao.class` | removido: bytecode recompilável |
| `bin/autobotz/AutorizacaoService.class` | removido: bytecode recompilável |
| `bin/autobotz/LoginMenu.class` | removido: bytecode recompilável |
| `bin/autobotz/Main.class` | removido: bytecode recompilável |
| `bin/autobotz/PerfilUsuario.class` | removido: bytecode recompilável |
| `bin/autobotz/SessaoUsuario.class` | removido: bytecode recompilável |
| `bin/autobotz/Usuario.class` | removido: bytecode recompilável |
| `bin/autobotz/UsuarioDAO.class` | removido: bytecode recompilável |
| `bin/autobotz/dao/AuditoriaDAO.class` | removido: bytecode recompilável |
| `bin/autobotz/dao/ClienteDAO.class` | removido: bytecode recompilável |
| `bin/autobotz/dao/OrdemServicoDAO.class` | removido: bytecode recompilável |
| `bin/autobotz/dao/ServicoDAO.class` | removido: bytecode recompilável |
| `bin/autobotz/dao/VeiculoDAO.class` | removido: bytecode recompilável |
| `bin/autobotz/dao/VendaDAO.class` | removido: bytecode recompilável |
| `bin/autobotz/model/Cliente.class` | removido: bytecode recompilável |
| `bin/autobotz/model/ItemOS.class` | removido: bytecode recompilável |
| `bin/autobotz/model/LogAuditoria.class` | removido: bytecode recompilável |
| `bin/autobotz/model/OrdemServico.class` | removido: bytecode recompilável |
| `bin/autobotz/model/Servico.class` | removido: bytecode recompilável |
| `bin/autobotz/model/Veiculo.class` | removido: bytecode recompilável |
| `bin/autobotz/model/Venda.class` | removido: bytecode recompilável |
| `bin/autobotz/service/ClienteService.class` | removido: bytecode recompilável |
| `bin/autobotz/service/RelatorioService.class` | removido: bytecode recompilável |
| `bin/autobotz/service/ResultadoOS.class` | removido: bytecode recompilável |
| `bin/autobotz/service/ServicoService.class` | removido: bytecode recompilável |
| `bin/autobotz/service/VendaService.class` | removido: bytecode recompilável |
| `bin/autobotz/ui/ClienteMenu.class` | removido: bytecode recompilável |
| `bin/autobotz/ui/RelatorioMenu.class` | removido: bytecode recompilável |
| `bin/autobotz/ui/ServicoMenu.class` | removido: bytecode recompilável |
| `bin/autobotz/ui/VeiculoMenu.class` | removido: bytecode recompilável |
| `bin/autobotz/util/ConexaoBanco.class` | removido: bytecode recompilável |
| `bin/autobotz/util/I18nUtils.class` | removido: bytecode recompilável |
| `bin/messages_en_US.properties` | removido: bytecode recompilável |
| `bin/messages_pt_BR.properties` | removido: bytecode recompilável |
| `clientes.txt` | removido: dados locais não necessários |
| `db/migrations/011_d11_compras.sql` | preservado: esquema/migration |
| `db/migrations/012_d12_assinaturas.sql` | preservado: esquema/migration |
| `db/migrations/013_d13_contratos_ledger.sql` | preservado: esquema/migration |
| `db/migrations/014_d14_security.sql` | preservado: esquema/migration |
| `db/migrations/015_d15_treinamento.sql` | preservado: esquema/migration |
| `db/migrations/016_d14_client_pii.sql` | preservado: esquema/migration |
| `db/migrations/README.md` | preservado: esquema/migration |
| `docs/AUTORIA_INTEGRACAO.md` | preservado: documentação pertinente |
| `docs/D11_COMPRAS_RELATORIO.md` | preservado: documentação pertinente |
| `docs/D11_QUALIDADE_SUSTENTABILIDADE.md` | preservado: documentação pertinente |
| `docs/D13_CONTRATOS_LEDGER.md` | preservado: documentação pertinente |
| `docs/D13_GUIA_PRIMEIROS_PASSOS.md` | preservado: documentação pertinente |
| `docs/D13_MAPA_E_MODELAGEM.md` | preservado: documentação pertinente |
| `docs/D13_ROTEIRO_POC.md` | preservado: documentação pertinente |
| `docs/D14_CIFRAGEM_DADOS_PESSOAIS.md` | preservado: documentação pertinente |
| `docs/D14_LOGIN_MFA.md` | preservado: documentação pertinente |
| `docs/D14_RELATORIO_INCIDENTE_SIMULADO.md` | preservado: documentação pertinente |
| `docs/D14_REVISAO_MIGRACAO_016.md` | preservado: documentação pertinente |
| `docs/D14_SESSAO_WEB.md` | preservado: documentação pertinente |
| `docs/D15_CONTRATO_API.md` | preservado: documentação pertinente |
| `docs/D15_FAQ.md` | preservado: documentação pertinente |
| `docs/D15_FLUXO_GITHUB.md` | preservado: documentação pertinente |
| `docs/D15_GUIA_RAPIDO.md` | preservado: documentação pertinente |
| `docs/D15_INTEGRACAO_ARTHUR.md` | preservado: documentação pertinente |
| `docs/D15_INTEGRACAO_ERP_ARTHUR.md` | preservado: documentação pertinente |
| `docs/D15_LANCAMENTO_E_MATERIAIS.md` | preservado: documentação pertinente |
| `docs/D15_MATERIAIS_TRANSICAO.md` | preservado: documentação pertinente |
| `docs/D15_METRICAS.md` | preservado: documentação pertinente |
| `docs/D15_PORTAL.md` | preservado: documentação pertinente |
| `docs/D15_ROTEIRO_VIDEO.md` | preservado: documentação pertinente |
| `docs/D15_ROTEIRO_VIDEO_30_MIN.md` | preservado: documentação pertinente |
| `docs/D15_STATUS_E_DECISOES.md` | preservado: documentação pertinente |
| `docs/MIGRACAO_D9.md` | preservado: documentação pertinente |
| `docs/ROTEIRO_VIDEO_RODADA2.md` | removido: roteiro histórico desatualizado |
| `docs/VALIDACAO.md` | preservado: documentação pertinente |
| `lib/mariadb-java-client-4.0.0.jar` | preservado: driver JDBC |
| `portal-treinamento/content/catalogo.json` | preservado: portal web |
| `portal-treinamento/content/faq.md` | preservado: documentação pertinente |
| `portal-treinamento/content/guia-rapido.md` | preservado: documentação pertinente |
| `portal-treinamento/content/roteiro-video-30-min.md` | preservado: documentação pertinente |
| `portal-treinamento/public/app.js` | preservado: portal web |
| `portal-treinamento/public/index.html` | preservado: portal web |
| `portal-treinamento/public/style.css` | preservado: portal web |
| `schema.sql` | preservado: esquema/migration |
| `scripts/run.sh` | preservado: launcher/testes |
| `scripts/test-d11-compras.sh` | preservado: launcher/testes |
| `scripts/test-d15-auth-jdbc.sh` | preservado: launcher/testes |
| `scripts/test-d15.sh` | preservado: launcher/testes |
| `scripts/test.sh` | preservado: launcher/testes |
| `src/.classpath` | preservado: código fonte ativo |
| `src/.project` | preservado: código fonte ativo |
| `src/autobotz/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/AuthService.java` | preservado: código fonte ativo |
| `src/autobotz/AutorizacaoService.java` | preservado: código fonte ativo |
| `src/autobotz/LoginMenu.java` | preservado: código fonte ativo |
| `src/autobotz/Main.java` | preservado: código fonte ativo |
| `src/autobotz/PerfilUsuario.java` | preservado: código fonte ativo |
| `src/autobotz/ProjetoDAO.java` | preservado: código fonte ativo |
| `src/autobotz/SessaoUsuario.java` | preservado: código fonte ativo |
| `src/autobotz/Usuario.java` | preservado: código fonte ativo |
| `src/autobotz/UsuarioDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/dao/AssinaturaDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/AuditoriaDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/ClienteDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/CompraDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/ContratoDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/EventoTreinamentoDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/FornecedorDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/LedgerContratoDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/OrdemServicoDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/SegurancaDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/SegurancaRepositorio.java` | preservado: código fonte ativo |
| `src/autobotz/dao/ServicoDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/VeiculoDAO.java` | preservado: código fonte ativo |
| `src/autobotz/dao/VendaDAO.java` | preservado: código fonte ativo |
| `src/autobotz/integracao/ApiBlockchainSimulada.java` | preservado: código fonte ativo |
| `src/autobotz/model/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/model/AssinaturaCliente.java` | preservado: código fonte ativo |
| `src/autobotz/model/Cliente.java` | preservado: código fonte ativo |
| `src/autobotz/model/ContratoFornecedor.java` | preservado: código fonte ativo |
| `src/autobotz/model/DesafioMfa.java` | preservado: código fonte ativo |
| `src/autobotz/model/EventoSeguranca.java` | preservado: código fonte ativo |
| `src/autobotz/model/EventoTreinamento.java` | preservado: código fonte ativo |
| `src/autobotz/model/Fornecedor.java` | preservado: código fonte ativo |
| `src/autobotz/model/ItemOS.java` | preservado: código fonte ativo |
| `src/autobotz/model/LogAuditoria.java` | preservado: código fonte ativo |
| `src/autobotz/model/MetricaTreinamento.java` | preservado: código fonte ativo |
| `src/autobotz/model/ModuloOpcional.java` | preservado: código fonte ativo |
| `src/autobotz/model/OrcamentoCompra.java` | preservado: código fonte ativo |
| `src/autobotz/model/OrdemServico.java` | preservado: código fonte ativo |
| `src/autobotz/model/PedidoCompra.java` | preservado: código fonte ativo |
| `src/autobotz/model/PlanoAssinatura.java` | preservado: código fonte ativo |
| `src/autobotz/model/RegistroLedgerContrato.java` | preservado: código fonte ativo |
| `src/autobotz/model/ResultadoIntegridadeLedger.java` | preservado: código fonte ativo |
| `src/autobotz/model/ResultadoSimulacao.java` | preservado: código fonte ativo |
| `src/autobotz/model/Servico.java` | preservado: código fonte ativo |
| `src/autobotz/model/SolicitacaoCompra.java` | preservado: código fonte ativo |
| `src/autobotz/model/StatusContrato.java` | preservado: código fonte ativo |
| `src/autobotz/model/StatusSolicitacao.java` | preservado: código fonte ativo |
| `src/autobotz/model/TipoEventoContrato.java` | preservado: código fonte ativo |
| `src/autobotz/model/Veiculo.java` | preservado: código fonte ativo |
| `src/autobotz/model/Venda.java` | preservado: código fonte ativo |
| `src/autobotz/portal/CatalogoMateriais.java` | preservado: código fonte ativo |
| `src/autobotz/portal/MockPortalBackend.java` | preservado: código fonte ativo |
| `src/autobotz/portal/PortalAtalho.java` | preservado: código fonte ativo |
| `src/autobotz/portal/PortalBackend.java` | preservado: código fonte ativo |
| `src/autobotz/portal/PortalLauncher.java` | preservado: código fonte ativo |
| `src/autobotz/portal/PortalServer.java` | preservado: código fonte ativo |
| `src/autobotz/portal/TreinamentoPortalBackend.java` | preservado: código fonte ativo |
| `src/autobotz/service/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/service/AssinaturaService.java` | preservado: código fonte ativo |
| `src/autobotz/service/CRMService.java` | preservado: código fonte ativo |
| `src/autobotz/service/ClienteService.java` | preservado: código fonte ativo |
| `src/autobotz/service/ComprasException.java` | preservado: código fonte ativo |
| `src/autobotz/service/ComprasService.java` | preservado: código fonte ativo |
| `src/autobotz/service/ContratoException.java` | preservado: código fonte ativo |
| `src/autobotz/service/ContratoService.java` | preservado: código fonte ativo |
| `src/autobotz/service/MfaService.java` | preservado: código fonte ativo |
| `src/autobotz/service/RelatorioService.java` | preservado: código fonte ativo |
| `src/autobotz/service/ResultadoLogin.java` | preservado: código fonte ativo |
| `src/autobotz/service/ResultadoOS.java` | preservado: código fonte ativo |
| `src/autobotz/service/SegurancaService.java` | preservado: código fonte ativo |
| `src/autobotz/service/ServicoService.java` | preservado: código fonte ativo |
| `src/autobotz/service/SessaoWebService.java` | preservado: código fonte ativo |
| `src/autobotz/service/TreinamentoService.java` | preservado: código fonte ativo |
| `src/autobotz/service/VendaService.java` | preservado: código fonte ativo |
| `src/autobotz/ui/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/ui/ClienteMenu.java` | preservado: código fonte ativo |
| `src/autobotz/ui/RelatorioMenu.java` | preservado: código fonte ativo |
| `src/autobotz/ui/ServicoMenu.java` | preservado: código fonte ativo |
| `src/autobotz/ui/VeiculoMenu.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/AppFrame.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/ComprasPanel.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/LanguageFrame.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/LoginFrame.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/SwingMain.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/SwingSupport.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/components/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/components/AppButton.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/components/AppComboBox.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/components/AppDialog.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/components/AppPasswordField.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/components/AppTable.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/components/AppTextField.java` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/theme/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/ui/swing/theme/AppTheme.java` | preservado: código fonte ativo |
| `src/autobotz/util/.gitignore` | preservado: código fonte ativo |
| `src/autobotz/util/ConexaoBanco.java` | preservado: código fonte ativo |
| `src/autobotz/util/ConexaoBancoDataSource.java` | preservado: código fonte ativo |
| `src/autobotz/util/CriptografiaDadosPessoais.java` | preservado: código fonte ativo |
| `src/autobotz/util/I18nUtils.java` | preservado: código fonte ativo |
| `src/autobotz/util/LedgerContratoHash.java` | preservado: código fonte ativo |
| `src/autobotz/util/MigracaoDadosPessoais.java` | preservado: código fonte ativo |
| `src/autobotz/util/MigracaoMembrosProjeto.java` | preservado: código fonte ativo |
| `src/autobotz/util/SenhaHash.java` | preservado: código fonte ativo |
| `src/messages_en_US.properties` | preservado: código fonte ativo |
| `src/messages_pt_BR.properties` | preservado: código fonte ativo |
| `tests/AssinaturaDAOJdbcTest.java` | preservado: teste |
| `tests/AssinaturaServiceTest.java` | preservado: teste |
| `tests/ConexaoBancoDataSourceTest.java` | preservado: teste |
| `tests/CoreRegression.java` | preservado: teste |
| `tests/D11ComprasJdbcTest.java` | preservado: teste |
| `tests/D11ComprasPermissaoTest.java` | preservado: teste |
| `tests/D11ComprasTest.java` | preservado: teste |
| `tests/D13BlockchainApiTest.java` | preservado: teste |
| `tests/D13JdbcIntegrationTest.java` | preservado: teste |
| `tests/D13LedgerIntegrityTest.java` | preservado: teste |
| `tests/D14JdbcIntegrationTest.java` | preservado: teste |
| `tests/D14SegurancaTest.java` | preservado: teste |
| `tests/D14SessaoWebTest.java` | preservado: teste |
| `tests/D15AtalhoTest.java` | preservado: teste |
| `tests/D15AuthHttpJdbcTest.java` | preservado: teste |
| `tests/D15ConteudoTest.java` | preservado: teste |
| `tests/D15PersistenciaTest.java` | preservado: teste |
| `tests/D15PortalTest.java` | preservado: teste |
| `tests/D15_CHECKLIST_ACEITACAO.md` | preservado: teste |
| `veiculos.txt` | removido: dados locais não necessários |
| `vendas.txt` | removido: dados locais não necessários |

**Autoria:** o histórico Git dos integrantes foi preservado; os ajustes de limpeza não são atribuídos a outro colega. Este documento registra o que foi alterado e testado, sem qualquer alegação de autoria exclusivamente humana.
