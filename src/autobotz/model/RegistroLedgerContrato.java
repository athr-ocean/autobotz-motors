package autobotz.model;

import autobotz.util.LedgerContratoHash;
import java.time.LocalDateTime;
import java.util.Objects;

/** Registro imutável de uma mudança no contrato, encadeado ao registro anterior. */
public final class RegistroLedgerContrato {
    private final long id;
    private final long contratoId;
    private final int sequencia;
    private final TipoEventoContrato tipo;
    private final String payload;
    private final String hashConteudo;
    private final String hashAnterior;
    private final String hashRegistro;
    private final LocalDateTime dataHora;

    public RegistroLedgerContrato(long id, long contratoId, int sequencia, TipoEventoContrato tipo,
                                  String payload, String hashConteudo, String hashAnterior,
                                  String hashRegistro, LocalDateTime dataHora) {
        this.id = id;
        this.contratoId = contratoId;
        this.sequencia = sequencia;
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.hashConteudo = Objects.requireNonNull(hashConteudo, "hashConteudo");
        this.hashAnterior = Objects.requireNonNull(hashAnterior, "hashAnterior");
        this.hashRegistro = Objects.requireNonNull(hashRegistro, "hashRegistro");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora");
    }

    public static RegistroLedgerContrato novo(long contratoId, int sequencia, TipoEventoContrato tipo,
                                               String payload, String hashAnterior, LocalDateTime dataHora) {
        String hashConteudo = LedgerContratoHash.hashConteudo(payload);
        String hashRegistro = LedgerContratoHash.hashRegistro(contratoId, sequencia, tipo,
                hashConteudo, hashAnterior, dataHora);
        return new RegistroLedgerContrato(0, contratoId, sequencia, tipo, payload,
                hashConteudo, hashAnterior, hashRegistro, dataHora);
    }

    public long getId() { return id; }
    public long getContratoId() { return contratoId; }
    public int getSequencia() { return sequencia; }
    public TipoEventoContrato getTipo() { return tipo; }
    public String getPayload() { return payload; }
    public String getHashConteudo() { return hashConteudo; }
    public String getHashAnterior() { return hashAnterior; }
    public String getHashRegistro() { return hashRegistro; }
    public LocalDateTime getDataHora() { return dataHora; }
}
