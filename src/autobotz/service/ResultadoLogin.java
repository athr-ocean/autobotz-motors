package autobotz.service;

import autobotz.Usuario;

/**
 * Resultado do primeiro passo do login. NEGADO não distingue usuário
 * inexistente de senha errada. BLOQUEADO vale para qualquer nome digitado
 * (existente ou não) e informa só o tempo restante, sem revelar se a conta existe.
 */
public final class ResultadoLogin {
    public enum Status { AUTENTICADO, MFA_NECESSARIA, NEGADO, BLOQUEADO }

    private final Status status;
    private final Usuario usuario;
    private final MfaService.Desafio desafio;
    private final MfaService.Status mfaStatus;
    private final long segundosRestantes;

    private ResultadoLogin(Status status, Usuario usuario, MfaService.Desafio desafio,
            MfaService.Status mfaStatus) {
        this(status, usuario, desafio, mfaStatus, 0);
    }

    private ResultadoLogin(Status status, Usuario usuario, MfaService.Desafio desafio,
            MfaService.Status mfaStatus, long segundosRestantes) {
        this.segundosRestantes = segundosRestantes;
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

    public static ResultadoLogin bloqueado(long segundosRestantes) {
        return new ResultadoLogin(Status.BLOQUEADO, null, null, null, segundosRestantes);
    }

    public static ResultadoLogin mfaFalha(MfaService.Status mfaStatus) {
        return new ResultadoLogin(Status.NEGADO, null, null, mfaStatus);
    }

    public Status getStatus() { return status; }

    /** Só preenchido em AUTENTICADO; em MFA_NECESSARIA ainda não há usuário liberado. */
    public Usuario getUsuario() { return usuario; }

    /** Só em BLOQUEADO: segundos até o login voltar a ser aceito (mínimo 1). */
    public long getSegundosRestantes() { return segundosRestantes; }

    /** "9 min 59 s", "2 min", "45 s". */
    public static String formatarTempo(long segundos) {
        long s = Math.max(1, segundos);
        long min = s / 60;
        long resto = s % 60;
        if (min == 0) {
            return resto + " s";
        }
        return resto == 0 ? min + " min" : min + " min " + resto + " s";
    }

    /** Só preenchido quando o NEGADO veio do passo MFA. */
    public MfaService.Status getMfaStatus() { return mfaStatus; }

    /** Só preenchido em MFA_NECESSARIA. */
    public MfaService.Desafio getDesafio() { return desafio; }
}
