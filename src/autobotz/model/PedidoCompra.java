package autobotz.model;

import java.math.BigDecimal;

/** Pedido gerado a partir de uma solicitação aprovada. Há no máximo um pedido por solicitação. */
public class PedidoCompra {
    private final int id;
    private final int solicitacaoId;
    private final int orcamentoId;
    private final int fornecedorId;
    private final BigDecimal valor;

    public PedidoCompra(int id, int solicitacaoId, int orcamentoId, int fornecedorId, BigDecimal valor) {
        this.id = id;
        this.solicitacaoId = solicitacaoId;
        this.orcamentoId = orcamentoId;
        this.fornecedorId = fornecedorId;
        this.valor = valor;
    }

    public int getId() { return id; }
    public int getSolicitacaoId() { return solicitacaoId; }
    public int getOrcamentoId() { return orcamentoId; }
    public int getFornecedorId() { return fornecedorId; }
    public BigDecimal getValor() { return valor; }

    @Override
    public String toString() {
        return "PedidoCompra{id=" + id + ", solicitacaoId=" + solicitacaoId + ", valor=" + valor + "}";
    }
}
