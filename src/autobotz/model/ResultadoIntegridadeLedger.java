package autobotz.model;

/** Resultado da verificação da cadeia, incluindo a primeira sequência inválida quando conhecida. */
public final class ResultadoIntegridadeLedger {
    private final boolean valido;
    private final int sequenciaComProblema;
    private final String motivo;

    private ResultadoIntegridadeLedger(boolean valido, int sequenciaComProblema, String motivo) {
        this.valido = valido;
        this.sequenciaComProblema = sequenciaComProblema;
        this.motivo = motivo;
    }

    public static ResultadoIntegridadeLedger valido() {
        return new ResultadoIntegridadeLedger(true, 0, "OK");
    }

    public static ResultadoIntegridadeLedger invalido(int sequencia, String motivo) {
        return new ResultadoIntegridadeLedger(false, sequencia, motivo);
    }

    public boolean isValido() { return valido; }
    public int getSequenciaComProblema() { return sequenciaComProblema; }
    public String getMotivo() { return motivo; }
}
