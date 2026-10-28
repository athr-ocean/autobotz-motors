package autobotz.model;

import java.math.BigDecimal;

public final class ResultadoSimulacao {
    private final int quantidadeBarato;
    private final int quantidadePermanecem;
    private final int quantidadeCaro;
    private final BigDecimal mrrAntes;
    private final BigDecimal mrrDepois;

    public ResultadoSimulacao(int quantidadeBarato,
            int quantidadePermanecem, int quantidadeCaro,
            BigDecimal mrrAntes, BigDecimal mrrDepois) {
        this.quantidadeBarato = quantidadeBarato;
        this.quantidadePermanecem = quantidadePermanecem;
        this.quantidadeCaro = quantidadeCaro;
        this.mrrAntes = mrrAntes;
        this.mrrDepois = mrrDepois;
    }

    public int getQuantidadeBarato() {
        return quantidadeBarato;
    }

    public int getQuantidadePermanecem() {
        return quantidadePermanecem;
    }

    public int getQuantidadeCaro() {
        return quantidadeCaro;
    }

    public BigDecimal getMrrAntes() {
        return mrrAntes;
    }

    public BigDecimal getMrrDepois() {
        return mrrDepois;
    }
}
