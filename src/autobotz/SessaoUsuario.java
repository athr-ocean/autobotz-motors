package autobotz;

public class SessaoUsuario {

    private static SessaoUsuario instancia;

    private Usuario usuario;

    private SessaoUsuario() {
    }

    public static SessaoUsuario getInstancia() {

        if (instancia == null) {
            instancia = new SessaoUsuario();
        }

        return instancia;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void encerrarSessao() {
        usuario = null;
    }

    public boolean estaLogado() {
        return usuario != null;
    }
}