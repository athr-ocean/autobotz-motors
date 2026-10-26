package autobotz.model;

import java.math.BigDecimal;

/** Solicitação de compra. A decisão (aprovador, motivo de rejeição) fica registrada na própria solicitação. */
public class SolicitacaoCompra {
    private final int id;
    private final int orcamentoId;
    private final int fornecedorId;
    private final String descricao;
    private final BigDecimal valor;
    private final StatusSolicitacao status;
    private final String solicitante;
    private final String aprovador;       // null enquanto PENDENTE
    private final String motivoRejeicao;  // null se não rejeitada

    public SolicitacaoCompra(int id, int orcamentoId, int fornecedorId, String descricao, BigDecimal valor,
                             StatusSolicitacao status, String solicitante, String aprovador, String motivoRejeicao) {
        this.id = id;
        this.orcamentoId = orcamentoId;
        this.fornecedorId = fornecedorId;
        this.descricao = descricao;
        this.valor = valor;
        this.status = status;
        this.solicitante = solicitante;
        this.aprovador = aprovador;
        this.motivoRejeicao = motivoRejeicao;
    }

    public int getId() { return id; }
    public int getOrcamentoId() { return orcamentoId; }
    public int getFornecedorId() { return fornecedorId; }
    public String getDescricao() { return descricao; }
    public BigDecimal getValor() { return valor; }
    public StatusSolicitacao getStatus() { return status; }
    public String getSolicitante() { return solicitante; }
    public String getAprovador() { return aprovador; }
    public String getMotivoRejeicao() { return motivoRejeicao; }

    @Override
    public String toString() {
        return "SolicitacaoCompra{id=" + id + ", status=" + status + ", valor=" + valor + "}";
    }
}
