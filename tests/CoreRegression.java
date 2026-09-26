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
  public static void main(String[] args) throws Exception {
    check(System.getenv("AUTOBOTZ_DB_URL")
              .matches(".*autobotz_.*_test(?:[?].*)?"),
          "ISOLATED_TEST_DATABASE");
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
    check(od.buscarItensObjetos(os.getId()).size() == 2 &&
              od.buscarItensDaOrdem(os.getId()).size() == 2,
          "OS_TWO_ITEMS_JOIN");
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
    System.out.println("CORE_REGRESSION_OK");
  }
}
