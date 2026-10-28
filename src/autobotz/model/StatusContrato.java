package autobotz.model;

/** Estados permitidos no ciclo de vida de um contrato com fornecedor. */
public enum StatusContrato {
    RASCUNHO,
    EM_NEGOCIACAO,
    ASSINADO,
    EM_EXECUCAO,
    FINALIZADO,
    CANCELADO;

    public boolean podeIrPara(StatusContrato destino) {
        if (destino == null) return false;
        switch (this) {
            case RASCUNHO:
                return destino == EM_NEGOCIACAO || destino == CANCELADO;
            case EM_NEGOCIACAO:
                return destino == ASSINADO || destino == CANCELADO;
            case ASSINADO:
                return destino == EM_EXECUCAO || destino == CANCELADO;
            case EM_EXECUCAO:
                return destino == FINALIZADO || destino == CANCELADO;
            default:
                return false;
        }
    }

    public boolean isTerminal() {
        return this == FINALIZADO || this == CANCELADO;
    }
}
