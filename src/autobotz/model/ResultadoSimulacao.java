package autobotz.model;

import java.math.BigDecimal;

public class ResultadoSimulacao {

    private int migraramBarato;
    private int permaneceram;
    private int migraramCaro;

    private BigDecimal mrrAntes;
    private BigDecimal mrrDepois;

    public ResultadoSimulacao(
            int migraramBarato,
            int permaneceram,
            int migraramCaro,
            BigDecimal mrrAntes,
            BigDecimal mrrDepois) {

        this.migraramBarato = migraramBarato;
        this.permaneceram = permaneceram;
        this.migraramCaro = migraramCaro;
        this.mrrAntes = mrrAntes;
        this.mrrDepois = mrrDepois;
    }

    public int getMigraramBarato() {
        return migraramBarato;
    }

    public int getPermaneceram() {
        return permaneceram;
    }

    public int getMigraramCaro() {
        return migraramCaro;
    }

    public BigDecimal getMrrAntes() {
        return mrrAntes;
    }

    public BigDecimal getMrrDepois() {
        return mrrDepois;
    }

    public BigDecimal getDiferenca() {
        return mrrDepois.subtract(mrrAntes);
    }
}