package autobotz.service;

import java.util.Locale;

/** Erro de validação/regra do D13, separado de falhas JDBC. */
public class ContratoException extends Exception {
    private static final long serialVersionUID = 1L;

    public enum Codigo {
        DADO_INVALIDO,
        NAO_AUTORIZADO,
        NAO_ENCONTRADO,
        TRANSICAO_INVALIDA,
        INTEGRIDADE_COMPROMETIDA
    }

    private final Codigo codigo;

    public ContratoException(Codigo codigo, String detalhe) {
        super(detalhe);
        this.codigo = codigo;
    }

    public Codigo getCodigo() { return codigo; }

    public String getChaveMensagem() {
        return "contrato.erro." + codigo.name().toLowerCase(Locale.ROOT);
    }
}
