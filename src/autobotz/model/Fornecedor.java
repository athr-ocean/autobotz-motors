package autobotz.model;

/** Fornecedor com ID persistente, referenciado pelo D11 (compras) e pelo D13 (contratos). */
public class Fornecedor {
    private final int id;
    private final String nome;
    private final String cnpj; // 14 dígitos sem máscara, ou null

    public Fornecedor(int id, String nome, String cnpj) {
        this.id = id;
        this.nome = nome;
        this.cnpj = cnpj;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCnpj() { return cnpj; }

    @Override
    public String toString() {
        return "Fornecedor{id=" + id + ", nome=" + nome + "}";
    }
}
