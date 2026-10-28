package autobotz.service;

import autobotz.integracao.ApiBlockchainSimulada;
import autobotz.dao.ContratoDAO;
import autobotz.dao.FornecedorDAO;
import autobotz.dao.LedgerContratoDAO;
import autobotz.model.ContratoFornecedor;
import autobotz.model.Fornecedor;
import autobotz.model.RegistroLedgerContrato;
import autobotz.model.ResultadoIntegridadeLedger;
import autobotz.model.StatusContrato;
import autobotz.model.TipoEventoContrato;
import autobotz.util.ConexaoBanco;
import autobotz.util.LedgerContratoHash;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Regras do D13. O ledger é uma simulação local, sem rede blockchain nem pagamento real. */
public class ContratoService {
    public static final int JANELA_ALERTA_DIAS = 10;
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999999.99");

    public enum Acao {
        CONSULTAR, GERENCIAR, VALIDAR_NOTA_FISCAL, PROCESSAR_VENCIMENTOS, INTEGRAR_API_SIMULADA
    }

    @FunctionalInterface
    public interface Autorizador { boolean permite(Acao acao); }

    @FunctionalInterface
    private interface Operacao<T> { T executar(Connection c) throws SQLException, ContratoException; }

    private final Autorizador autorizador;
    private final FornecedorDAO fornecedorDAO;
    private final ContratoDAO contratoDAO;
    private final LedgerContratoDAO ledgerDAO;
    private final Clock clock;

    public ContratoService(Autorizador autorizador) {
        this(autorizador, new FornecedorDAO(), new ContratoDAO(), new LedgerContratoDAO(), Clock.systemUTC());
    }

    public ContratoService(Autorizador autorizador, FornecedorDAO fornecedorDAO,
                           ContratoDAO contratoDAO, LedgerContratoDAO ledgerDAO, Clock clock) {
        this.autorizador = Objects.requireNonNull(autorizador, "autorizador");
        this.fornecedorDAO = Objects.requireNonNull(fornecedorDAO, "fornecedorDAO");
        this.contratoDAO = Objects.requireNonNull(contratoDAO, "contratoDAO");
        this.ledgerDAO = Objects.requireNonNull(ledgerDAO, "ledgerDAO");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ContratoFornecedor criarContrato(int fornecedorId, BigDecimal valor, LocalDate vencimento)
            throws SQLException, ContratoException {
        exigir(Acao.GERENCIAR);
        BigDecimal valorValidado = validarValor(valor);
        if (fornecedorId <= 0 || vencimento == null) {
            throw erro(ContratoException.Codigo.DADO_INVALIDO, "Fornecedor e vencimento são obrigatórios");
        }
        return emTransacao(c -> {
            Fornecedor fornecedor = fornecedorDAO.buscarPorId(c, fornecedorId);
            if (fornecedor == null) throw erro(ContratoException.Codigo.NAO_ENCONTRADO, "Fornecedor não encontrado");
            long id = contratoDAO.inserir(c, fornecedorId, valorValidado, vencimento);
            ContratoFornecedor contrato = contratoDAO.buscarPorId(c, id, true);
            acrescentarEvento(c, null, contrato, TipoEventoContrato.CONTRATO_CRIADO, "criado");
            return contratoDAO.buscarPorId(c, id, false);
        });
    }

    public ContratoFornecedor consultar(long contratoId) throws SQLException, ContratoException {
        exigir(Acao.CONSULTAR);
        return emTransacao(c -> buscarObrigatorio(c, contratoId, false));
    }

    public ContratoFornecedor alterarEstado(long contratoId, StatusContrato novoEstado)
            throws SQLException, ContratoException {
        exigir(Acao.GERENCIAR);
        if (novoEstado == null) throw erro(ContratoException.Codigo.DADO_INVALIDO, "Estado obrigatório");
        return emTransacao(c -> {
            ContratoFornecedor atual = buscarObrigatorio(c, contratoId, true);
            if (!atual.getEstado().podeIrPara(novoEstado)) {
                throw erro(ContratoException.Codigo.TRANSICAO_INVALIDA,
                        atual.getEstado() + " -> " + novoEstado + " não é uma transição permitida");
            }
            if (!contratoDAO.atualizarEstado(c, contratoId, atual.getEstado(), novoEstado)) {
                throw erro(ContratoException.Codigo.TRANSICAO_INVALIDA, "Contrato alterado por outra operação");
            }
            ContratoFornecedor atualizado = contratoDAO.buscarPorId(c, contratoId, false);
            acrescentarEvento(c, atual, atualizado, TipoEventoContrato.ESTADO_ALTERADO,
                    "de=" + atual.getEstado().name() + ";para=" + novoEstado.name());
            return contratoDAO.buscarPorId(c, contratoId, false);
        });
    }

    public ContratoFornecedor validarNotaFiscal(long contratoId) throws SQLException, ContratoException {
        exigir(Acao.VALIDAR_NOTA_FISCAL);
        return emTransacao(c -> {
            ContratoFornecedor atual = buscarObrigatorio(c, contratoId, true);
            if (atual.isNotaFiscalValidada()) {
                throw erro(ContratoException.Codigo.TRANSICAO_INVALIDA, "Nota fiscal já validada");
            }
            if (!contratoDAO.marcarNotaFiscalValidada(c, contratoId)) {
                throw erro(ContratoException.Codigo.TRANSICAO_INVALIDA, "Contrato alterado por outra operação");
            }
            ContratoFornecedor atualizado = contratoDAO.buscarPorId(c, contratoId, false);
            acrescentarEvento(c, atual, atualizado, TipoEventoContrato.NOTA_FISCAL_VALIDADA, "notaFiscalValidada=true");
            return contratoDAO.buscarPorId(c, contratoId, false);
        });
    }

    /** Emite eventos de alerta uma única vez para contratos que vencem hoje ou nos próximos 10 dias. */
    public List<ContratoFornecedor> registrarAlertasVencimento(LocalDate hoje) throws SQLException, ContratoException {
        exigir(Acao.PROCESSAR_VENCIMENTOS);
        LocalDate dataReferencia = Objects.requireNonNull(hoje, "hoje");
        return emTransacao(c -> {
            List<ContratoFornecedor> alertados = new ArrayList<>();
            for (ContratoFornecedor item : contratoDAO.listarNaJanelaVencimento(c, dataReferencia,
                    dataReferencia.plusDays(JANELA_ALERTA_DIAS))) {
                ContratoFornecedor atual = contratoDAO.buscarPorId(c, item.getId(), true);
                if (atual == null || atual.getEstado().isTerminal()
                        || !estaNaJanelaDeAlerta(atual.getDataVencimento(), dataReferencia)
                        || ledgerDAO.existeEvento(c, atual.getId(), TipoEventoContrato.ALERTA_VENCIMENTO)) {
                    continue;
                }
                acrescentarEvento(c, atual, atual, TipoEventoContrato.ALERTA_VENCIMENTO,
                        "vencimento=" + atual.getDataVencimento() + ";janelaDias=" + JANELA_ALERTA_DIAS);
                alertados.add(contratoDAO.buscarPorId(c, atual.getId(), false));
            }
            return alertados;
        });
    }

    /** Libera somente um estado simulado; nenhuma cobrança ou transferência é realizada. */
    public List<ContratoFornecedor> processarPagamentosSimulados(LocalDate hoje)
            throws SQLException, ContratoException {
        exigir(Acao.PROCESSAR_VENCIMENTOS);
        LocalDate dataReferencia = Objects.requireNonNull(hoje, "hoje");
        return emTransacao(c -> {
            List<ContratoFornecedor> liberados = new ArrayList<>();
            for (ContratoFornecedor item : contratoDAO.listarPendentesPagamento(c, dataReferencia)) {
                ContratoFornecedor atual = contratoDAO.buscarPorId(c, item.getId(), true);
                if (atual == null || !podeLiberarPagamento(atual, dataReferencia)) continue;
                if (!contratoDAO.liberarPagamentoSimulado(c, atual.getId(), dataReferencia)) continue;
                ContratoFornecedor atualizado = contratoDAO.buscarPorId(c, atual.getId(), false);
                acrescentarEvento(c, atual, atualizado, TipoEventoContrato.PAGAMENTO_SIMULADO_LIBERADO,
                        "vencimento=" + atual.getDataVencimento() + ";notaFiscalValidada=true;pagamento=SIMULADO");
                liberados.add(contratoDAO.buscarPorId(c, atual.getId(), false));
            }
            return liberados;
        });
    }

    public List<RegistroLedgerContrato> listarEventos(long contratoId) throws SQLException, ContratoException {
        exigir(Acao.CONSULTAR);
        return emTransacao(c -> {
            ContratoFornecedor contrato = buscarObrigatorio(c, contratoId, false);
            return ledgerDAO.listarPorContrato(c, contrato.getId());
        });
    }

    public ResultadoIntegridadeLedger verificarIntegridade(long contratoId) throws SQLException, ContratoException {
        exigir(Acao.CONSULTAR);
        return emTransacao(c -> {
            ContratoFornecedor contrato = buscarObrigatorio(c, contratoId, false);
            return verificarIntegridade(c, contrato);
        });
    }

    /**
     * Envia um retrato mínimo do contrato ao endpoint local de simulação e registra a resposta
     * recebida no ledger. A chamada HTTP ocorre fora da transação JDBC; a versão enviada é
     * conferida novamente antes de gravar o retorno.
     */
    public ApiBlockchainSimulada.Resposta enviarParaApiBlockchainSimulada(
            long contratoId, ApiBlockchainSimulada.Cliente cliente)
            throws SQLException, ContratoException, IOException, InterruptedException {
        exigir(Acao.INTEGRAR_API_SIMULADA);
        Objects.requireNonNull(cliente, "cliente");
        ContratoFornecedor contratoEnviado = emTransacao(c -> {
            ContratoFornecedor contrato = buscarObrigatorio(c, contratoId, false);
            ResultadoIntegridadeLedger integridade = verificarIntegridade(c, contrato);
            if (!integridade.isValido()) {
                throw erro(ContratoException.Codigo.INTEGRIDADE_COMPROMETIDA,
                        "Não é possível enviar um contrato com ledger inválido");
            }
            return contrato;
        });

        ApiBlockchainSimulada.Resposta resposta = cliente.enviarContrato(contratoEnviado);
        if (resposta.contratoId() != contratoEnviado.getId()
                || !resposta.hashLedger().equals(contratoEnviado.getUltimoHashLedger())
                || !resposta.simulado() || resposta.status() == null) {
            throw erro(ContratoException.Codigo.DADO_INVALIDO,
                    "O retorno da API simulada não corresponde à versão enviada");
        }

        emTransacao(c -> {
            ContratoFornecedor atual = buscarObrigatorio(c, contratoId, true);
            if (!resposta.hashLedger().equals(atual.getUltimoHashLedger())) {
                throw erro(ContratoException.Codigo.TRANSICAO_INVALIDA,
                        "O contrato mudou enquanto aguardava a resposta da API simulada");
            }
            ResultadoIntegridadeLedger integridade = verificarIntegridade(c, atual);
            if (!integridade.isValido()) {
                throw erro(ContratoException.Codigo.INTEGRIDADE_COMPROMETIDA,
                        "O ledger foi alterado durante a integração simulada");
            }
            acrescentarEvento(c, atual, atual, TipoEventoContrato.STATUS_API_SIMULADA_RECEBIDO,
                    "status=" + resposta.status().name() + ";protocolo=" + resposta.protocolo()
                            + ";simulado=true");
            if (resposta.status() == ApiBlockchainSimulada.Status.LIBERACAO_SIMULADA_ELEGIVEL) {
                ContratoFornecedor antesPagamento = contratoDAO.buscarPorId(c, contratoId, true);
                LocalDate hoje = LocalDate.now(clock);
                if (!podeLiberarPagamento(antesPagamento, hoje)
                        || !contratoDAO.liberarPagamentoSimulado(c, contratoId, hoje)) {
                    throw erro(ContratoException.Codigo.TRANSICAO_INVALIDA,
                            "A API simulada informou elegibilidade, mas a regra local não permite a liberação");
                }
                ContratoFornecedor depoisPagamento = contratoDAO.buscarPorId(c, contratoId, false);
                acrescentarEvento(c, antesPagamento, depoisPagamento,
                        TipoEventoContrato.PAGAMENTO_SIMULADO_LIBERADO,
                        "origem=API_SIMULADA;vencimento=" + depoisPagamento.getDataVencimento()
                                + ";notaFiscalValidada=true;pagamento=SIMULADO");
            }
            return null;
        });
        return resposta;
    }

    public static boolean estaNaJanelaDeAlerta(LocalDate vencimento, LocalDate hoje) {
        return vencimento != null && hoje != null && !vencimento.isBefore(hoje)
                && !vencimento.isAfter(hoje.plusDays(JANELA_ALERTA_DIAS));
    }

    public static boolean podeLiberarPagamento(ContratoFornecedor contrato, LocalDate hoje) {
        return contrato != null && hoje != null && contrato.isNotaFiscalValidada()
                && !contrato.isPagamentoSimuladoLiberado() && !contrato.getDataVencimento().isAfter(hoje)
                && !contrato.getEstado().isTerminal();
    }

    private ResultadoIntegridadeLedger verificarIntegridade(Connection c, ContratoFornecedor contrato)
            throws SQLException {
        List<RegistroLedgerContrato> eventos = ledgerDAO.listarPorContrato(c, contrato.getId());
        ResultadoIntegridadeLedger resultado = LedgerContratoHash.verificar(eventos,
                contrato.getUltimaSequenciaLedger(), contrato.getUltimoHashLedger());
        if (!resultado.isValido()) return resultado;
        if (eventos.isEmpty()) {
            return ResultadoIntegridadeLedger.invalido(1, "Contrato sem evento de criação");
        }
        if (!LedgerContratoHash.retratoCorrespondeAoContrato(contrato, eventos.get(eventos.size() - 1))) {
            return ResultadoIntegridadeLedger.invalido(eventos.size(), "Dados atuais divergem do último evento");
        }
        return ResultadoIntegridadeLedger.valido();
    }

    private void acrescentarEvento(Connection c, ContratoFornecedor antes, ContratoFornecedor atual,
                                   TipoEventoContrato tipo, String detalhe)
            throws SQLException, ContratoException {
        List<RegistroLedgerContrato> existentes = ledgerDAO.listarPorContrato(c, atual.getId());
        ResultadoIntegridadeLedger verificacao = LedgerContratoHash.verificar(existentes,
                atual.getUltimaSequenciaLedger(), atual.getUltimoHashLedger());
        if (!verificacao.isValido()) {
            throw erro(ContratoException.Codigo.INTEGRIDADE_COMPROMETIDA,
                    "Ledger inválido na sequência " + verificacao.getSequenciaComProblema());
        }
        if (existentes.isEmpty()) {
            if (antes != null || atual.getUltimaSequenciaLedger() != 0
                    || !ContratoFornecedor.HASH_GENESIS.equals(atual.getUltimoHashLedger())) {
                throw erro(ContratoException.Codigo.INTEGRIDADE_COMPROMETIDA,
                        "Contrato sem o histórico esperado no ledger");
            }
        } else if (antes == null || !LedgerContratoHash.retratoCorrespondeAoContrato(
                antes, existentes.get(existentes.size() - 1))) {
            throw erro(ContratoException.Codigo.INTEGRIDADE_COMPROMETIDA,
                    "Dados atuais divergem do último evento do ledger");
        }
        int proximaSequencia = atual.getUltimaSequenciaLedger() + 1;
        LocalDateTime agoraUtc = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        RegistroLedgerContrato evento = RegistroLedgerContrato.novo(atual.getId(), proximaSequencia,
                tipo, LedgerContratoHash.retratoContrato(atual) + ";detalhe=" + detalhe,
                atual.getUltimoHashLedger(), agoraUtc);
        ledgerDAO.inserir(c, evento);
        if (!contratoDAO.atualizarAncoraLedger(c, atual.getId(), atual.getUltimaSequenciaLedger(),
                atual.getUltimoHashLedger(), proximaSequencia, evento.getHashRegistro())) {
            throw erro(ContratoException.Codigo.TRANSICAO_INVALIDA, "Âncora do ledger alterada por outra operação");
        }
    }

    private ContratoFornecedor buscarObrigatorio(Connection c, long id, boolean bloquear)
            throws SQLException, ContratoException {
        if (id <= 0) throw erro(ContratoException.Codigo.DADO_INVALIDO, "ID de contrato inválido");
        ContratoFornecedor contrato = contratoDAO.buscarPorId(c, id, bloquear);
        if (contrato == null) throw erro(ContratoException.Codigo.NAO_ENCONTRADO, "Contrato não encontrado");
        return contrato;
    }

    private BigDecimal validarValor(BigDecimal valor) throws ContratoException {
        if (valor == null || valor.signum() <= 0 || valor.compareTo(VALOR_MAXIMO) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            throw erro(ContratoException.Codigo.DADO_INVALIDO, "Valor deve ser positivo e ter até duas casas decimais");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void exigir(Acao acao) throws ContratoException {
        if (!autorizador.permite(acao)) throw erro(ContratoException.Codigo.NAO_AUTORIZADO, "Ação não permitida");
    }

    private static ContratoException erro(ContratoException.Codigo codigo, String detalhe) {
        return new ContratoException(codigo, detalhe);
    }

    private static <T> T emTransacao(Operacao<T> operacao) throws SQLException, ContratoException {
        try (Connection c = ConexaoBanco.getConexao()) {
            boolean autoCommitAnterior = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T resultado = operacao.executar(c);
                c.commit();
                return resultado;
            } catch (SQLException | ContratoException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException falha) { e.addSuppressed(falha); }
                throw e;
            } finally {
                try { c.setAutoCommit(autoCommitAnterior); } catch (SQLException ignorada) { /* conexão será fechada */ }
            }
        }
    }
}
