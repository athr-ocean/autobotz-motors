package autobotz.service;

import autobotz.AutorizacaoService;
import autobotz.AutorizacaoService.Permissao;
import autobotz.dao.SegurancaDAO;
import autobotz.dao.SegurancaRepositorio;
import autobotz.model.EventoSeguranca;
import java.sql.SQLException;
import java.util.List;

/** Consulta restrita da trilha de segurança (D14). A permissão é checada aqui, não na tela. */
public class SegurancaService {
    private final SegurancaRepositorio repo;
    private final AutorizacaoService autorizacao;

    public SegurancaService(AutorizacaoService autorizacao) {
        this(new SegurancaDAO(), autorizacao);
    }

    public SegurancaService(SegurancaRepositorio repo, AutorizacaoService autorizacao) {
        this.repo = repo;
        this.autorizacao = autorizacao;
    }

    /** @throws SecurityException se a sessão atual não tiver CONSULTAR_EVENTOS_SEGURANCA */
    public List<EventoSeguranca> consultarEventos(int limite) throws SQLException {
        if (!autorizacao.temPermissao(Permissao.CONSULTAR_EVENTOS_SEGURANCA)) {
            throw new SecurityException("Permissao negada: CONSULTAR_EVENTOS_SEGURANCA");
        }
        return repo.listarEventos(limite);
    }
}
