import autobotz.AutorizacaoService;
import autobotz.PerfilUsuario;

/** Verifica a política de acesso desktop atual para a tela de compras D11. */
public final class D11ComprasPermissaoTest {
  private D11ComprasPermissaoTest() {}

  public static void main(String[] args) {
    AutorizacaoService autorizacao = new AutorizacaoService(null);
    check(autorizacao.temPermissao(PerfilUsuario.ADMIN, AutorizacaoService.Permissao.ACESSAR_COMPRAS),
        "ADMIN_PODE_ACESSAR_COMPRAS");
    check(!autorizacao.temPermissao(PerfilUsuario.VENDEDOR, AutorizacaoService.Permissao.ACESSAR_COMPRAS),
        "VENDEDOR_NAO_PODE_ACESSAR_COMPRAS");
    check(!autorizacao.temPermissao(null, AutorizacaoService.Permissao.ACESSAR_COMPRAS),
        "PERFIL_AUSENTE_NAO_PODE_ACESSAR_COMPRAS");
    check(autorizacao.temPermissao(PerfilUsuario.ADMIN,
        AutorizacaoService.Permissao.ENVIAR_PEDIDO_GOVCONNECT),
        "ADMIN_PODE_ENVIAR_PEDIDO_GOVCONNECT");
    check(!autorizacao.temPermissao(PerfilUsuario.VENDEDOR,
        AutorizacaoService.Permissao.ENVIAR_PEDIDO_GOVCONNECT),
        "VENDEDOR_NAO_PODE_ENVIAR_PEDIDO_GOVCONNECT");
    check(!autorizacao.temPermissao(null,
        AutorizacaoService.Permissao.ENVIAR_PEDIDO_GOVCONNECT),
        "PERFIL_AUSENTE_NAO_PODE_ENVIAR_PEDIDO_GOVCONNECT");
    System.out.println("D11_COMPRAS_PERMISSAO_OK=6");
  }

  private static void check(boolean condicao, String nome) {
    if (!condicao)
      throw new AssertionError("FAIL " + nome);
    System.out.println("PASS " + nome);
  }
}
