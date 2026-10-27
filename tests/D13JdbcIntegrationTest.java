import autobotz.dao.FornecedorDAO;
import autobotz.model.ContratoFornecedor;
import autobotz.model.Fornecedor;
import autobotz.model.RegistroLedgerContrato;
import autobotz.model.StatusContrato;
import autobotz.model.TipoEventoContrato;
import autobotz.service.ContratoException;
import autobotz.service.ContratoService;
import autobotz.util.ConexaoBanco;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Integração D13. Só opera em autobotz_d13_test e remove exclusivamente os próprios dados. */
public final class D13JdbcIntegrationTest {
    private static int passed;
    private static final List<Long> contratosCriados = new ArrayList<>();
    private static int fornecedorId;

    private D13JdbcIntegrationTest() { }

    private static void check(boolean ok, String nome) {
        if (!ok) throw new AssertionError("FAIL " + nome);
        passed++;
        System.out.println("PASS " + nome);
    }

    private static int contar(String sql, Object... parametros) throws Exception {
        try (PreparedStatement ps = ConexaoBanco.getConexao().prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public static void main(String[] args) throws Exception {
        String url = System.getenv("AUTOBOTZ_DB_URL");
        if (url == null || !url.matches("(?i).*autobotz_d13_test(?:[?].*)?")) {
            System.err.println("RECUSADO: AUTOBOTZ_DB_URL deve apontar para autobotz_d13_test.");
            System.exit(2);
        }
        check(ConexaoBanco.getConexao().isValid(2), "JDBC_CONECTA");
        check(contar("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME IN ('fornecedores','contratos_fornecedor','ledger_contratos')") == 3,
                "TABELAS_D11_D13_EXISTEM");

        String sufixo = Long.toString(System.currentTimeMillis(), 36);
        try {
            try (Connection c = ConexaoBanco.getConexao()) {
                boolean autoCommit = c.getAutoCommit();
                c.setAutoCommit(false);
                try {
                    fornecedorId = new FornecedorDAO().inserir(c,
                            new Fornecedor(0, "d13it_" + sufixo, null));
                    c.commit();
                } catch (Exception e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(autoCommit);
                }
            }

            Clock relogio = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);
            ContratoService service = new ContratoService(acao -> true, new FornecedorDAO(),
                    new autobotz.dao.ContratoDAO(), new autobotz.dao.LedgerContratoDAO(), relogio);
            LocalDate hoje = LocalDate.of(2026, 10, 27);

            ContratoFornecedor contratoAlerta = service.criarContrato(fornecedorId,
                    new BigDecimal("2500.00"), hoje.plusDays(10));
            contratosCriados.add(contratoAlerta.getId());
            check(contratoAlerta.getUltimaSequenciaLedger() == 1, "CRIACAO_REGISTRA_EVENTO_INICIAL");
            check(service.registrarAlertasVencimento(hoje).size() == 1, "ALERTA_REGISTRADO_NA_JANELA");
            check(service.registrarAlertasVencimento(hoje).isEmpty(), "ALERTA_NAO_DUPLICA");
            check(service.verificarIntegridade(contratoAlerta.getId()).isValido(), "CADEIA_VALIDADA_NO_BANCO");

            ContratoFornecedor contratoPagar = service.criarContrato(fornecedorId,
                    new BigDecimal("1250.00"), hoje.minusDays(1));
            contratosCriados.add(contratoPagar.getId());
            contratoPagar = service.alterarEstado(contratoPagar.getId(), StatusContrato.EM_NEGOCIACAO);
            contratoPagar = service.alterarEstado(contratoPagar.getId(), StatusContrato.ASSINADO);
            contratoPagar = service.alterarEstado(contratoPagar.getId(), StatusContrato.EM_EXECUCAO);
            service.validarNotaFiscal(contratoPagar.getId());
            check(service.processarPagamentosSimulados(hoje).size() == 1, "PAGAMENTO_SIMULADO_APOS_NF_E_VENCIMENTO");
            check(service.processarPagamentosSimulados(hoje).isEmpty(), "PAGAMENTO_SIMULADO_NAO_DUPLICA");
            check(service.verificarIntegridade(contratoPagar.getId()).isValido(), "CADEIA_APOS_EVENTOS_VALIDADA");

            List<RegistroLedgerContrato> eventos = service.listarEventos(contratoPagar.getId());
            check(eventos.stream().anyMatch(e -> e.getTipo() == TipoEventoContrato.PAGAMENTO_SIMULADO_LIBERADO),
                    "EVENTO_DE_PAGAMENTO_SIMULADO_PERSISTIDO");
            try (PreparedStatement ps = ConexaoBanco.getConexao().prepareStatement(
                    "UPDATE ledger_contratos SET payload = CONCAT(payload, ';alterado=true') "
                            + "WHERE contrato_id = ? AND sequencia = 1")) {
                ps.setLong(1, contratoPagar.getId());
                check(ps.executeUpdate() == 1, "ALTERACAO_DE_TESTE_APLICADA");
            }
            check(!service.verificarIntegridade(contratoPagar.getId()).isValido(), "PAYLOAD_ALTERADO_DETECTADO_NO_BANCO");
            try {
                service.alterarEstado(contratoPagar.getId(), StatusContrato.FINALIZADO);
                throw new AssertionError("Ledger corrompido deveria impedir escrita");
            } catch (ContratoException e) {
                check(e.getCodigo() == ContratoException.Codigo.INTEGRIDADE_COMPROMETIDA,
                        "LEDGER_CORROMPIDO_BLOQUEIA_NOVA_ESCRITA");
            }
        } finally {
            limpar();
        }
        System.out.println("D13_JDBC_OK checks=" + passed);
    }

    private static void limpar() throws Exception {
        try (Connection c = ConexaoBanco.getConexao()) {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                for (long id : contratosCriados) {
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM ledger_contratos WHERE contrato_id = ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM contratos_fornecedor WHERE id = ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                }
                if (fornecedorId > 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM fornecedores WHERE id = ? AND nome LIKE 'd13it_%'")) {
                        ps.setInt(1, fornecedorId);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        }
    }
}
