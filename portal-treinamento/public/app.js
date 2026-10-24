const TIPOS = [["", "Todos"], ["GUIA", "Guias"], ["VIDEO", "Vídeos"], ["WEBINAR", "Webinars"], ["FAQ", "Perguntas frequentes"], ["FORUM", "Fórum"]];
const $ = (id) => document.getElementById(id);

let sessao;
try {
  sessao = sessionStorage.getItem("sessaoId") || crypto.randomUUID();
  sessionStorage.setItem("sessaoId", sessao);
} catch (e) {
  sessao = String(Date.now());
}

async function api(url, opcoes) {
  const r = await fetch(url, opcoes);
  const dados = await r.json().catch(() => null);
  if (!r.ok) throw new Error(dados?.erro?.mensagem || "O servidor não respondeu como esperado.");
  return dados;
}

// Falha ao registrar evento nunca deve atrapalhar a navegação.
function registrar(tipoEvento, materialId = null) {
  api("/api/eventos", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ tipoEvento, materialId, sessaoId: sessao }),
  }).catch(() => {});
}

async function carregar(tipo) {
  $("status").textContent = "Carregando materiais…";
  $("lista").replaceChildren();
  try {
    const itens = await api("/api/catalogo" + (tipo ? "?tipo=" + encodeURIComponent(tipo) : ""));
    $("status").textContent = itens.length ? "" : "Ainda não há materiais nesta categoria.";
    for (const m of itens) {
      const li = document.createElement("li");
      const b = document.createElement("button");
      b.type = "button";
      b.textContent = m.titulo + (m.simulado ? " (exemplo)" : "");
      b.onclick = () => abrir(m.id);
      li.append(b);
      $("lista").append(li);
    }
  } catch (e) {
    $("status").textContent = "Não foi possível carregar os materiais. Confira se o servidor do portal está ligado e tente de novo.";
  }
}

async function abrir(id) {
  try {
    const m = await api("/api/catalogo/" + encodeURIComponent(id));
    const d = $("detalhe");
    const h = document.createElement("h2");
    h.textContent = m.titulo;
    h.tabIndex = -1;
    const p = document.createElement("p");
    p.textContent = m.descricao;
    d.replaceChildren(h, p);
    if (m.simulado) {
      const s = document.createElement("p");
      s.className = "selo";
      s.textContent = "Conteúdo de exemplo, ainda não é o material oficial.";
      d.append(s);
    }
    d.hidden = false;
    h.focus();
    registrar(m.tipo === "VIDEO" ? "VIDEO_VIEW" : "MATERIAL_VIEW", m.id);
  } catch (e) {
    $("status").textContent = "Não foi possível abrir o material. " + e.message;
  }
}

for (const [valor, rotulo] of TIPOS) {
  const b = document.createElement("button");
  b.type = "button";
  b.textContent = rotulo;
  b.onclick = () => { $("detalhe").hidden = true; carregar(valor); };
  $("filtros").append(b);
}

registrar("PORTAL_OPEN");
carregar("");
