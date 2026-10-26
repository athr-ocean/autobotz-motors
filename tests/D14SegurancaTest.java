import autobotz.AuthService;
import autobotz.AutorizacaoService;
import autobotz.PerfilUsuario;
import autobotz.SessaoUsuario;
import autobotz.Usuario;
import autobotz.UsuarioDAO;
import autobotz.dao.SegurancaRepositorio;
import autobotz.model.DesafioMfa;
import autobotz.model.EventoSeguranca;
import autobotz.service.MfaService;
import autobotz.service.ResultadoLogin;
import autobotz.service.SegurancaService;
import autobotz.util.I18nUtils;
import autobotz.util.SenhaHash;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Suíte D14 (login seguro, bloqueio, MFA, migração de hash, RBAC da trilha).
 * Não usa banco: relógio controlável + repositório em memória. A camada SQL
 * (SegurancaDAO/UsuarioDAO) precisa de teste de integração em banco _test.
 */
public class D14SegurancaTest {
  static int passed = 0;

  static void check(boolean ok, String name) {
    if (!ok)
      throw new AssertionError("FAIL " + name);
    passed++;
    System.out.println("PASS " + name);
  }

  // ---- doubles ------------------------------------------------------------
  static class MutableClock extends Clock {
    Instant now = Instant.parse("2026-10-26T12:00:00Z");
    void advance(Duration d) { now = now.plus(d); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId z) { return this; }
    @Override public Instant instant() { return now; }
  }

  static class FakeUsuarioDAO extends UsuarioDAO {
    final Map<Integer, Usuario> porId = new HashMap<>();
    int seq = 0;
    Usuario add(String nome, String hash, PerfilUsuario p) {
      Usuario u = new Usuario(++seq, nome, hash, p);
      porId.put(u.getId(), u);
      return u;
    }
    @Override public void salvar(Usuario u) {
      u.setId(++seq);
      porId.put(u.getId(), u);
    }
    @Override public Usuario buscarPorNome(String nome) {
      return porId.values().stream().filter(u -> u.getNomeUsuario().equalsIgnoreCase(nome))
          .findFirst().orElse(null);
    }
    @Override public Usuario buscarPorId(int id) { return porId.get(id); }
    @Override public boolean atualizarSenhaHash(int id, String atual, String novo) {
      Usuario u = porId.get(id);
      if (u == null || !u.getSenhaHash().equals(atual))
        return false;
      porId.put(id, new Usuario(id, u.getNomeUsuario(), novo, u.getPerfil()));
      return true;
    }
  }

  static class MemRepo implements SegurancaRepositorio {
    final List<EventoSeguranca> eventos = new ArrayList<>();
    final Map<Long, DesafioMfa> mfa = new HashMap<>();
    long seq = 0;
    @Override public void registrarEvento(EventoSeguranca e) { eventos.add(e); }
    @Override public int contarFalhasLogin(String nome, Instant inicio) {
      Instant ultimoOk = inicio;
      for (var e : eventos)
        if (e.nomeUsuario().equalsIgnoreCase(nome) && e.tipo().equals("LOGIN") && e.sucesso()
            && e.dataHora().isAfter(ultimoOk))
          ultimoOk = e.dataHora();
      int n = 0;
      for (var e : eventos)
        if (e.nomeUsuario().equalsIgnoreCase(nome) && e.tipo().equals("LOGIN") && !e.sucesso()
            && e.dataHora().isAfter(inicio) && e.dataHora().isAfter(ultimoOk))
          n++;
      return n;
    }
    @Override public List<EventoSeguranca> listarEventos(int limite) {
      List<EventoSeguranca> l = new ArrayList<>(eventos);
      java.util.Collections.reverse(l);
      return l.subList(0, Math.min(limite, l.size()));
    }
    @Override public long criarDesafioMfa(int u, String h, Instant c, Instant e, int max) {
      long id = ++seq;
      mfa.put(id, new DesafioMfa(id, u, h, e, 0, max, false));
      return id;
    }
    @Override public void invalidarDesafiosPendentes(int u) {
      mfa.replaceAll((k, d) -> d.idUsuario() == u && !d.consumido() ? com(d, d.tentativas(), true) : d);
    }
    @Override public DesafioMfa buscarDesafioMfa(long id) { return mfa.get(id); }
    @Override public boolean incrementarTentativaMfa(long id) {
      DesafioMfa d = mfa.get(id);
      if (d == null || d.consumido() || d.tentativas() >= d.maxTentativas())
        return false;
      mfa.put(id, com(d, d.tentativas() + 1, false));
      return true;
    }
    @Override public boolean consumirDesafioMfa(long id, Instant agora) {
      DesafioMfa d = mfa.get(id);
      if (d == null || d.consumido() || !d.expiraEm().isAfter(agora))
        return false;
      mfa.put(id, com(d, d.tentativas(), true));
      return true;
    }
    @Override public void invalidarDesafio(long id) {
      DesafioMfa d = mfa.get(id);
      if (d != null)
        mfa.put(id, com(d, d.tentativas(), true));
    }
    static DesafioMfa com(DesafioMfa d, int t, boolean consumido) {
      return new DesafioMfa(d.id(), d.idUsuario(), d.codigoHash(), d.expiraEm(), t,
          d.maxTentativas(), consumido);
    }
  }

  // ---- fixtures ------------------------------------------------------------
  static final String SENHA_ADMIN = "Adm1n-teste-ok";
  static final String SENHA_VEND = "Vend-teste-ok";
  static String hashAdmin;
  static String hashVend;

  static class Ctx {
    final MutableClock clock = new MutableClock();
    final FakeUsuarioDAO dao = new FakeUsuarioDAO();
    final MemRepo repo = new MemRepo();
    final AuthService auth = new AuthService(dao, repo, clock, AuthService.PERFIS_COM_MFA);
    Ctx() {
      dao.add("admin", hashAdmin, PerfilUsuario.ADMIN);
      dao.add("vendedor", hashVend, PerfilUsuario.VENDEDOR);
    }
    void falhar(String nome, int n) throws Exception {
      for (int i = 0; i < n; i++)
        if (auth.iniciarLogin(nome, "errada-" + i).getStatus() != ResultadoLogin.Status.NEGADO)
          throw new AssertionError("FAIL falha_esperada_" + nome + "_" + (i + 1));
    }
    boolean entra(String nome, String senha) throws Exception {
      return auth.iniciarLogin(nome, senha).getStatus() == ResultadoLogin.Status.AUTENTICADO;
    }
  }

  static void expect(Class<? extends Throwable> t, String name, Callable c) {
    try {
      c.run();
    } catch (Throwable e) {
      check(t.isInstance(e), name);
      return;
    }
    throw new AssertionError("FAIL " + name + " (nada lancado)");
  }
  interface Callable { void run() throws Exception; }

  public static void main(String[] args) throws Exception {
    hashAdmin = SenhaHash.gerar(SENHA_ADMIN);
    hashVend = SenhaHash.gerar(SENHA_VEND);

    // ===== Hash de senha =====
    check(hashAdmin.startsWith("pbkdf2_sha256$600000$"), "HASH_FORMATO_VERSIONADO");
    check(!SenhaHash.gerar("x", 1000).equals(SenhaHash.gerar("x", 1000)), "HASH_SAL_ALEATORIO");
    check(SenhaHash.verificar(SENHA_ADMIN, hashAdmin) && !SenhaHash.verificar("outra", hashAdmin),
        "HASH_VERIFICA_CERTO_E_ERRADO");
    check(!SenhaHash.verificar("x", "pbkdf2_sha256$abc$@@$@@") && !SenhaHash.verificar("x", "lixo")
        && !SenhaHash.verificar(null, hashAdmin), "HASH_MALFORMADO_NAO_QUEBRA");
    String leg = SenhaHash.legadoSha256("senha-antiga");
    check(leg.length() == 64 && SenhaHash.verificar("senha-antiga", leg)
        && !SenhaHash.verificar("outra", leg) && SenhaHash.ehLegado(leg), "HASH_LEGADO_AINDA_VERIFICA");
    check(SenhaHash.precisaAtualizar(leg) && SenhaHash.precisaAtualizar(SenhaHash.gerar("x", 1000))
        && !SenhaHash.precisaAtualizar(hashAdmin), "HASH_PRECISA_ATUALIZAR");

    // ===== Bloqueio: 4 falhas, 5 falhas, janela =====
    {
      Ctx c = new Ctx();
      c.falhar("vendedor", 4);
      check(c.entra("vendedor", SENHA_VEND), "QUATRO_FALHAS_NAO_BLOQUEIAM");
    }
    {
      Ctx c = new Ctx();
      c.falhar("vendedor", 5);
      check(!c.entra("vendedor", SENHA_VEND), "CINCO_FALHAS_BLOQUEIAM_MESMO_COM_SENHA_CERTA");
      check(c.repo.eventos.stream().anyMatch(e -> e.tipo().equals("LOGIN_BLOQUEADO")),
          "BLOQUEIO_REGISTRADO_NA_TRILHA");
      c.clock.advance(Duration.ofMinutes(9).plusSeconds(59));
      check(!c.entra("vendedor", SENHA_VEND), "AINDA_BLOQUEADO_AOS_9M59");
      c.clock.advance(Duration.ofSeconds(2));
      check(c.entra("vendedor", SENHA_VEND), "JANELA_EXPIRA_APOS_10_MIN");
    }
    {
      Ctx c = new Ctx(); // janela movel
      c.falhar("vendedor", 3);
      c.clock.advance(Duration.ofMinutes(9));
      c.falhar("vendedor", 2);
      check(!c.entra("vendedor", SENHA_VEND), "JANELA_MOVEL_BLOQUEIA_3_ANTIGAS_MAIS_2");
      c.clock.advance(Duration.ofMinutes(1).plusSeconds(1)); // 3 antigas saem da janela
      check(c.entra("vendedor", SENHA_VEND), "JANELA_MOVEL_LIBERA_QUANDO_ANTIGAS_SAEM");
    }
    {
      Ctx c = new Ctx(); // tentativas bloqueadas nao prorrogam o bloqueio
      c.falhar("vendedor", 5);
      for (int i = 0; i < 4; i++) {
        c.clock.advance(Duration.ofMinutes(2));
        c.entra("vendedor", "tentando-durante-bloqueio");
      }
      c.clock.advance(Duration.ofMinutes(2+1)); // 11 min apos as 5 falhas
      check(c.entra("vendedor", SENHA_VEND), "TENTATIVA_BLOQUEADA_NAO_PRORROGA_BLOQUEIO");
    }
    {
      Ctx c = new Ctx(); // acerto zera a contagem
      c.falhar("vendedor", 4);
      check(c.entra("vendedor", SENHA_VEND), "ACERTO_ANTES_DO_LIMITE");
      c.clock.advance(Duration.ofSeconds(1));
      c.falhar("vendedor", 4);
      check(c.entra("vendedor", SENHA_VEND), "ACERTO_ZERA_CONTAGEM_ANTERIOR");
    }

    // ===== Anti-enumeracao =====
    {
      Ctx c = new Ctx();
      ResultadoLogin inexistente = c.auth.iniciarLogin("fantasma", "qualquer");
      ResultadoLogin senhaErrada = c.auth.iniciarLogin("vendedor", "qualquer");
      check(inexistente.getStatus() == senhaErrada.getStatus()
          && inexistente.getUsuario() == null && senhaErrada.getUsuario() == null
          && inexistente.getDesafio() == null && senhaErrada.getDesafio() == null
          && inexistente.getMfaStatus() == null && senhaErrada.getMfaStatus() == null,
          "RESULTADO_IGUAL_INEXISTENTE_E_SENHA_ERRADA");
      c.falhar("vendedor", 4); // 1 falha acima + 4 = 5 falhas no vendedor
      ResultadoLogin bloqueado = c.auth.iniciarLogin("vendedor", SENHA_VEND);
      check(bloqueado.getStatus() == ResultadoLogin.Status.NEGADO && bloqueado.getMfaStatus() == null
          && bloqueado.getUsuario() == null, "RESULTADO_BLOQUEADO_INDISTINGUIVEL");
      for (int i = 0; i < 4; i++) c.auth.iniciarLogin("fantasma", "x" + i);
      var fant = c.auth.iniciarLogin("fantasma", "x");
      check(fant.getStatus() == ResultadoLogin.Status.NEGADO
          && c.repo.eventos.stream().filter(e -> e.nomeUsuario().equals("fantasma")
              && e.tipo().equals("LOGIN_BLOQUEADO")).count() == 1,
          "USUARIO_INEXISTENTE_TAMBEM_BLOQUEIA");
      Ctx d = new Ctx();
      d.falhar("ADMIN", 3);
      d.falhar(" admin ", 2);
      check(!d.entra("Admin", SENHA_ADMIN), "CAIXA_E_ESPACOS_COMPARTILHAM_CONTADOR");
      check(!d.auth.iniciarLogin(null, "x").getStatus().equals(ResultadoLogin.Status.AUTENTICADO)
          && !d.auth.iniciarLogin("", null).getStatus().equals(ResultadoLogin.Status.AUTENTICADO),
          "ENTRADA_NULA_OU_VAZIA_NEGADA");
    }

    // ===== MFA =====
    {
      Ctx c = new Ctx();
      var r = c.auth.iniciarLogin("vendedor", SENHA_VEND);
      check(r.getStatus() == ResultadoLogin.Status.AUTENTICADO && r.getDesafio() == null,
          "VENDEDOR_NAO_EXIGE_MFA");
      r = c.auth.iniciarLogin("admin", SENHA_ADMIN);
      check(r.getStatus() == ResultadoLogin.Status.MFA_NECESSARIA && r.getUsuario() == null
          && r.getDesafio() != null, "ADMIN_EXIGE_MFA_E_NAO_LIBERA_USUARIO_SEM_ELE");
      var d = r.getDesafio();
      check(d.codigoSimulado().matches("\\d{6}"), "MFA_CODIGO_6_DIGITOS");
      check(c.repo.mfa.values().stream().noneMatch(x -> x.codigoHash().contains(d.codigoSimulado())),
          "MFA_CODIGO_NAO_FICA_EM_CLARO_NO_REPOSITORIO");
      String errado = d.codigoSimulado().equals("000000") ? "111111" : "000000";
      var f = c.auth.concluirMfa(d.id(), errado);
      check(f.getStatus() == ResultadoLogin.Status.NEGADO && f.getMfaStatus() == MfaService.Status.INVALIDO
          && f.getUsuario() == null, "MFA_CODIGO_INVALIDO_NEGA");
      var ok = c.auth.concluirMfa(d.id(), d.codigoSimulado());
      check(ok.getStatus() == ResultadoLogin.Status.AUTENTICADO && ok.getUsuario().getNomeUsuario().equals("admin"),
          "MFA_CODIGO_VALIDO_LIBERA_USUARIO");
      var reuso = c.auth.concluirMfa(d.id(), d.codigoSimulado());
      check(reuso.getStatus() == ResultadoLogin.Status.NEGADO
          && reuso.getMfaStatus() == MfaService.Status.JA_UTILIZADO, "MFA_CODIGO_NAO_PODE_SER_REUTILIZADO");
      check(c.auth.concluirMfa(999, "123456").getMfaStatus() == MfaService.Status.INEXISTENTE,
          "MFA_DESAFIO_INEXISTENTE");
    }
    {
      Ctx c = new Ctx(); // expiracao
      var d = c.auth.iniciarLogin("admin", SENHA_ADMIN).getDesafio();
      c.clock.advance(MfaService.VALIDADE.minusSeconds(1));
      var antes = c.auth.concluirMfa(d.id(), d.codigoSimulado());
      check(antes.getStatus() == ResultadoLogin.Status.AUTENTICADO, "MFA_VALIDO_ATE_O_ULTIMO_SEGUNDO");
      var d2 = c.auth.iniciarLogin("admin", SENHA_ADMIN).getDesafio();
      c.clock.advance(MfaService.VALIDADE);
      var exp = c.auth.concluirMfa(d2.id(), d2.codigoSimulado());
      check(exp.getMfaStatus() == MfaService.Status.EXPIRADO && exp.getUsuario() == null,
          "MFA_CODIGO_EXPIRADO_RECUSADO_MESMO_CORRETO");
    }
    {
      Ctx c = new Ctx(); // tentativas
      var d = c.auth.iniciarLogin("admin", SENHA_ADMIN).getDesafio();
      String errado = d.codigoSimulado().equals("000000") ? "111111" : "000000";
      check(c.auth.concluirMfa(d.id(), errado).getMfaStatus() == MfaService.Status.INVALIDO, "MFA_TENTATIVA_1");
      check(c.auth.concluirMfa(d.id(), errado).getMfaStatus() == MfaService.Status.INVALIDO, "MFA_TENTATIVA_2");
      check(c.auth.concluirMfa(d.id(), errado).getMfaStatus() == MfaService.Status.ESGOTADO, "MFA_TENTATIVA_3_ESGOTA");
      check(c.auth.concluirMfa(d.id(), d.codigoSimulado()).getUsuario() == null,
          "MFA_CODIGO_CERTO_APOS_ESGOTAR_RECUSADO");
      var e = c.auth.iniciarLogin("admin", SENHA_ADMIN).getDesafio();
      c.auth.concluirMfa(e.id(), errado);
      c.auth.concluirMfa(e.id(), errado);
      check(c.auth.concluirMfa(e.id(), e.codigoSimulado()).getUsuario() != null,
          "MFA_ACERTA_NA_ULTIMA_TENTATIVA");
    }
    {
      Ctx c = new Ctx(); // novo desafio invalida o anterior
      var a = c.auth.iniciarLogin("admin", SENHA_ADMIN).getDesafio();
      var b = c.auth.iniciarLogin("admin", SENHA_ADMIN).getDesafio();
      check(c.auth.concluirMfa(a.id(), a.codigoSimulado()).getUsuario() == null
          && c.auth.concluirMfa(b.id(), b.codigoSimulado()).getUsuario() != null,
          "MFA_NOVO_DESAFIO_INVALIDA_ANTERIOR");
    }
    {
      Ctx c = new Ctx(); // desafio de um usuario nao serve com codigo de outro
      var a = c.auth.iniciarLogin("admin", SENHA_ADMIN).getDesafio();
      String outro = a.codigoSimulado().equals("123456") ? "654321" : "123456";
      check(c.auth.concluirMfa(a.id(), outro).getUsuario() == null, "MFA_CODIGO_DE_OUTRO_DESAFIO_NAO_VALE");
    }

    // ===== Migracao de hash legado =====
    {
      Ctx c = new Ctx();
      Usuario u = c.dao.add("antigo", SenhaHash.legadoSha256("senha-antiga"), PerfilUsuario.VENDEDOR);
      check(!c.entra("antigo", "senha-errada") && SenhaHash.ehLegado(c.dao.buscarPorId(u.getId()).getSenhaHash()),
          "LEGADO_SENHA_ERRADA_NAO_MIGRA");
      check(c.entra("antigo", "senha-antiga"), "LEGADO_LOGIN_CONTINUA_FUNCIONANDO");
      String novo = c.dao.buscarPorId(u.getId()).getSenhaHash();
      check(novo.startsWith("pbkdf2_sha256$") && !SenhaHash.ehLegado(novo), "LEGADO_HASH_MIGRADO_NO_LOGIN");
      check(c.entra("antigo", "senha-antiga") && !c.entra("antigo", "senha-errada"), "LEGADO_APOS_MIGRACAO_LOGIN_OK");
      check(c.repo.eventos.stream().anyMatch(e -> e.tipo().equals("HASH_MIGRADO") && e.sucesso()),
          "LEGADO_MIGRACAO_NA_TRILHA");
    }
    {
      Ctx c = new Ctx(); // cadastrar usa formato novo
      check(c.auth.cadastrar("novo", "Senha-nova-1", PerfilUsuario.VENDEDOR)
          && !c.auth.cadastrar("novo", "outra", PerfilUsuario.VENDEDOR), "CADASTRAR_E_DUPLICADO");
      check(c.dao.buscarPorNome("novo").getSenhaHash().startsWith("pbkdf2_sha256$")
          && c.entra("novo", "Senha-nova-1"), "CADASTRAR_GRAVA_PBKDF2");
      check(c.auth.autenticar("vendedor", SENHA_VEND) != null && c.auth.autenticar("vendedor", "x") == null
          && c.auth.autenticar("admin", SENHA_ADMIN) != null, "AUTENTICAR_PRIMEIRO_FATOR_COMPATIVEL");
    }

    // ===== Trilha sem segredos / sem origem inventada =====
    {
      Ctx c = new Ctx();
      c.falhar("vendedor", 2);
      c.auth.iniciarLogin("fantasma", "SegredoDoFantasma1");
      c.entra("vendedor", SENHA_VEND);
      var d = c.auth.iniciarLogin("admin", SENHA_ADMIN).getDesafio();
      c.auth.concluirMfa(d.id(), "999999");
      c.auth.concluirMfa(d.id(), d.codigoSimulado());
      boolean vaza = false;
      for (var e : c.repo.eventos) {
        String t = e.toString();
        for (String seg : new String[] {SENHA_ADMIN, SENHA_VEND, "SegredoDoFantasma1", "errada-0",
            d.codigoSimulado(), hashAdmin, hashVend})
          if (t.contains(seg))
            vaza = true;
      }
      check(!vaza, "TRILHA_NAO_CONTEM_SENHA_CODIGO_OU_HASH");
      check(!d.toString().contains(d.codigoSimulado()), "DESAFIO_TOSTRING_NAO_EXPOE_CODIGO");
      check(c.repo.eventos.stream().allMatch(e -> e.origem() == null), "ORIGEM_NAO_INVENTADA");
      check(c.repo.eventos.stream().anyMatch(e -> e.tipo().equals("MFA_EMITIDO"))
          && c.repo.eventos.stream().anyMatch(e -> e.tipo().equals("MFA_VERIFICACAO") && !e.sucesso())
          && c.repo.eventos.stream().anyMatch(e -> e.tipo().equals("MFA_VERIFICACAO") && e.sucesso()),
          "TRILHA_REGISTRA_MFA");
    }

    // ===== RBAC da consulta =====
    {
      Ctx c = new Ctx();
      c.falhar("vendedor", 1);
      var autz = new AutorizacaoService(null);
      var svc = new SegurancaService(c.repo, autz);
      var sessao = SessaoUsuario.getInstancia();
      try {
        sessao.encerrarSessao();
        expect(SecurityException.class, "EVENTOS_NEGADOS_SEM_SESSAO", () -> svc.consultarEventos(10));
        sessao.setUsuario(c.dao.buscarPorNome("vendedor"));
        expect(SecurityException.class, "EVENTOS_NEGADOS_PARA_VENDEDOR", () -> svc.consultarEventos(10));
        sessao.setUsuario(c.dao.buscarPorNome("admin"));
        check(svc.consultarEventos(10).size() == 1, "EVENTOS_PERMITIDOS_PARA_ADMIN");
      } finally {
        sessao.encerrarSessao();
      }
    }

    // ===== i18n =====
    {
      var pt = I18nUtils.getBundle(Locale.forLanguageTag("pt-BR"));
      var en = I18nUtils.getBundle(Locale.US);
      check(pt.keySet().equals(en.keySet()), "BUNDLES_PARIDADE_PT_EN");
      String[] chaves = {"login.mfa_titulo", "login.mfa_demo", "login.mfa_codigo", "login.mfa_invalido",
          "login.mfa_reiniciar", "gui.mfa_title", "gui.mfa_help", "gui.mfa_demo_code", "gui.mfa_code",
          "gui.mfa_verify", "gui.mfa_invalid", "gui.mfa_restart"};
      boolean todas = true;
      for (String k : chaves)
        todas &= pt.containsKey(k) && en.containsKey(k) && !pt.getString(k).isBlank()
            && !en.getString(k).isBlank();
      check(todas, "CHAVES_D14_PRESENTES_E_NAO_VAZIAS");
      check(String.format(pt.getString("gui.mfa_demo_code"), "123456").contains("123456")
          && String.format(en.getString("gui.mfa_demo_code"), "123456").contains("123456"),
          "MFA_ROTULO_DEMO_FORMATA_CODIGO");
      for (String arq : new String[] {"src/messages_pt_BR.properties", "src/messages_en_US.properties"}) {
        Set<String> vistas = new HashSet<>();
        List<String> duplicadas = new ArrayList<>();
        for (String linha : Files.readAllLines(Path.of(arq))) {
          String l = linha.strip();
          if (l.isEmpty() || l.startsWith("#") || !l.contains("="))
            continue;
          if (!vistas.add(l.substring(0, l.indexOf('='))))
            duplicadas.add(l.substring(0, l.indexOf('=')));
        }
        check(duplicadas.isEmpty(), "SEM_CHAVE_DUPLICADA_" + arq + " " + duplicadas);
      }
    }
    System.out.println("D14_OK checks=" + passed);
  }
}
