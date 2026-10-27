import autobotz.AuthService;
import autobotz.PerfilUsuario;
import autobotz.Usuario;
import autobotz.UsuarioDAO;
import autobotz.dao.SegurancaDAO;
import autobotz.model.EventoSeguranca;
import autobotz.service.MfaService;
import autobotz.service.ResultadoLogin;
import autobotz.service.SessaoWebService;
import autobotz.util.ConexaoBanco;
import autobotz.util.SenhaHash;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * D14 - integração JDBC REAL (UsuarioDAO + SegurancaDAO + migração 014).
 *
 * Exige MariaDB de TESTE com o schema/migração 014 aplicados. Recusa rodar se
 * AUTOBOTZ_DB_URL não indicar banco "autobotz_*_test". Só cria e apaga as próprias linhas
 * (usuários "d14it_*" e o que depende deles). Ordem de uso:
 *   AUTOBOTZ_DB_URL=jdbc:mariadb://localhost:3306/autobotz_d14_test AUTOBOTZ_DB_USER=... AUTOBOTZ_DB_PASSWORD=...
 *   java -cp build/classes:build/tests:<driver-mariadb>.jar:src D14JdbcIntegrationTest
 */
public class D14JdbcIntegrationTest {
  static int passed = 0;
  static final List<Integer> idsCriados = new ArrayList<>();

  static void check(boolean ok, String name) {
    if (!ok)
      throw new AssertionError("FAIL " + name);
    passed++;
    System.out.println("PASS " + name);
  }

  static int contar(String sql, Object... params) throws Exception {
    try (PreparedStatement st = ConexaoBanco.getConexao().prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++)
        st.setObject(i + 1, params[i]);
      try (ResultSet rs = st.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    String url = System.getenv("AUTOBOTZ_DB_URL");
    if (url == null || !url.matches(".*autobotz_.*_test(?:[?].*)?")) {
      System.err.println("RECUSADO: AUTOBOTZ_DB_URL deve apontar para um banco autobotz_*_test.");
      System.exit(2);
    }
    check(ConexaoBanco.getConexao().isValid(2), "JDBC_CONECTA");
    check(contar("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() "
        + "AND TABLE_NAME IN ('eventos_seguranca','mfa_desafios')") == 2, "TABELAS_D14_EXISTEM");
    check(contar("SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
        + "AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'senha_hash'") >= 120,
        "SENHA_HASH_COMPORTA_PBKDF2_(NAO_PODE_SER_CHAR_64)");

    String sufixo = Long.toString(System.currentTimeMillis(), 36);
    String nVend = "d14it_v_" + sufixo, nAdm = "d14it_a_" + sufixo, nLeg = "d14it_l_" + sufixo;
    var clock = new D14SegurancaTest.MutableClock();
    clock.now = Instant.now();
    var usuarios = new UsuarioDAO();
    var seg = new SegurancaDAO();
    var auth = new AuthService(usuarios, seg, clock, AuthService.PERFIS_COM_MFA);

    try {
      // cadastro grava PBKDF2 (prova que a coluna comporta) e busca por id/nome funcionam
      check(auth.cadastrar(nVend, "Senha-it-1", PerfilUsuario.VENDEDOR)
          && auth.cadastrar(nAdm, "Senha-it-2", PerfilUsuario.ADMIN), "CADASTRAR_GRAVA_HASH_LONGO");
      Usuario vend = usuarios.buscarPorNome(nVend), adm = usuarios.buscarPorNome(nAdm);
      idsCriados.add(vend.getId());
      idsCriados.add(adm.getId());
      check(vend.getSenhaHash().startsWith("pbkdf2_sha256$") && usuarios.buscarPorId(vend.getId()) != null
          && usuarios.buscarPorId(vend.getId()).getNomeUsuario().equals(nVend)
          && usuarios.buscarPorId(-1) == null, "BUSCAR_POR_ID");

      // hash legado migra no login (compare-and-set no UPDATE)
      usuarios.salvar(new Usuario(nLeg, SenhaHash.legadoSha256("legado-it"), PerfilUsuario.VENDEDOR));
      Usuario leg = usuarios.buscarPorNome(nLeg);
      idsCriados.add(leg.getId());
      check(!usuarios.atualizarSenhaHash(leg.getId(), "hash-que-nao-e-o-atual", "x"), "CAS_RECUSA_HASH_DESATUALIZADO");
      check(auth.iniciarLogin(nLeg, "legado-it").getStatus() == ResultadoLogin.Status.AUTENTICADO
          && usuarios.buscarPorNome(nLeg).getSenhaHash().startsWith("pbkdf2_sha256$")
          && auth.iniciarLogin(nLeg, "legado-it").getStatus() == ResultadoLogin.Status.AUTENTICADO,
          "LEGADO_MIGRA_NO_BANCO_E_CONTINUA_ENTRANDO");

      // bloqueio 5/10min com a consulta SQL real
      for (int i = 0; i < 4; i++)
        auth.iniciarLogin(nVend, "errada" + i);
      var quinta = auth.iniciarLogin(nVend, "errada4");
      check(quinta.getStatus() == ResultadoLogin.Status.BLOQUEADO && quinta.getSegundosRestantes() == 600,
          "BLOQUEIO_NA_QUINTA_FALHA_COM_600S");
      clock.advance(Duration.ofMinutes(3));
      var certa = auth.iniciarLogin(nVend, "Senha-it-1");
      check(certa.getStatus() == ResultadoLogin.Status.BLOQUEADO && certa.getSegundosRestantes() == 420,
          "BLOQUEADO_COM_SENHA_CERTA_E_420S");
      clock.advance(Duration.ofMinutes(7));
      check(auth.iniciarLogin(nVend, "Senha-it-1").getStatus() == ResultadoLogin.Status.AUTENTICADO,
          "LIBERA_APOS_10_MIN");
      check(seg.listarEventos(500).stream().anyMatch(e -> e.nomeUsuario().equalsIgnoreCase(nVend)
          && e.tipo().equals(EventoSeguranca.LOGIN_BLOQUEADO)), "TRILHA_TEM_LOGIN_BLOQUEADO");

      // MFA no banco: invalido, valido, reuso, expirado
      var l1 = auth.iniciarLogin(nAdm, "Senha-it-2");
      check(l1.getStatus() == ResultadoLogin.Status.MFA_NECESSARIA, "ADMIN_PEDE_MFA");
      var d = l1.getDesafio();
      String errado = d.codigoSimulado().equals("000000") ? "111111" : "000000";
      check(auth.concluirMfa(d.id(), errado).getMfaStatus() == MfaService.Status.INVALIDO, "MFA_INVALIDO_NO_BANCO");
      check(auth.concluirMfa(d.id(), d.codigoSimulado()).getStatus() == ResultadoLogin.Status.AUTENTICADO,
          "MFA_VALIDO_NO_BANCO");
      check(auth.concluirMfa(d.id(), d.codigoSimulado()).getMfaStatus() == MfaService.Status.JA_UTILIZADO,
          "MFA_NAO_REUTILIZA_NO_BANCO");
      check(contar("SELECT COUNT(*) FROM mfa_desafios WHERE id_desafio = ? AND consumido = TRUE AND tentativas = 2",
          d.id()) == 1, "MFA_LINHA_CONSUMIDA_COM_2_TENTATIVAS");
      var d2 = auth.iniciarLogin(nAdm, "Senha-it-2").getDesafio();
      clock.advance(MfaService.VALIDADE);
      check(auth.concluirMfa(d2.id(), d2.codigoSimulado()).getMfaStatus() == MfaService.Status.EXPIRADO,
          "MFA_EXPIRADO_NO_BANCO");

      // sessao web de ponta a ponta sobre o banco
      var web = new SessaoWebService(usuarios, seg, clock);
      String tv = web.iniciarLogin(nVend, "Senha-it-1").token();
      var w1 = web.iniciarLogin(nAdm, "Senha-it-2");
      String ta = web.concluirMfa(w1.desafio().id(), w1.desafio().codigoSimulado()).token();
      check(tv != null && ta != null && web.autorizarMetricas(null).httpStatus() == 401
          && web.autorizarMetricas(tv).httpStatus() == 403 && web.autorizarMetricas(ta).httpStatus() == 200,
          "WEB_401_403_200_NO_BANCO");
      web.logout(ta);
      check(web.autorizarMetricas(ta).httpStatus() == 401, "WEB_LOGOUT_NO_BANCO");
    } finally {
      limpar();
    }
    System.out.println("D14_JDBC_OK checks=" + passed);
  }

  /** Remove apenas o que este teste criou. */
  static void limpar() throws Exception {
    try (PreparedStatement a = ConexaoBanco.getConexao()
            .prepareStatement("DELETE FROM eventos_seguranca WHERE nome_usuario LIKE 'd14it\\_%' OR nome_usuario = ?");
         PreparedStatement b = ConexaoBanco.getConexao()
            .prepareStatement("DELETE FROM mfa_desafios WHERE id_usuario = ?");
         PreparedStatement c = ConexaoBanco.getConexao()
            .prepareStatement("DELETE FROM usuarios WHERE id_usuario = ? AND nome_usuario LIKE 'd14it\\_%'")) {
      for (int id : idsCriados) {
        a.setString(1, "id_usuario:" + id);
        a.executeUpdate();
        b.setInt(1, id);
        b.executeUpdate();
        c.setInt(1, id);
        c.executeUpdate();
      }
    }
  }
}
