import autobotz.AutorizacaoService;
import autobotz.AuthService;
import autobotz.PerfilUsuario;
import autobotz.ProjetoDAO;
import autobotz.SessaoUsuario;
import autobotz.Usuario;
import autobotz.UsuarioDAO;
import autobotz.dao.ClienteDAO;
import autobotz.dao.OrdemServicoDAO;
import autobotz.dao.ServicoDAO;
import autobotz.dao.VeiculoDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.Cliente;
import autobotz.model.ItemOS;
import autobotz.model.OrdemServico;
import autobotz.model.Servico;
import autobotz.model.Veiculo;
import autobotz.model.Venda;
import autobotz.service.CRMService;
import autobotz.service.ServicoService;
import autobotz.util.ConexaoBanco;
import autobotz.util.CriptografiaDadosPessoais;
import autobotz.util.MigracaoDadosPessoais;
import autobotz.util.I18nUtils;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
public class CoreRegression {
  static void check(boolean b, String m) {
    if (!b)
      throw new AssertionError(m);
    System.out.println("PASS " + m);
  }
  static String capture(Run r) throws Exception {
    var out = new ByteArrayOutputStream();
    var old = System.out;
    try {
      System.setOut(new PrintStream(out));
      r.go();
    } finally {
      System.setOut(old);
    }
    return out.toString();
  }
  interface Run {
    void go() throws Exception;
  }
  static int count(String sql) throws Exception {
    try (var s = ConexaoBanco.getConexao().createStatement();
         var r = s.executeQuery(sql)) {
      r.next();
      return r.getInt(1);
    }
  }
  static String[] rawClientPii(int id) throws Exception {
    String sql = "SELECT nome, cpf, cpf_hash, telefone, email "
        + "FROM clientes WHERE id_cliente = ?";
    try (var stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
      stmt.setInt(1, id);
      try (var rs = stmt.executeQuery()) {
        if (!rs.next()) {
          throw new AssertionError("Cliente de regressão não encontrado");
        }
        return new String[] {
            rs.getString("nome"), rs.getString("cpf"), rs.getString("cpf_hash"),
            rs.getString("telefone"), rs.getString("email")
        };
      }
    }
  }
  public static void main(String[] args) throws Exception {
    check(System.getenv("AUTOBOTZ_DB_URL")
              .matches(".*autobotz_.*_test(?:[?].*)?"),
          "ISOLATED_TEST_DATABASE");
    CriptografiaDadosPessoais.validarConfiguracao();
    check(true, "PII_KEY_CONFIGURED");
    String cipherProbe = CriptografiaDadosPessoais.proteger("probe", "nome");
    check("probe".equals(CriptografiaDadosPessoais.revelar(cipherProbe, "nome")),
          "PII_CIPHER_ROUNDTRIP");
    try {
      CriptografiaDadosPessoais.revelar(cipherProbe, "cpf");
      throw new AssertionError("Campo cifrado foi aceito em outra coluna");
    } catch (java.sql.SQLException expected) {
      check(true, "PII_AAD_FIELD_BINDING");
    }
    String tamperedProbe = cipherProbe.substring(0, cipherProbe.length() - 1)
        + (cipherProbe.endsWith("A") ? "B" : "A");
    try {
      CriptografiaDadosPessoais.revelar(tamperedProbe, "nome");
      throw new AssertionError("Texto cifrado alterado foi aceito");
    } catch (java.sql.SQLException expected) {
      check(true, "PII_CIPHER_TAMPER_REJECTED");
    }
    check(ConexaoBanco.getConexao().isValid(2), "JDBC");
    var pt = I18nUtils.getBundle(Locale.forLanguageTag("pt-BR"));
    var en = I18nUtils.getBundle(Locale.US);
    check(pt.keySet().equals(en.keySet()),
          "BUNDLES_PARITY_" + pt.keySet().size());
    for (var k : pt.keySet())
      if (pt.getString(k).isEmpty() || en.getString(k).isEmpty())
        throw new AssertionError(k);
    System.out.println("DATE_PT=" +
                       I18nUtils.formatDate(LocalDate.of(2026, 9, 26),
                                            Locale.forLanguageTag("pt-BR")));
    System.out.println("DATE_EN=" + I18nUtils.formatDate(
                                        LocalDate.of(2026, 9, 26), Locale.US));
    System.out.println(
        "CURRENCY_PT=" +
        I18nUtils.formatCurrency(1234.56, Locale.forLanguageTag("pt-BR")));
    System.out.println("CURRENCY_EN=" +
                       I18nUtils.formatCurrency(1234.56, Locale.US));
    var auth = new AuthService(new UsuarioDAO());
    check(auth.cadastrar("audit_admin", "audit-only", PerfilUsuario.ADMIN),
          "CADASTRO");
    var admin = auth.autenticar("audit_admin", "audit-only");
    check(admin != null && auth.autenticar("audit_admin", "wrong") == null,
          "AUTH_VALID_INVALID");
    var rb = new AutorizacaoService(pt);
    check(!rb.temPermissao(AutorizacaoService.Permissao.ACESSAR_CRM),
          "CRM_DENY_NO_SESSION");
    SessaoUsuario.getInstancia().setUsuario(admin);
    check(rb.temPermissao(AutorizacaoService.Permissao.ACESSAR_CRM),
          "CRM_ALLOW_ADMIN");
    SessaoUsuario.getInstancia().setUsuario(
        new Usuario("seller", "unused", PerfilUsuario.VENDEDOR));
    check(rb.temPermissao(AutorizacaoService.Permissao.ACESSAR_CRM) &&
              !rb.temPermissao(AutorizacaoService.Permissao.CRIAR_PROJETOS),
          "RBAC_SELLER");
    SessaoUsuario.getInstancia().encerrarSessao();
    var cd = new ClienteDAO();
    cd.salvar(new Cliente("Audit Client", "AUDIT001", "000",
                          "audit@example.invalid"));
    cd.salvar(new Cliente("Audit Second", "AUDIT002", "000",
                          "audit@example.invalid"));
    int cid = cd.buscarPorCpf("AUDIT001").getId();
    int cid2 = cd.buscarPorCpf("AUDIT002").getId();
    check(cd.listar().size() == 2, "CLIENT_CREATE_READ");
    String[] rawPii = rawClientPii(cid);
    check(CriptografiaDadosPessoais.isProtegido(rawPii[0])
              && CriptografiaDadosPessoais.isProtegido(rawPii[1])
              && rawPii[2] != null && rawPii[2].matches("[0-9a-f]{64}")
              && CriptografiaDadosPessoais.isProtegido(rawPii[3])
              && CriptografiaDadosPessoais.isProtegido(rawPii[4]),
          "CLIENT_PII_ENCRYPTED_AT_REST");
    var decodedClient = cd.buscarPorCpf("AUDIT001");
    check(decodedClient != null && decodedClient.getNome().equals("Audit Client")
              && decodedClient.getEmail().equals("audit@example.invalid"),
          "CLIENT_PII_LOOKUP_AND_DECRYPT");
    String longUnicodeName = "🚘".repeat(100);
    cd.salvar(new Cliente(longUnicodeName, "LONG-UNICODE-001", "000",
                          "unicode@example.invalid"));
    var longUnicodeClient = cd.buscarPorCpf("LONG-UNICODE-001");
    String[] rawLongUnicode = rawClientPii(longUnicodeClient.getId());
    check(rawLongUnicode[0].length() > 512 && rawLongUnicode[0].length() <= 1024
              && longUnicodeClient.getNome().equals(longUnicodeName),
          "CLIENT_LONG_UTF8_CIPHERTEXT_FITS_SCHEMA");
    var vd = new VeiculoDAO();
    vd.cadastrar(new Veiculo("AUD0001", "Model", "Brand", 2026, 1000));
    vd.cadastrar(new Veiculo("AUD0002", "Model", "Brand", 2026, 2000));
    var vs = vd.listarTodos();
    int vid = vs.get(0).getId();
    var vendas = new VendaDAO();
    vendas.registrarVenda(new Venda(vid, cid, 1000));
    vendas.registrarVenda(new Venda(vs.get(1).getId(), cid, 2000));
    check(vendas.listarVendasPorCliente(cid).size() == 2, "TWO_SALES");
    try {
      vendas.registrarVenda(new Venda(vid, cid, 1000));
      throw new AssertionError("resale allowed");
    } catch (IllegalArgumentException ok) {
    }
    check(vendas.listarVendasPorCliente(cid).size() == 2, "RESALE_REJECTED");
    var sd = new ServicoDAO();
    var review = new Servico("Revisao", 100);
    var other = new Servico("Pintura", 50);
    sd.inserir(review);
    sd.inserir(other);
    var od = new OrdemServicoDAO();
    var os = new OrdemServico(cid, vid, LocalDate.now(), "ABERTA");
    od.inserirOrdem(os);
    od.inserirItem(new ItemOS(os.getId(), review.getId(), 2, 100));
    od.inserirItem(new ItemOS(os.getId(), other.getId(), 3, 50));
    var orderDetails = od.buscarItensDaOrdem(os.getId());
    check(od.buscarItensObjetos(os.getId()).size() == 2
              && orderDetails.size() == 2,
          "OS_TWO_ITEMS_JOIN");
    check(orderDetails.stream().allMatch(line ->
              line.contains("Audit Client") && !line.contains("enc:v1:")),
          "D14_WORKSHOP_DECRYPTS_CLIENT_NAME");
    check(od.calcularTotal(os.getId()) == 350, "OS_TOTAL_350");
    var service = new ServicoService(od, sd, vendas);
    var total = service.calcularTotalOS(os.getId());
    check(total.isGarantiaAtiva() && total.getTotalDesconto() == 200 &&
              total.getTotalMaoDeObra() == 150,
          "WARRANTY_200_PAY_150");
    var expired = service.calcularTotalOS(od.buscarItensObjetos(os.getId()),
                                          LocalDate.now().minusDays(366));
    check(!expired.isGarantiaAtiva() && expired.getTotalMaoDeObra() == 350,
          "WARRANTY_EXPIRED");
    for (var locale : List.of(Locale.forLanguageTag("pt-BR"), Locale.US)) {
      I18nUtils.setLocale(locale);
      String crm = capture(() -> new CRMService().exibirHistoricoCliente(cid));
      check(crm.contains(I18nUtils.getString("crm.quantidade") + "2") &&
                crm.contains(I18nUtils.formatCurrency(3000)),
            "CRM_" + locale);
      try {
        new CRMService().exibirHistoricoCliente(999999);
        throw new AssertionError();
      } catch (IllegalArgumentException ok) {
      }
    }
    check(cd.anonimizar(cid) && cd.anonimizar(cid2), "ANONYMIZE_MULTIPLE");
    check(!cd.buscarPorId(cid).isAtivo() &&
              vendas.listarVendasPorCliente(cid).size() == 2 &&
              od.buscarPorId(os.getId()).getClienteId() == cid,
          "ANONYMIZE_FK_PRESERVED");
    String[] rawAnonymousPii = rawClientPii(cid);
    check(CriptografiaDadosPessoais.isProtegido(rawAnonymousPii[0])
              && CriptografiaDadosPessoais.isProtegido(rawAnonymousPii[1])
              && rawAnonymousPii[3] == null && rawAnonymousPii[4] == null
              && cd.buscarPorId(cid).getNome().equals("ANÔNIMO"),
          "ANONYMIZATION_PII_PROTECTED");
    var pd = new ProjetoDAO(en);
    pd.criarProjeto("A", "OwnerA", "TeamA", "OPEN");
    pd.criarProjeto("B", "OwnerB", "TeamB", "CLOSED");
    pd.atualizarMembros("A", "Alice");
    pd.atualizarMembros("B", "Bob");
    String a = capture(() -> pd.consultarMembros("A"));
    String b = capture(() -> pd.consultarMembros("B"));
    check(a.contains("Alice") && !a.contains("Bob") && b.contains("Bob") &&
              !b.contains("Alice"),
          "PROJECT_MEMBERS_ISOLATED");
    pd.atualizarProjeto("A", "OwnerX", "TeamX", "DONE");
    pd.atualizarMembros("A", "Carol");
    check(count("SELECT COUNT(*) FROM projetos WHERE nome_projeto='A' AND " +
                "responsavel='OwnerX' AND equipe='TeamX' AND status='DONE'") ==
              1,
          "PROJECT_UPDATE");
    check(capture(() -> pd.consultarMembros("B")).contains("Bob"),
          "PROJECT_B_PRESERVED");
    check(I18nUtils
              .formatDate(LocalDate.of(2026, 9, 26),
                          Locale.forLanguageTag("pt-BR"))
              .startsWith("26/09/"),
          "DATE_PT_ORDER");
    check(I18nUtils.formatDate(LocalDate.of(2026, 9, 26), Locale.US)
              .startsWith("9/26/"),
          "DATE_EN_ORDER");
    check(I18nUtils.formatDate(null).isEmpty(), "DATE_NULL");
    I18nUtils.setLocale(Locale.US);
    String us = I18nUtils.formatDateTime(LocalDateTime.of(2026, 9, 26, 18, 30));
    I18nUtils.setLocale(Locale.forLanguageTag("pt-BR"));
    String br = I18nUtils.formatDateTime(LocalDateTime.of(2026, 9, 26, 18, 30));
    check(!us.equals(br) && us.contains("PM") && br.contains("18:30"),
          "DATETIME_LOCALE");
    var future = service.calcularTotalOS(od.buscarItensObjetos(os.getId()),
                                         LocalDate.now().plusDays(1));
    check(!future.isGarantiaAtiva() && future.getTotalMaoDeObra() == 350,
          "FUTURE_NO_WARRANTY");
    var invalidSale = new Venda(vid, cid, Double.NaN);
    try {
      vendas.registrarVenda(invalidSale);
      throw new AssertionError("NaN sale");
    } catch (IllegalArgumentException expected) {
    }
    invalidSale.setValorTotal(10);
    invalidSale.setDataVenda(LocalDate.now().plusDays(1));
    try {
      vendas.registrarVenda(invalidSale);
      throw new AssertionError("Future sale");
    } catch (IllegalArgumentException expected) {
    }
    String badDate = capture(
        ()
            -> new autobotz.ui.RelatorioMenu(new Scanner("1\ninvalid\n"))
                   .exibirMenu());
    check(badDate.contains(I18nUtils.getString("relatorio.erro")),
          "INVALID_DATE_HANDLED");
    check(cd.atualizar(new Cliente(cid, "Updated", "UPDATE-TEST", "000",
                                   "test@example.invalid", true)),
          "CLIENT_UPDATE");
    check(cd.buscarPorId(cid).isAtivo() &&
              vendas.listarVendasPorCliente(cid).size() == 2,
          "CLIENT_UPDATE_REFERENCES");
    String[] rawUpdatedPii = rawClientPii(cid);
    check(CriptografiaDadosPessoais.isProtegido(rawUpdatedPii[0])
              && CriptografiaDadosPessoais.isProtegido(rawUpdatedPii[1])
              && rawUpdatedPii[2].equals(
                      CriptografiaDadosPessoais.fingerprintCpf("UPDATE-TEST")),
          "CLIENT_UPDATE_ENCRYPTS_PII");
    // D14: valida diagnóstico somente leitura e migração de um registro legado.
    try (var stmt = ConexaoBanco.getConexao().prepareStatement(
        "INSERT INTO clientes (nome, cpf, cpf_hash, telefone, email, ativo) "
            + "VALUES (?, ?, NULL, ?, ?, TRUE)")) {
      stmt.setString(1, "Legacy Migration Client");
      stmt.setString(2, "MIGRATE-001");
      stmt.setString(3, "111");
      stmt.setString(4, "migration@example.invalid");
      stmt.executeUpdate();
    }
    var legacyClient = cd.buscarPorCpf("MIGRATE-001");
    check(legacyClient != null
              && legacyClient.getNome().equals("Legacy Migration Client"),
          "MIGRATION_LEGACY_LOOKUP");
    int legacyId = legacyClient.getId();
    String[] rawLegacy = rawClientPii(legacyId);
    check(!CriptografiaDadosPessoais.isProtegido(rawLegacy[0])
              && !CriptografiaDadosPessoais.isProtegido(rawLegacy[1])
              && rawLegacy[2] == null
              && !CriptografiaDadosPessoais.isProtegido(rawLegacy[3])
              && !CriptografiaDadosPessoais.isProtegido(rawLegacy[4]),
          "MIGRATION_LEGACY_PRECONDITION");

    String migrationCheck = capture(
        () -> MigracaoDadosPessoais.main(new String[] {"--check"}));
    check(migrationCheck.contains(
              "MIGRACAO_DADOS_PESSOAIS=DIAGNOSTICO_SEM_ALTERACOES"),
          "MIGRATION_CHECK_MODE");
    check(java.util.Arrays.equals(rawLegacy, rawClientPii(legacyId)),
          "MIGRATION_CHECK_READ_ONLY");

    String activeDatabase = ConexaoBanco.getConexao().getCatalog();
    String migrationApply = capture(
        () -> MigracaoDadosPessoais.main(new String[] {
            "--apply", "--confirm-db=" + activeDatabase
        }));
    check(migrationApply.contains("MIGRACAO_DADOS_PESSOAIS=APLICADA"),
          "MIGRATION_APPLY_MODE");

    String[] rawMigrated = rawClientPii(legacyId);
    check(CriptografiaDadosPessoais.isProtegido(rawMigrated[0])
              && CriptografiaDadosPessoais.isProtegido(rawMigrated[1])
              && rawMigrated[2] != null
              && rawMigrated[2].equals(
                  CriptografiaDadosPessoais.fingerprintCpf("MIGRATE-001"))
              && CriptografiaDadosPessoais.isProtegido(rawMigrated[3])
              && CriptografiaDadosPessoais.isProtegido(rawMigrated[4]),
          "MIGRATION_LEGACY_ENCRYPTED");
    var migratedClient = cd.buscarPorCpf("MIGRATE-001");
    check(migratedClient != null
              && migratedClient.getNome().equals("Legacy Migration Client")
              && migratedClient.getCpf().equals("MIGRATE-001")
              && migratedClient.getTelefone().equals("111")
              && migratedClient.getEmail().equals("migration@example.invalid"),
          "MIGRATION_LEGACY_LOOKUP_PRESERVED");

    String migrationRepeat = capture(
        () -> MigracaoDadosPessoais.main(new String[] {
            "--apply", "--confirm-db=" + activeDatabase
        }));
    check(migrationRepeat.contains("REGISTROS_PENDENTES=0"),
          "MIGRATION_IDEMPOTENT");
    System.out.println("CORE_REGRESSION_OK");
  }
}
