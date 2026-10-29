package autobotz.model;

import java.math.BigDecimal;
public class ModuloOpcional {
	  private int id;
	    private String nome;
	    private BigDecimal preco;

	    public ModuloOpcional(int id, String nome, BigDecimal preco) {
	        this.id = id;
	        this.nome = nome;
	        this.preco = preco;
	    }

	    public int getId() {
	        return id;
	    }

	    public String getNome() {
	        return nome;
	    }

	    public BigDecimal getPreco() {
	        return preco;
	    }
}
