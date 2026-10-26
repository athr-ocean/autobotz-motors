package autobotz.model;

/** Linha agregada de métricas para um tipo de evento e um material. */
public final class MetricaTreinamento {
    private final String tipoEvento;
    private final String materialSlug;
    private final long total;

    public MetricaTreinamento(String tipoEvento, String materialSlug, long total) {
        this.tipoEvento = tipoEvento;
        this.materialSlug = materialSlug;
        this.total = total;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getMaterialSlug() {
        return materialSlug;
    }

    public long getTotal() {
        return total;
    }
}
