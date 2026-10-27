import autobotz.AutorizacaoService;
import autobotz.AutorizacaoService.Permissao;
import autobotz.PerfilUsuario;
import autobotz.SessaoUsuario;
import autobotz.Usuario;
import autobotz.model.EventoSeguranca;
import autobotz.service.MfaService;
import autobotz.service.ResultadoLogin;
import autobotz.service.SessaoWebService;
import autobotz.service.SessaoWebService.Acesso;
import autobotz.util.SenhaHash;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;

/**
 * D14 - sessão de servidor para o portal: /metricas = 401 sem sessão válida, 403 sem ADMIN,
 * 200 só para ADMIN autenticado (senha + MFA) no servidor. Sem banco: usa os dublês de
 * D14SegurancaTest (relógio controlável, repositório e usuários em memória).
 */
public class D14SessaoWebTest {
  static int passed = 0;

  static void check(boolean ok, String name) {
    if (!ok)
      throw new AssertionError("FAIL " + name);
    passed++;
    System.out.println("PASS " + name);
  }

  static final String SENHA_ADMIN = "Adm1n-web-ok";
  static final String SENHA_VEND = "Vend-web-ok";
  static String hashAdmin;
  static String hashVend;

  static class Ctx {
    final D14SegurancaTest.MutableClock clock = new D14SegurancaTest.MutableClock();
    final D14SegurancaTest.FakeUsuarioDAO dao = new D14SegurancaTest.FakeUsuarioDAO();
    final D14SegurancaTest.MemRepo repo = new D14SegurancaTest.MemRepo();
    final SessaoWebService svc = new SessaoWebService(dao, repo, clock);
    final Usuario admin = dao.add("admin", hashAdmin, PerfilUsuario.ADMIN);
    final Usuario vend = dao.add("vendedor", hashVend, PerfilUsuario.VENDEDOR);

    String tokenVendedor() throws Exception {
      var l = svc.iniciarLogin("vendedor", SENHA_VEND);
      check(l.status() == ResultadoLogin.Status.AUTENTICADO && l.token() != null, "LOGIN_WEB_VENDEDOR_SEM_MFA");
      return l.token();
    }

    String tokenAdmin() throws Exception {
      var l1 = svc.iniciarLogin("admin", SENHA_ADMIN);
      var l2 = svc.concluirMfa(l1.desafio().id(), l1.desafio().codigoSimulado());
      return l2.token();
    }
  }

  static int metricas(Ctx c, String token) throws Exception {
    return c.svc.autorizarMetricas(token).httpStatus();
  }

  public static void main(String[] args) throws Exception {
    hashAdmin = SenhaHash.gerar(SENHA_ADMIN);
    hashVend = SenhaHash.gerar(SENHA_VEND);
    String tokFalso = "A".repeat(43);

    // ===== 401 / 403 / 200 =====
    {
      Ctx c = new Ctx();
      check(metricas(c, null) == 401 && metricas(c, "") == 401 && metricas(c, "lixo!") == 401
          && metricas(c, tokFalso) == 401, "METRICAS_401_SEM_SESSAO_VALIDA");
      String tv = c.tokenVendedor();
      var dv = c.svc.autorizarMetricas(tv);
      check(dv.acesso() == Acesso.PROIBIDO && dv.httpStatus() == 403, "METRICAS_403_VENDEDOR_AUTENTICADO");
      check(c.svc.autenticar(tv).httpStatus() == 200
          && c.svc.autorizar(tv, Permissao.LISTAR_CLIENTES).httpStatus() == 200
          && c.svc.autorizar(tv, Permissao.EXCLUIR_VEICULO).httpStatus() == 403,
          "VENDEDOR_RECEBE_SO_O_QUE_A_MATRIZ_PERMITE");
      String ta = c.tokenAdmin();
      var da = c.svc.autorizarMetricas(ta);
      check(da.acesso() == Acesso.PERMITIDO && da.httpStatus() == 200
          && da.usuario().getNomeUsuario().equals("admin"), "METRICAS_200_SOMENTE_ADMIN_AUTENTICADO");
      check(metricas(c, tv) == 403 && metricas(c, ta) == 200, "SESSOES_INDEPENDENTES");
    }

    // ===== ADMIN so tem sessao depois do MFA =====
    {
      Ctx c = new Ctx();
      var l1 = c.svc.iniciarLogin("admin", SENHA_ADMIN);
      check(l1.status() == ResultadoLogin.Status.MFA_NECESSARIA && l1.token() == null && l1.desafio() != null,
          "ADMIN_SENHA_SOZINHA_NAO_GERA_TOKEN");
      String errado = l1.desafio().codigoSimulado().equals("000000") ? "111111" : "000000";
      var f = c.svc.concluirMfa(l1.desafio().id(), errado);
      check(f.status() == ResultadoLogin.Status.NEGADO && f.token() == null
          && f.mfaStatus() == MfaService.Status.INVALIDO, "MFA_ERRADO_NAO_GERA_TOKEN");
      var ok = c.svc.concluirMfa(l1.desafio().id(), l1.desafio().codigoSimulado());
      check(ok.status() == ResultadoLogin.Status.AUTENTICADO && ok.token() != null
          && metricas(c, ok.token()) == 200, "MFA_CORRETO_GERA_TOKEN_ADMIN");
      var reuso = c.svc.concluirMfa(l1.desafio().id(), l1.desafio().codigoSimulado());
      check(reuso.token() == null && reuso.mfaStatus() == MfaService.Status.JA_UTILIZADO,
          "MFA_REUTILIZADO_NAO_GERA_SEGUNDO_TOKEN");
      var l3 = c.svc.iniciarLogin("admin", "senha-errada");
      check(l3.status() == ResultadoLogin.Status.NEGADO && l3.token() == null && l3.desafio() == null,
          "SENHA_ERRADA_WEB_NEGADA");
    }

    // ===== Bloqueio por tentativas vale na web =====
    {
      Ctx c = new Ctx();
      for (int i = 0; i < 4; i++)
        c.svc.iniciarLogin("vendedor", "x" + i);
      var quinta = c.svc.iniciarLogin("vendedor", "x4");
      check(quinta.status() == ResultadoLogin.Status.BLOQUEADO && quinta.segundosRestantes() == 600
          && quinta.token() == null, "WEB_QUINTA_FALHA_BLOQUEIA_COM_TEMPO");
      var certa = c.svc.iniciarLogin("vendedor", SENHA_VEND);
      check(certa.status() == ResultadoLogin.Status.BLOQUEADO && certa.token() == null,
          "WEB_BLOQUEADO_NAO_EMITE_TOKEN_NEM_COM_SENHA_CERTA");
    }

    // ===== Logout =====
    {
      Ctx c = new Ctx();
      String ta = c.tokenAdmin();
      check(metricas(c, ta) == 200 && c.svc.logout(ta), "LOGOUT_ENCERRA_SESSAO");
      check(metricas(c, ta) == 401 && c.svc.autenticar(ta).httpStatus() == 401, "TOKEN_APOS_LOGOUT_DA_401");
      check(!c.svc.logout(ta) && !c.svc.logout(null) && !c.svc.logout("lixo"), "LOGOUT_IDEMPOTENTE");
      check(c.repo.eventos.stream().anyMatch(e -> e.tipo().equals("LOGOUT") && e.nomeUsuario().equals("admin")
          && e.sucesso()), "LOGOUT_NA_TRILHA");
      String t1 = c.tokenVendedor();
      String t2 = c.tokenVendedor();
      c.svc.logout(t1);
      check(!t1.equals(t2) && metricas(c, t1) == 401 && c.svc.autenticar(t2).httpStatus() == 200,
          "LOGOUT_AFETA_SO_A_PROPRIA_SESSAO");
    }

    // ===== Expiracao =====
    {
      Ctx c = new Ctx();
      String t = c.tokenAdmin();
      c.clock.advance(SessaoWebService.OCIOSIDADE.minusSeconds(1));
      check(metricas(c, t) == 200, "SESSAO_VALE_ATE_29M59_OCIOSA");
      c.clock.advance(SessaoWebService.OCIOSIDADE.minusSeconds(1));
      check(metricas(c, t) == 200, "ATIVIDADE_RENOVA_OCIOSIDADE");
      c.clock.advance(SessaoWebService.OCIOSIDADE);
      check(metricas(c, t) == 401, "SESSAO_EXPIRA_POR_OCIOSIDADE");
      check(metricas(c, t) == 401, "SESSAO_EXPIRADA_NAO_REVIVE");
    }
    {
      Ctx c = new Ctx();
      String t = c.tokenAdmin();
      boolean ok = true;
      for (int i = 0; i < 23; i++) { // 23 x 20 min = 7h40, sempre com atividade
        c.clock.advance(Duration.ofMinutes(20));
        ok &= metricas(c, t) == 200;
      }
      check(ok, "SESSAO_ATIVA_CONTINUA_VALIDA_ANTES_DO_LIMITE");
      c.clock.advance(Duration.ofMinutes(20)); // 8h exatas
      check(metricas(c, t) == 401, "SESSAO_EXPIRA_NA_DURACAO_MAXIMA_MESMO_ATIVA");
    }

    // ===== Perfil e conta sao relidos do banco =====
    {
      Ctx c = new Ctx();
      String t = c.tokenAdmin();
      c.dao.porId.put(c.admin.getId(), new Usuario(c.admin.getId(), "admin", hashAdmin, PerfilUsuario.VENDEDOR));
      check(metricas(c, t) == 403, "ADMIN_REBAIXADO_PERDE_ACESSO_NA_PROXIMA_REQUISICAO");
      c.dao.porId.remove(c.admin.getId());
      check(metricas(c, t) == 401, "USUARIO_REMOVIDO_PERDE_SESSAO");
    }

    // ===== Isolamento da sessao do desktop =====
    {
      Ctx c = new Ctx();
      var desktop = SessaoUsuario.getInstancia();
      try {
        desktop.encerrarSessao();
        c.tokenAdmin();
        c.tokenVendedor();
        check(!desktop.estaLogado(), "LOGIN_WEB_NAO_TOCA_SESSAO_DO_DESKTOP");
        desktop.setUsuario(c.admin); // alguem logado como ADMIN no Swing
        check(metricas(c, null) == 401 && metricas(c, tokFalso) == 401,
            "SESSAO_DO_DESKTOP_NAO_VIRA_IDENTIDADE_HTTP");
        c.svc.logout(c.tokenAdmin());
        check(desktop.estaLogado() && desktop.getUsuario() == c.admin, "LOGOUT_WEB_NAO_ENCERRA_SESSAO_DO_DESKTOP");
      } finally {
        desktop.encerrarSessao();
      }
    }

    // ===== Trilha e vazamento =====
    {
      Ctx c = new Ctx();
      String tv = c.tokenVendedor();
      metricas(c, tv);
      var nega = c.repo.eventos.stream().filter(e -> e.tipo().equals("ACESSO_NEGADO")).toList();
      check(nega.size() == 1 && nega.get(0).nomeUsuario().equals("vendedor") && !nega.get(0).sucesso()
          && nega.get(0).causa().equals("SEM_PERMISSAO:CONSULTAR_METRICAS_TREINAMENTO"),
          "ACESSO_NEGADO_403_NA_TRILHA");
      metricas(c, null);
      metricas(c, tokFalso);
      check(c.repo.eventos.stream().filter(e -> e.tipo().equals("ACESSO_NEGADO")).count() == 1,
          "401_ANONIMO_NAO_POLUI_A_TRILHA");
      String ta = c.tokenAdmin();
      c.svc.logout(ta);
      boolean vaza = c.repo.eventos.stream().anyMatch(e -> e.toString().contains(tv) || e.toString().contains(ta));
      var login = c.svc.iniciarLogin("vendedor", SENHA_VEND);
      check(!vaza && !login.toString().contains(login.token()), "TOKEN_NAO_APARECE_NA_TRILHA_NEM_NO_TOSTRING");
      check(tv.length() == 43 && !tv.equals(ta) && tv.matches("[A-Za-z0-9_-]+"), "TOKEN_256_BITS_URL_SAFE");
    }

    // ===== Extracao do token (Authorization / cookie) =====
    {
      String t = "Ab3_-".repeat(9); // 45 chars validos
      check(t.equals(SessaoWebService.extrairToken("Bearer " + t, null)), "EXTRAI_BEARER");
      check(t.equals(SessaoWebService.extrairToken("bearer   " + t + "  ", null)), "EXTRAI_BEARER_MINUSCULO_COM_ESPACOS");
      check(t.equals(SessaoWebService.extrairToken(null, "a=1; ABM_SESSAO=" + t + "; b=2")), "EXTRAI_COOKIE");
      check(t.equals(SessaoWebService.extrairToken("Bearer " + t, "ABM_SESSAO=" + "B".repeat(43))), "AUTHORIZATION_TEM_PRIORIDADE");
      check(SessaoWebService.extrairToken(null, null) == null
          && SessaoWebService.extrairToken("Basic " + t, null) == null
          && SessaoWebService.extrairToken("Bearer ", null) == null
          && SessaoWebService.extrairToken("Bearer curto", null) == null
          && SessaoWebService.extrairToken("Bearer " + t + "!", null) == null
          && SessaoWebService.extrairToken(null, "OUTRO=" + t) == null
          && SessaoWebService.extrairToken(null, "ABM_SESSAO=" + "x".repeat(200)) == null,
          "TOKEN_AUSENTE_OU_MALFORMADO_VIRA_NULL");
    }

    // ===== Matriz =====
    {
      var autz = new AutorizacaoService(null);
      check(autz.temPermissao(PerfilUsuario.ADMIN, Permissao.CONSULTAR_METRICAS_TREINAMENTO)
          && !autz.temPermissao(PerfilUsuario.VENDEDOR, Permissao.CONSULTAR_METRICAS_TREINAMENTO)
          && !autz.temPermissao(null, Permissao.CONSULTAR_METRICAS_TREINAMENTO),
          "MATRIZ_METRICAS_SOMENTE_ADMIN");
      check(autz.temPermissao(PerfilUsuario.VENDEDOR, Permissao.REALIZAR_VENDA)
          && !autz.temPermissao(PerfilUsuario.VENDEDOR, Permissao.EXCLUIR_VEICULO)
          && !autz.temPermissao(PerfilUsuario.VENDEDOR, Permissao.CONSULTAR_EVENTOS_SEGURANCA),
          "MATRIZ_ANTIGA_DO_VENDEDOR_INALTERADA");
      var desktop = SessaoUsuario.getInstancia();
      try {
        desktop.encerrarSessao();
        boolean semSessao = !autz.temPermissao(Permissao.ACESSAR_CRM);
        desktop.setUsuario(new Usuario(1, "x", "h", PerfilUsuario.VENDEDOR));
        check(semSessao && autz.temPermissao(Permissao.ACESSAR_CRM) && !autz.temPermissao(Permissao.EXCLUIR_VEICULO),
            "TEMPERMISSAO_DO_DESKTOP_CONTINUA_USANDO_A_SESSAO");
      } finally {
        desktop.encerrarSessao();
      }
    }

    // ===== Contrato por HTTP de verdade (servidor JDK em loopback) =====
    {
      Ctx c = new Ctx();
      HttpServer srv = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
      srv.createContext("/metricas", ex -> {
        String token = SessaoWebService.extrairToken(ex.getRequestHeaders().getFirst("Authorization"),
            ex.getRequestHeaders().getFirst("Cookie"));
        int status;
        try {
          status = c.svc.autorizarMetricas(token).httpStatus();
        } catch (SQLException e) {
          status = 500;
        }
        byte[] corpo = ("{\"status\":" + status + "}").getBytes();
        ex.sendResponseHeaders(status, corpo.length);
        ex.getResponseBody().write(corpo);
        ex.close();
      });
      srv.start();
      try {
        HttpClient http = HttpClient.newHttpClient();
        String base = "http://127.0.0.1:" + srv.getAddress().getPort() + "/metricas";
        String tv = c.tokenVendedor();
        String ta = c.tokenAdmin();
        check(http(http, base, null, null) == 401, "HTTP_SEM_CREDENCIAL_401");
        check(http(http, base, "Bearer " + tokFalso, null) == 401, "HTTP_TOKEN_FALSO_401");
        check(http(http, base, "Bearer " + tv, null) == 403, "HTTP_VENDEDOR_403");
        check(http(http, base, "Bearer " + ta, null) == 200, "HTTP_ADMIN_BEARER_200");
        check(http(http, base, null, "ABM_SESSAO=" + ta) == 200, "HTTP_ADMIN_COOKIE_200");
        c.svc.logout(ta);
        check(http(http, base, "Bearer " + ta, null) == 401 && http(http, base, null, "ABM_SESSAO=" + ta) == 401,
            "HTTP_APOS_LOGOUT_401");
      } finally {
        srv.stop(0);
      }
    }

    System.out.println("D14_SESSAO_WEB_OK checks=" + passed);
  }

  static int http(HttpClient http, String url, String authorization, String cookie) throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).GET();
    if (authorization != null)
      b.header("Authorization", authorization);
    if (cookie != null)
      b.header("Cookie", cookie);
    return http.send(b.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
  }
}
