const ROTULOS = { GUIA: "Guias", VIDEO: "Vídeos", WEBINAR: "Webinars", FAQ: "Perguntas frequentes", FORUM: "Fórum" };
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
    t.textContent = m.conteudo;
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

iniciar();
