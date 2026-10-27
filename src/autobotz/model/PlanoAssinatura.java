package autobotz.model;
import java.math.BigDecimal;

public class PlanoAssinatura {

	    private int id;
	    private String nome;
	    private BigDecimal precoBase;

	    public PlanoAssinatura(int id, String nome,
	                           BigDecimal precoBase) {
	        this.id = id;
	        this.nome = nome;
	        this.precoBase = precoBase;
	    }

	    public int getId() {
	        return id;
	    }

	    public String getNome() {
	        return nome;
	    }

	    public BigDecimal getPrecoBase() {
	        return precoBase;
	    }
		
}
