package autobotz;

public class SessaoUsuario {
    private static final SessaoUsuario INSTANCIA = new SessaoUsuario();
    private Usuario usuario;

    private SessaoUsuario() {
    }

    public static SessaoUsuario getInstancia() {
        return INSTANCIA;
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

    public boolean possuiPerfil(PerfilUsuario perfil) {
        return usuario != null && usuario.getPerfil() == perfil;
    }
}
