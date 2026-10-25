package autobotz.model;

import java.time.LocalDateTime;

public class LogAuditoria {
    private long id;
    private Integer usuarioId;
    private String nomeUsuario;
    private String acao;
    private String entidade;
    private Integer entidadeId;
    private String detalhes;
    private LocalDateTime dataHora;

    public LogAuditoria(Integer usuarioId, String nomeUsuario, String acao,
            String entidade, Integer entidadeId, String detalhes) {
        this.usuarioId = usuarioId;
        this.nomeUsuario = nomeUsuario;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhes = detalhes;
    }

    public LogAuditoria(long id, Integer usuarioId, String nomeUsuario, String acao,
            String entidade, Integer entidadeId, String detalhes, LocalDateTime dataHora) {
        this(usuarioId, nomeUsuario, acao, entidade, entidadeId, detalhes);
        this.id = id;
        this.dataHora = dataHora;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Integer getUsuarioId() { return usuarioId; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public Integer getEntidadeId() { return entidadeId; }
    public String getDetalhes() { return detalhes; }
    public LocalDateTime getDataHora() { return dataHora; }
}
