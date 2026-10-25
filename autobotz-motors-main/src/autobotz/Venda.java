package autobotz;

public class Venda {
    private int id;
    private int idCliente;
    private int idVeiculo;
    private double valorFinal;

    public Venda(int idCliente, int idVeiculo, double valorFinal) {
        this.idCliente = idCliente;
        this.idVeiculo = idVeiculo;
        this.valorFinal = valorFinal;
    }

    public Venda(int id, int idCliente, int idVeiculo, double valorFinal) {
        this.id = id;
        this.idCliente = idCliente;
        this.idVeiculo = idVeiculo;
        this.valorFinal = valorFinal;
    }

    public int getId() { return id; }
    public int getIdCliente() { return idCliente; }
    public int getIdVeiculo() { return idVeiculo; }
    public double getValorFinal() { return valorFinal; }
}