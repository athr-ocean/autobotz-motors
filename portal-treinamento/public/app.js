const ROTULOS = { GUIA: "Guias", VIDEO: "Vídeos", WEBINAR: "Webinars", FAQ: "Perguntas frequentes", FORUM: "Fórum", ROTEIRO: "Roteiros", COMUNICADO: "Comunicados" };
const BASE = "/api/treinamentos";
const $ = (id) => document.getElementById(id);
let materiais = [];

async function api(url, opcoes) {
  const r = await fetch(url, opcoes);
  const dados = await r.json().catch(() => null);
  if (!r.ok) throw new Error(dados?.erro?.mensagem || "O servidor não respondeu como esperado.");
  return dados;
}

// Falha ao registrar evento nunca deve atrapalhar a navegação.
function registrar(corpo) {
  api(BASE + "/eventos", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(corpo),
  }).catch(() => {});
}

// Renderização Markdown mínima e segura: cria elementos DOM e nunca interpreta HTML do material.
function textoEmLinha(texto) {
  const fragmento = document.createDocumentFragment();
  const padrao = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let inicio = 0;
  for (const match of texto.matchAll(padrao)) {
    if (match.index > inicio) fragmento.append(document.createTextNode(texto.slice(inicio, match.index)));
    const token = match[0];
    const elemento = token.startsWith("`") ? document.createElement("code") : document.createElement("strong");
    elemento.textContent = token.startsWith("`") ? token.slice(1, -1) : token.slice(2, -2);
    fragmento.append(elemento);
    inicio = match.index + token.length;
  }
  if (inicio < texto.length) fragmento.append(document.createTextNode(texto.slice(inicio)));
  return fragmento;
}

function renderizarMarkdown(markdown) {
  const fragmento = document.createDocumentFragment();
  let lista = null;
  let tipoLista = null;

  for (const linha of String(markdown).split(/\r?\n/)) {
    if (!linha.trim()) {
      lista = null;
      tipoLista = null;
      continue;
    }

    const item = linha.match(/^\s*([-*]|\d+\.)\s+(.+)$/);
    if (item) {
      const novoTipo = /^\d+\./.test(item[1]) ? "ol" : "ul";
      if (!lista || tipoLista !== novoTipo) {
        lista = document.createElement(novoTipo);
        fragmento.append(lista);
        tipoLista = novoTipo;
      }
      const li = document.createElement("li");
      li.append(textoEmLinha(item[2]));
      lista.append(li);
      continue;
    }

    lista = null;
    tipoLista = null;
    const titulo = linha.match(/^(#{1,3})\s+(.+)$/);
    if (titulo) {
      const nivel = Math.min(titulo[1].length + 2, 5);
      const h = document.createElement("h" + nivel);
      h.append(textoEmLinha(titulo[2]));
      fragmento.append(h);
    } else {
      const p = document.createElement("p");
      p.append(textoEmLinha(linha));
      fragmento.append(p);
    }
  }
  return fragmento;
}

function mostrar(tipo) {
  const itens = tipo ? materiais.filter((m) => m.tipo === tipo) : materiais;
  $("lista").replaceChildren();
  $("status").textContent = itens.length ? "" : "Ainda não há materiais publicados nesta categoria.";
  for (const m of itens) {
    const li = document.createElement("li");
    const b = document.createElement("button");
    b.type = "button";
    b.textContent = m.titulo;
    b.onclick = () => abrir(m.slug);
    li.append(b);
    $("lista").append(li);
  }
}

function montarFiltros() {
  const tipos = [...new Set(materiais.map((m) => m.tipo))];
  for (const [valor, rotulo] of [["", "Todos"], ...tipos.map((t) => [t, ROTULOS[t] || t])]) {
    const b = document.createElement("button");
    b.type = "button";
    b.textContent = rotulo;
    b.onclick = () => { $("detalhe").hidden = true; mostrar(valor); };
    $("filtros").append(b);
  }
}

async function abrir(slug) {
  try {
    const m = await api(BASE + "/materiais/" + encodeURIComponent(slug));
    const h = document.createElement("h2");
    h.textContent = m.titulo;
    h.tabIndex = -1;
    const t = document.createElement("div");
    t.className = "texto";
    t.append(renderizarMarkdown(m.conteudo));
    $("detalhe").replaceChildren(h, t);
    $("detalhe").hidden = false;
    h.focus();
    registrar({ tipoEvento: m.tipo === "VIDEO" ? "VIDEO_VIEW" : "MATERIAL_VIEW", materialSlug: m.slug });
  } catch (e) {
    $("status").textContent = "Não foi possível abrir o material. " + e.message;
  }
}

async function iniciar() {
  registrar({ tipoEvento: "PORTAL_OPEN" });
  try {
    materiais = (await api(BASE + "/materiais")).materiais;
    montarFiltros();
    mostrar("");
  } catch (e) {
    $("status").textContent = "Não foi possível carregar os materiais. Confira se o servidor do portal está ligado e tente de novo.";
  }
}



// A sessão web do D14 usa cookie HttpOnly. O browser não lê ou armazena o token.
const auth = { desafioId: null };
function statusAuth(mensagem) { $("auth-status").textContent = mensagem; }
function atualizarAutenticacao(autenticado) {
  $("login-form").hidden = autenticado;
  $("mfa-form").hidden = true;
  $("sair").hidden = !autenticado;
  $("dashboard").hidden = !autenticado;
  if (!autenticado) $("metricas-linhas").replaceChildren();
}
async function entrar(evento) {
  evento.preventDefault();
  statusAuth("Validando credenciais...");
  const usuario = $("usuario").value.trim();
  const senha = $("senha").value;
  try {
    const resposta = await api("/api/login", {
      method: "POST", headers: {"Content-Type": "application/json"},
      body: JSON.stringify({ usuario, senha })
    });
    $("senha").value = "";
    if (resposta.status === "MFA_NECESSARIA") {
      auth.desafioId = resposta.desafioId;
      $("login-form").hidden = true;
      $("mfa-form").hidden = false;
      $("codigo-demo").textContent =
        "Segundo fator SIMULADO, não enviado por e-mail/SMS. Código demonstrativo: "
        + (resposta.codigoDemonstracao || "indisponível");
      statusAuth("Informe o segundo fator de demonstração.");
    } else if (resposta.status === "AUTENTICADO") {
      atualizarAutenticacao(true);
      statusAuth("Sessão autenticada. As métricas só são acessíveis a ADMIN.");
    } else {
      throw new Error("Resposta inesperada no login.");
    }
  } catch (erro) { statusAuth(erro.message); }
}
async function confirmarMfa(evento) {
  evento.preventDefault();
  if (auth.desafioId == null) return;
  try {
    await api("/api/login/mfa", {
      method: "POST", headers: {"Content-Type": "application/json"},
      body: JSON.stringify({ desafioId: auth.desafioId, codigo: $("codigo-mfa").value.trim() })
    });
    auth.desafioId = null;
    $("codigo-mfa").value = "";
    $("codigo-demo").textContent = "";
    atualizarAutenticacao(true);
    statusAuth("Sessão autenticada. Consulte as métricas se o perfil for ADMIN.");
  } catch (erro) { statusAuth(erro.message); }
}
async function sair() {
  try { await api("/api/logout", {method: "POST"}); }
  catch (_) { /* A sessão local é descartada mesmo se a rede falhar. */ }
  auth.desafioId = null;
  $("usuario").value = "";
  $("senha").value = "";
  atualizarAutenticacao(false);
  statusAuth("Sessão encerrada. Faça login para consultar novamente.");
}
function diaSeguinte(iso) {
  const data = new Date(iso + "T00:00:00Z");
  data.setUTCDate(data.getUTCDate() + 1);
  return data.toISOString().slice(0, 10);
}
async function consultarMetricas(evento) {
  evento.preventDefault();
  $("metricas-linhas").replaceChildren();
  if ($("fim").value < $("inicio").value) {
    $("metricas-status").textContent = "O fim precisa ser igual ou posterior ao início.";
    return;
  }
  const inicio = $("inicio").value + "T00:00:00";
  const fim = diaSeguinte($("fim").value) + "T00:00:00";
  try {
    const dados = await api(BASE + "/metricas?" +
      new URLSearchParams({ inicio, fim }));
    const linhas = Array.isArray(dados.metricas) ? dados.metricas : [];
    for (const item of linhas) {
      const tr = document.createElement("tr");
      for (const valor of [item.tipoEvento, item.materialSlug || "—", item.total]) {
        const td = document.createElement("td");
        td.textContent = String(valor);
        tr.append(td);
      }
      $("metricas-linhas").append(tr);
    }
    $("metricas-status").textContent = linhas.length
      ? "Dados do backend: " + (dados.origem || "não identificada")
      : "Nenhum evento encontrado no período.";
  } catch (erro) {
    $("metricas-status").textContent = "Consulta indisponível: " + erro.message +
      " (permissão ADMIN necessária).";
  }
}
function iniciarDashboard() {
  const hoje = new Date();
  $("fim").value = [
    hoje.getFullYear(), String(hoje.getMonth() + 1).padStart(2, "0"),
    String(hoje.getDate()).padStart(2, "0")].join("-");
  const trinta = new Date(hoje);
  trinta.setDate(trinta.getDate() - 29);
  $("inicio").value = [
    trinta.getFullYear(), String(trinta.getMonth() + 1).padStart(2, "0"),
    String(trinta.getDate()).padStart(2, "0")].join("-");
  $("login-form").addEventListener("submit", entrar);
  $("mfa-form").addEventListener("submit", confirmarMfa);
  $("sair").addEventListener("click", sair);
  $("metricas-form").addEventListener("submit", consultarMetricas);
  atualizarAutenticacao(false);
}

iniciarDashboard();
iniciar();
