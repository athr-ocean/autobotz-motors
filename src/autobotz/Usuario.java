package autobotz;

public class Usuario {

    private int id;
    private String nomeUsuario;
    private String senhaHash;
    private PerfilUsuario perfil;

    public Usuario(int id, String nomeUsuario, String senhaHash, PerfilUsuario perfil) {
        this.id = id;
        this.nomeUsuario = nomeUsuario;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public Usuario(String nomeUsuario, String senhaHash, PerfilUsuario perfil) {
        this.nomeUsuario = nomeUsuario;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public int getId() {
        return id;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public void setId(int id) {
        this.id = id;
    }
}