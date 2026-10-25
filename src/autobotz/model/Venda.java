package autobotz.model;

import java.time.LocalDate;

public class Venda {
    private int id;
    private int idVeiculo;
    private int idCliente;
    private double valorTotal;
    private LocalDate dataVenda;

    // Construtor padrao sem argumentos
    public Venda() {}

    // Construtor completo
    public Venda(int idVeiculo, int idCliente, double valorTotal) {
        this.idVeiculo = idVeiculo;
        this.idCliente = idCliente;
        this.valorTotal = valorTotal;
        this.dataVenda = LocalDate.now();
    }

    public Venda(int id, int idVeiculo, int idCliente, double valorTotal, LocalDate dataVenda) {
        this(idVeiculo, idCliente, valorTotal);
        this.id = id;
        this.dataVenda = dataVenda;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdVeiculo() { return idVeiculo; }
    public void setIdVeiculo(int idVeiculo) { this.idVeiculo = idVeiculo; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }

    public LocalDate getDataVenda() { return dataVenda; }
    public void setDataVenda(LocalDate dataVenda) { this.dataVenda = dataVenda; }
}
