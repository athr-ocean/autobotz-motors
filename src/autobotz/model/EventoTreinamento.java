package autobotz.model;

import java.util.Objects;

/** Evento mínimo de acesso/visualização do portal de treinamento. */
public final class EventoTreinamento {
    public static final String PORTAL_OPEN = "PORTAL_OPEN";
    public static final String MATERIAL_VIEW = "MATERIAL_VIEW";
    public static final String VIDEO_VIEW = "VIDEO_VIEW";

    private final String tipoEvento;
    private final String materialSlug;

    public EventoTreinamento(String tipoEvento, String materialSlug) {
        this.tipoEvento = Objects.requireNonNull(tipoEvento, "tipoEvento");
        this.materialSlug = materialSlug;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getMaterialSlug() {
        return materialSlug;
    }
}
