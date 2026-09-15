package autobotz.model;

public class ItemOS {
    private int id;
    private int ordemServicoId;
    private int servicoId;
    private int quantidade;
    private double preco;

    public ItemOS(int ordemServicoId, int servicoId, int quantidade, double preco) {
        this.ordemServicoId = ordemServicoId;
        this.servicoId = servicoId;
        this.quantidade = quantidade;
        this.preco = preco;
    }

    public ItemOS(int id, int ordemServicoId, int servicoId,
            int quantidade, double preco) {
        this.id = id;
        this.ordemServicoId = ordemServicoId;
        this.servicoId = servicoId;
        this.quantidade = quantidade;
        this.preco = preco;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOrdemServicoId() { return ordemServicoId; }
    public int getServicoId() { return servicoId; }
    public int getQuantidade() { return quantidade; }
    public double getPreco() { return preco; }
}
