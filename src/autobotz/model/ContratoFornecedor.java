package autobotz.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Contrato de fornecedor. O fornecedor é referenciado pelo ID estável do módulo D11. */
public final class ContratoFornecedor {
    public static final String HASH_GENESIS = "0".repeat(64);

    private final long id;
    private final int fornecedorId;
    private final BigDecimal valor;
    private final LocalDate dataVencimento;
    private final StatusContrato estado;
    private final boolean notaFiscalValidada;
    private final boolean pagamentoSimuladoLiberado;
    private final int ultimaSequenciaLedger;
    private final String ultimoHashLedger;
    private final LocalDateTime criadoEm;

    public ContratoFornecedor(long id, int fornecedorId, BigDecimal valor, LocalDate dataVencimento,
                              StatusContrato estado, boolean notaFiscalValidada,
                              boolean pagamentoSimuladoLiberado, int ultimaSequenciaLedger,
                              String ultimoHashLedger, LocalDateTime criadoEm) {
        this.id = id;
        this.fornecedorId = fornecedorId;
        this.valor = Objects.requireNonNull(valor, "valor");
        this.dataVencimento = Objects.requireNonNull(dataVencimento, "dataVencimento");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.notaFiscalValidada = notaFiscalValidada;
        this.pagamentoSimuladoLiberado = pagamentoSimuladoLiberado;
        this.ultimaSequenciaLedger = ultimaSequenciaLedger;
        this.ultimoHashLedger = Objects.requireNonNull(ultimoHashLedger, "ultimoHashLedger");
        this.criadoEm = criadoEm;
    }

    public long getId() { return id; }
    public int getFornecedorId() { return fornecedorId; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public StatusContrato getEstado() { return estado; }
    public boolean isNotaFiscalValidada() { return notaFiscalValidada; }
    public boolean isPagamentoSimuladoLiberado() { return pagamentoSimuladoLiberado; }
    public int getUltimaSequenciaLedger() { return ultimaSequenciaLedger; }
    public String getUltimoHashLedger() { return ultimoHashLedger; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
