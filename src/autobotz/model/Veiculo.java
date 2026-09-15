package autobotz.model;

public class Veiculo {
    private int id;
    private String placa;
    private String marca;
    private String modelo;
    private int ano;
    private double preco;
    private String status;

    public Veiculo(String placa, String modelo, String marca, int ano, double preco) {
        this.placa = placa;
        this.modelo = modelo;
        this.marca = marca;
        this.ano = ano;
        this.preco = preco;
        this.status = "DISPONIVEL";
    }

    public Veiculo(int id, String placa, String modelo, String marca, int ano, double preco, String status) {
        this.id = id;
        this.placa = placa;
        this.modelo = modelo;
        this.marca = marca;
        this.ano = ano;
        this.preco = preco;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "ID: " + id + " | Placa: " + placa + " | " + marca + " " + modelo
                + " | Ano: " + ano + " | Preco: R$" + preco + " | Status: " + status;
    }
}
