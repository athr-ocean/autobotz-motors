package autobotz.service;

import autobotz.Usuario;

/**
 * Resultado do primeiro passo do login. Propositalmente não distingue
 * usuário inexistente, senha errada e conta bloqueada (anti-enumeração).
 */
public final class ResultadoLogin {
    public enum Status { AUTENTICADO, MFA_NECESSARIA, NEGADO }

    private final Status status;
    private final Usuario usuario;
    private final MfaService.Desafio desafio;
    private final MfaService.Status mfaStatus;

    private ResultadoLogin(Status status, Usuario usuario, MfaService.Desafio desafio,
            MfaService.Status mfaStatus) {
        this.status = status;
        this.usuario = usuario;
        this.desafio = desafio;
        this.mfaStatus = mfaStatus;
    }

    public static ResultadoLogin autenticado(Usuario usuario) {
        return new ResultadoLogin(Status.AUTENTICADO, usuario, null, null);
    }

    public static ResultadoLogin mfaNecessaria(MfaService.Desafio desafio) {
        return new ResultadoLogin(Status.MFA_NECESSARIA, null, desafio, null);
    }

    public static ResultadoLogin negado() {
        return new ResultadoLogin(Status.NEGADO, null, null, null);
    }

    public static ResultadoLogin mfaFalha(MfaService.Status mfaStatus) {
        return new ResultadoLogin(Status.NEGADO, null, null, mfaStatus);
    }

    public Status getStatus() { return status; }

    /** Só preenchido em AUTENTICADO; em MFA_NECESSARIA ainda não há usuário liberado. */
    public Usuario getUsuario() { return usuario; }

    /** Só preenchido quando o NEGADO veio do passo MFA. */
    public MfaService.Status getMfaStatus() { return mfaStatus; }

    /** Só preenchido em MFA_NECESSARIA. */
    public MfaService.Desafio getDesafio() { return desafio; }
}
