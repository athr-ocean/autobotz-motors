package autobotz.model;

/**
 * Estados de uma solicitação de compra.
 *
 * Transições permitidas:
 *   PENDENTE -> APROVADA | REJEITADA
 *   APROVADA -> PEDIDO_GERADO
 * REJEITADA e PEDIDO_GERADO são terminais.
 */
public enum StatusSolicitacao {
    PENDENTE,
    APROVADA,
    REJEITADA,
    PEDIDO_GERADO;

    public boolean podeIrPara(StatusSolicitacao destino) {
        switch (this) {
            case PENDENTE:
                return destino == APROVADA || destino == REJEITADA;
            case APROVADA:
                return destino == PEDIDO_GERADO;
            default:
                return false;
        }
    }

    public boolean isTerminal() {
        return this == REJEITADA || this == PEDIDO_GERADO;
    }
}
