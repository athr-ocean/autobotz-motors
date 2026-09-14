package autobotz.model;

import java.time.LocalDateTime;

public class LogAuditoria {
    private int id;
    private String acao;
    private int usuarioId;
    private LocalDateTime dataHora;

    public LogAuditoria(String acao, int usuarioId) {
        this.acao = acao;
        this.usuarioId = usuarioId;
        this.dataHora = LocalDateTime.now(); 
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getAcao() { return acao; }
    public int getUsuarioId() { return usuarioId; }
    public LocalDateTime getDataHora() { return dataHora; }
}