// src/autobotz/Veiculo.java
package autobotz;

public class Veiculo {
	private int id;
	private String marca;
	private String modelo;
	private int ano;
	private double preco;
	private String status;

	public Veiculo(String marca, String modelo, int ano, double preco) {
		this.marca = marca;
		this.modelo = modelo;
		this.ano = ano;
		this.preco = preco;
		this.status = "Disponivel";
	}

	public Veiculo(int id, String marca, String modelo, int ano, double preco, String status) {
		this.id = id;
		this.marca = marca;
		this.modelo = modelo;
		this.ano = ano;
		this.preco = preco;
		this.status = status;
	}

	public int getId() { return id; }
	public void setId(int id) { this.id = id; }
	public String getMarca() { return marca; }
	public String getModelo() { return modelo; }
	public int getAno() { return ano; }
	public double getPreco() { return preco; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
}