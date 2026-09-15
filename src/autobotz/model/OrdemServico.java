package autobotz.model;

import java.time.LocalDate;

public class OrdemServico {
    private int id;
    private int clienteId;
    private int veiculoId;
    private LocalDate dataAbertura;
    private String status;

    public OrdemServico(int clienteId, int veiculoId, LocalDate dataAbertura, String status) {
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.dataAbertura = dataAbertura;
        this.status = status;
    }

    public OrdemServico(int id, int clienteId, int veiculoId,
            LocalDate dataAbertura, String status) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.dataAbertura = dataAbertura;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getClienteId() { return clienteId; }
    public int getVeiculoId() { return veiculoId; }
    public LocalDate getDataAbertura() { return dataAbertura; }
    public String getStatus() { return status; }
}
