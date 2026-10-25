package autobotz.service;

import autobotz.dao.CompraDAO;
import autobotz.dao.FornecedorDAO;
import autobotz.model.Fornecedor;
import autobotz.model.OrcamentoCompra;
import autobotz.model.PedidoCompra;
import autobotz.model.SolicitacaoCompra;
import autobotz.model.StatusSolicitacao;
import autobotz.service.ComprasException.Codigo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Fluxo mínimo de compras do D11: solicitação -> aprovação -> pedido, com baixa de orçamento.
 *
 * Contrato de erros: regras de negócio e validação lançam {@link ComprasException};
 * falhas de banco lançam {@link SQLException}.
 *
 * Garantia transacional: em {@link #aprovar}, a baixa do saldo, a criação do pedido e a mudança de
 * status acontecem na mesma transação; qualquer falha desfaz tudo (rollback).
 */
public class ComprasService {

    /** Ações protegidas. O mapeamento para o AutorizacaoService é feito por quem instancia o serviço. */
    public enum Acao {
        CONSULTAR,
        SOLICITAR,
        APROVAR,
        GERENCIAR_CADASTROS
    }

    @FunctionalInterface
    public interface Autorizador {
        boolean permite(Acao acao);
    }

    @FunctionalInterface
    private interface Transacao<T> {
        T executar(Connection c) throws SQLException, ComprasException;
    }

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999999.99"); // limite de DECIMAL(12,2)

    private final Autorizador autorizador;
    private final FornecedorDAO fornecedorDAO;
    private final CompraDAO compraDAO;

    public ComprasService(Autorizador autorizador) {
        this(autorizador, new FornecedorDAO(), new CompraDAO());
    }

    /** Construtor com DAOs injetados (usado nos testes para simular falha intermediária). */
    public ComprasService(Autorizador autorizador, FornecedorDAO fornecedorDAO, CompraDAO compraDAO) {
        this.autorizador = Objects.requireNonNull(autorizador, "autorizador");
        this.fornecedorDAO = Objects.requireNonNull(fornecedorDAO, "fornecedorDAO");
        this.compraDAO = Objects.requireNonNull(compraDAO, "compraDAO");
    }

    /**
     * Única linha que decide o banco usado pelo serviço.
     * Deve apontar para a mesma classe usada em FornecedorDAO.abrirConexao().
     */
    private static Connection abrirConexao() throws SQLException {
        return autobotz.util.ConexaoBanco.getConexao();          // OFICIAL
        // return autobotz.util.FabricaConexao.getConexao();     // TESTES (banco1): use esta linha no lugar da de cima
    }

    /** Nome do banco em que o serviço grava. Existe para o teste recusar bancos não liberados. */
    public String nomeDoBanco() throws SQLException {
        try (Connection c = abrirConexao()) {
            return c.getCatalog();
        }
    }

    // ---------- cadastros ----------

    public Fornecedor cadastrarFornecedor(String nome, String cnpj) throws ComprasException, SQLException {
        exigir(Acao.GERENCIAR_CADASTROS);
        final String nomeLimpo = textoObrigatorio(nome, "nome", 120);
        final String cnpjNormalizado = normalizarCnpj(cnpj);
        return emTransacao(c -> {
            if (cnpjNormalizado != null && fornecedorDAO.buscarPorCnpj(c, cnpjNormalizado) != null) {
                throw new ComprasException(Codigo.DUPLICADO, "CNPJ já cadastrado");
            }
            int id = fornecedorDAO.inserir(c, new Fornecedor(0, nomeLimpo, cnpjNormalizado));
            return new Fornecedor(id, nomeLimpo, cnpjNormalizado);
        });
    }

    public Fornecedor buscarFornecedor(int id) throws ComprasException, SQLException {
        exigir(Acao.CONSULTAR);
        return emTransacao(c -> {
            Fornecedor f = fornecedorDAO.buscarPorId(c, id);
            if (f == null) {
                throw naoEncontrado("Fornecedor " + id);
            }
            return f;
        });
    }

    public OrcamentoCompra criarOrcamento(String nome, BigDecimal saldoInicial) throws ComprasException, SQLException {
        exigir(Acao.GERENCIAR_CADASTROS);
        final String nomeLimpo = textoObrigatorio(nome, "nome", 120);
        final BigDecimal saldo = valorMonetario(saldoInicial, "saldoInicial", true);
        return emTransacao(c -> {
            int id = compraDAO.inserirOrcamento(c, new OrcamentoCompra(0, nomeLimpo, saldo, saldo));
            return new OrcamentoCompra(id, nomeLimpo, saldo, saldo);
        });
    }

    // ---------- fluxo de compra ----------

    public SolicitacaoCompra criarSolicitacao(int orcamentoId, int fornecedorId, String descricao,
                                              BigDecimal valor, String solicitante)
            throws ComprasException, SQLException {
        exigir(Acao.SOLICITAR);
        final String desc = textoObrigatorio(descricao, "descricao", 200);
        final String quem = textoObrigatorio(solicitante, "solicitante", 60);
        final BigDecimal v = valorMonetario(valor, "valor", false);
        return emTransacao(c -> {
            if (compraDAO.buscarOrcamento(c, orcamentoId, false) == null) {
                throw naoEncontrado("Orçamento " + orcamentoId);
            }
            if (fornecedorDAO.buscarPorId(c, fornecedorId) == null) {
                throw naoEncontrado("Fornecedor " + fornecedorId);
            }
            int id = compraDAO.inserirSolicitacao(c, new SolicitacaoCompra(0, orcamentoId, fornecedorId,
                    desc, v, StatusSolicitacao.PENDENTE, quem, null, null));
            return compraDAO.buscarSolicitacao(c, id, false);
        });
    }

    /**
     * Aprova a solicitação, baixa o orçamento e gera o pedido, tudo numa única transação.
     * Aprovar de novo uma solicitação já processada lança TRANSICAO_INVALIDA e não altera nada.
     */
    public PedidoCompra aprovar(int solicitacaoId, String aprovador) throws ComprasException, SQLException {
        exigir(Acao.APROVAR);
        final String quem = textoObrigatorio(aprovador, "aprovador", 60);
        return emTransacao(c -> {
            SolicitacaoCompra s = compraDAO.buscarSolicitacao(c, solicitacaoId, true);
            if (s == null) {
                throw naoEncontrado("Solicitação " + solicitacaoId);
            }
            exigirTransicao(s.getStatus(), StatusSolicitacao.APROVADA);

            OrcamentoCompra o = compraDAO.buscarOrcamento(c, s.getOrcamentoId(), true);
            if (o == null) {
                throw naoEncontrado("Orçamento " + s.getOrcamentoId());
            }
            if (o.getSaldoAtual().compareTo(s.getValor()) < 0) {
                throw new ComprasException(Codigo.ORCAMENTO_INSUFICIENTE,
                        "Saldo insuficiente no orçamento " + o.getId());
            }

            if (!compraDAO.registrarDecisao(c, s.getId(), StatusSolicitacao.PENDENTE,
                    StatusSolicitacao.APROVADA, quem, null)) {
                throw new ComprasException(Codigo.TRANSICAO_INVALIDA, "Solicitação alterada por outra operação");
            }
            if (!compraDAO.debitarSaldo(c, o.getId(), s.getValor())) {
                throw new ComprasException(Codigo.ORCAMENTO_INSUFICIENTE,
                        "Saldo insuficiente no orçamento " + o.getId());
            }
            compraDAO.inserirPedido(c, new PedidoCompra(0, s.getId(), o.getId(), s.getFornecedorId(), s.getValor()));

            exigirTransicao(StatusSolicitacao.APROVADA, StatusSolicitacao.PEDIDO_GERADO);
            if (!compraDAO.avancarStatus(c, s.getId(), StatusSolicitacao.APROVADA, StatusSolicitacao.PEDIDO_GERADO)) {
                throw new ComprasException(Codigo.TRANSICAO_INVALIDA, "Solicitação alterada por outra operação");
            }
            return compraDAO.buscarPedidoPorSolicitacao(c, s.getId());
        });
    }

    /** Rejeita uma solicitação PENDENTE. Não mexe no saldo. */
    public SolicitacaoCompra rejeitar(int solicitacaoId, String aprovador, String motivo)
            throws ComprasException, SQLException {
        exigir(Acao.APROVAR);
        final String quem = textoObrigatorio(aprovador, "aprovador", 60);
        final String motivoLimpo = textoObrigatorio(motivo, "motivo", 200);
        return emTransacao(c -> {
            SolicitacaoCompra s = compraDAO.buscarSolicitacao(c, solicitacaoId, true);
            if (s == null) {
                throw naoEncontrado("Solicitação " + solicitacaoId);
            }
            exigirTransicao(s.getStatus(), StatusSolicitacao.REJEITADA);
            if (!compraDAO.registrarDecisao(c, s.getId(), StatusSolicitacao.PENDENTE,
                    StatusSolicitacao.REJEITADA, quem, motivoLimpo)) {
                throw new ComprasException(Codigo.TRANSICAO_INVALIDA, "Solicitação alterada por outra operação");
            }
            return compraDAO.buscarSolicitacao(c, s.getId(), false);
        });
    }

    // ---------- consultas ----------

    public BigDecimal consultarSaldo(int orcamentoId) throws ComprasException, SQLException {
        exigir(Acao.CONSULTAR);
        return emTransacao(c -> {
            OrcamentoCompra o = compraDAO.buscarOrcamento(c, orcamentoId, false);
            if (o == null) {
                throw naoEncontrado("Orçamento " + orcamentoId);
            }
            return o.getSaldoAtual();
        });
    }

    public SolicitacaoCompra consultarSolicitacao(int solicitacaoId) throws ComprasException, SQLException {
        exigir(Acao.CONSULTAR);
        return emTransacao(c -> {
            SolicitacaoCompra s = compraDAO.buscarSolicitacao(c, solicitacaoId, false);
            if (s == null) {
                throw naoEncontrado("Solicitação " + solicitacaoId);
            }
            return s;
        });
    }

    /** Retorna o pedido gerado pela solicitação, ou null se ainda não houver. */
    public PedidoCompra consultarPedidoDaSolicitacao(int solicitacaoId) throws ComprasException, SQLException {
        exigir(Acao.CONSULTAR);
        return emTransacao(c -> compraDAO.buscarPedidoPorSolicitacao(c, solicitacaoId));
    }

    // ---------- infraestrutura interna ----------

    private void exigir(Acao acao) throws ComprasException {
        if (!autorizador.permite(acao)) {
            throw new ComprasException(Codigo.NAO_AUTORIZADO, "Ação não permitida: " + acao);
        }
    }

    private static void exigirTransicao(StatusSolicitacao de, StatusSolicitacao para) throws ComprasException {
        if (!de.podeIrPara(para)) {
            throw new ComprasException(Codigo.TRANSICAO_INVALIDA, "Transição inválida: " + de + " -> " + para);
        }
    }

    private static ComprasException naoEncontrado(String o) {
        return new ComprasException(Codigo.NAO_ENCONTRADO, o + " não encontrado(a)");
    }

    private <T> T emTransacao(Transacao<T> corpo) throws SQLException, ComprasException {
        try (Connection c = abrirConexao()) {
            boolean autoCommitAnterior = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T resultado = corpo.executar(c);
                c.commit();
                return resultado;
            } catch (SQLException | ComprasException | RuntimeException e) {
                reverter(c, e);
                throw e;
            } finally {
                try {
                    c.setAutoCommit(autoCommitAnterior);
                } catch (SQLException ignorado) {
                    // a conexão será fechada em seguida; não há o que recuperar aqui
                }
            }
        }
    }

    private static void reverter(Connection c, Exception causa) {
        try {
            c.rollback();
        } catch (SQLException falhaNoRollback) {
            causa.addSuppressed(falhaNoRollback);
        }
    }

    private static String textoObrigatorio(String valor, String campo, int max) throws ComprasException {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ComprasException(Codigo.DADO_INVALIDO, "Campo obrigatório: " + campo);
        }
        String t = valor.trim();
        if (t.length() > max) {
            throw new ComprasException(Codigo.DADO_INVALIDO, "Campo muito longo: " + campo);
        }
        return t;
    }

    private static BigDecimal valorMonetario(BigDecimal v, String campo, boolean permitirZero)
            throws ComprasException {
        if (v == null) {
            throw new ComprasException(Codigo.DADO_INVALIDO, "Campo obrigatório: " + campo);
        }
        if (v.stripTrailingZeros().scale() > 2) {
            throw new ComprasException(Codigo.DADO_INVALIDO, "Máximo de 2 casas decimais: " + campo);
        }
        int sinal = v.signum();
        if (sinal < 0 || (sinal == 0 && !permitirZero)) {
            throw new ComprasException(Codigo.DADO_INVALIDO, "Valor fora do intervalo permitido: " + campo);
        }
        if (v.compareTo(VALOR_MAXIMO) > 0) {
            throw new ComprasException(Codigo.DADO_INVALIDO, "Valor acima do limite: " + campo);
        }
        return v.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String normalizarCnpj(String cnpj) throws ComprasException {
        if (cnpj == null || cnpj.trim().isEmpty()) {
            return null;
        }
        String digitos = cnpj.replaceAll("\\D", "");
        if (digitos.length() != 14) {
            throw new ComprasException(Codigo.DADO_INVALIDO, "CNPJ deve ter 14 dígitos");
        }
        return digitos;
    }
}