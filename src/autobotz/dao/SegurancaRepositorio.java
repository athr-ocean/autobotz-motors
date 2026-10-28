package autobotz.dao;

import autobotz.model.DesafioMfa;
import autobotz.model.EventoSeguranca;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * Persistência da trilha de segurança e dos desafios MFA (D14).
 * Interface para permitir testes com relógio controlável e sem banco.
 */
public interface SegurancaRepositorio {

    void registrarEvento(EventoSeguranca evento) throws SQLException;

    /**
     * Horários (mais antigo primeiro) das falhas de LOGIN do usuário após
     * {@code janelaInicio} e após o último LOGIN bem-sucedido (um acerto zera
     * a contagem). Não inclui eventos LOGIN_BLOQUEADO.
     */
    List<Instant> listarFalhasLogin(String nomeUsuario, Instant janelaInicio) throws SQLException;

    /** Mais recentes primeiro. */
    List<EventoSeguranca> listarEventos(int limite) throws SQLException;

    long criarDesafioMfa(int idUsuario, String codigoHash, Instant criadoEm,
            Instant expiraEm, int maxTentativas) throws SQLException;

    /** Marca como consumidos os desafios ainda abertos do usuário. */
    void invalidarDesafiosPendentes(int idUsuario) throws SQLException;

    DesafioMfa buscarDesafioMfa(long idDesafio) throws SQLException;

    /** Soma uma tentativa de forma atômica; false se já consumido ou esgotado. */
    boolean incrementarTentativaMfa(long idDesafio) throws SQLException;

    /** Consome o desafio uma única vez; false se já consumido ou expirado. */
    boolean consumirDesafioMfa(long idDesafio, Instant agora) throws SQLException;

    void invalidarDesafio(long idDesafio) throws SQLException;
}
