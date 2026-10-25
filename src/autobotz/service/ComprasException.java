package autobotz.service;

import java.util.Locale;

/**
 * Erro de regra de negócio/validação do módulo de compras (distinto de SQLException, que indica falha de banco).
 * A UI deve traduzir por {@link #getChaveMensagem()} nos bundles PT-BR/EN-US; {@link #getMessage()} é só para log.
 */
public class ComprasException extends Exception {
    private static final long serialVersionUID = 1L;

    public enum Codigo {
        DADO_INVALIDO,
        NAO_AUTORIZADO,
        NAO_ENCONTRADO,
        DUPLICADO,
        TRANSICAO_INVALIDA,
        ORCAMENTO_INSUFICIENTE
    }

    private final Codigo codigo;

    public ComprasException(Codigo codigo, String detalhe) {
        super(detalhe);
        this.codigo = codigo;
    }

    public Codigo getCodigo() {
        return codigo;
    }

    /** Ex.: {@code compras.erro.orcamento_insuficiente}. */
    public String getChaveMensagem() {
        return "compras.erro." + codigo.name().toLowerCase(Locale.ROOT);
    }
}
