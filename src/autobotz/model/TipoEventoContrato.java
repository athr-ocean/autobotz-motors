package autobotz.model;

/** Eventos auditáveis do contrato; os registros são somente acrescentados ao ledger. */
public enum TipoEventoContrato {
    CONTRATO_CRIADO,
    ESTADO_ALTERADO,
    NOTA_FISCAL_VALIDADA,
    ALERTA_VENCIMENTO,
    PAGAMENTO_SIMULADO_LIBERADO
}
