package autobotz;

public class Cliente {

    private int id;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;
    private boolean ativo;

    public void setId(int id) {
        this.id = id;
    }

    public Cliente(int id, String nome) {
        this.id = id;
        this.nome = nome;
        this.ativo = true;
    }

    // Usado pelo Main atual
    public Cliente(String nome, String cpf, String telefone, String email) {
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.email = email;
        this.ativo = true;
    }

    // Usado para buscar/listar clientes
    public Cliente(int id, String nome, String cpf, String telefone, String email) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.email = email;
        this.ativo = true;
    }

    // Construtor completo, incluindo ativo
    public Cliente(int id, String nome, String cpf, String telefone,
                   String email, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.email = email;
        this.ativo = ativo;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
