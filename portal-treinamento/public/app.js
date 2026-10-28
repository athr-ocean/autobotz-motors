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
      const novoTipo = /^\\d+\\./.test(item[1]) ? "ol" : "ul";
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

iniciar();
