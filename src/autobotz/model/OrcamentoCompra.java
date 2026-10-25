package autobotz.model;

import java.math.BigDecimal;

/** Orçamento disponível para compras. O saldo só diminui quando um pedido é gerado. */
public class OrcamentoCompra {
    private final int id;
    private final String nome;
    private final BigDecimal saldoInicial;
    private final BigDecimal saldoAtual;

    public OrcamentoCompra(int id, String nome, BigDecimal saldoInicial, BigDecimal saldoAtual) {
        this.id = id;
        this.nome = nome;
        this.saldoInicial = saldoInicial;
        this.saldoAtual = saldoAtual;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public BigDecimal getSaldoAtual() { return saldoAtual; }

    @Override
    public String toString() {
        return "OrcamentoCompra{id=" + id + ", nome=" + nome + ", saldoAtual=" + saldoAtual + "}";
    }
}
