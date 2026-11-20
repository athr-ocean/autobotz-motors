const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('portal-treinamento/public/app.js', 'utf8')
  .replace(/iniciarDashboard\(\);\s*iniciar\(\);\s*$/, '');

function ambiente(fetch) {
  const elements = new Map();
  const document = { getElementById(id) {
    if (!elements.has(id)) elements.set(id, {
      hidden: false, textContent: '', value: '',
      replaceChildren() {}, addEventListener() {}
    });
    return elements.get(id);
  }};
  const context = vm.createContext({ document, fetch, URLSearchParams });
  vm.runInContext(source, context);
  document.getElementById('inicio').value = '2026-11-01';
  document.getElementById('fim').value = '2026-11-19';
  vm.runInContext('atualizarAutenticacao(false)', context);
  return { context, elements, run: code => vm.runInContext(code, context) };
}

(async () => {
  for (const status of [200, 401, 403, 500]) {
    let requested;
    const env = ambiente(async url => {
      requested = url;
      return { ok: status === 200, status };
    });
    await env.run('restaurarSessao()');
    assert.equal(requested, '/api/treinamentos/metricas?inicio=2026-11-01T00%3A00%3A00&fim=2026-11-20T00%3A00%3A00');
    const authenticated = status === 200 || status === 403;
    assert.equal(env.elements.get('login-form').hidden, authenticated);
    assert.equal(env.elements.get('dashboard').hidden, !authenticated);
    assert.equal(env.elements.get('sair').hidden, !authenticated);
    if (status === 401) assert.equal(env.elements.get('auth-status')?.textContent || '', '');
    if (status === 403) assert.match(env.elements.get('auth-status').textContent, /exigem perfil ADMIN/);
    if (status === 500) assert.match(env.elements.get('auth-status').textContent, /Não foi possível verificar/);
    console.log('PASS restauração HTTP ' + status);
  }
  const failed = ambiente(async () => { throw new Error('offline'); });
  await failed.run('restaurarSessao()');
  assert.equal(failed.elements.get('dashboard').hidden, true);
  assert.match(failed.elements.get('auth-status').textContent, /Não foi possível verificar/);
  console.log('PASS falha de rede não simula sessão autenticada');

  for (const status of [200, 403, 500]) {
    let resolve;
    const env = ambiente(() => new Promise(done => { resolve = done; }));
    const pending = env.run('restaurarSessao()');
    env.run('atualizarAutenticacao(false); statusAuth("Sessão encerrada.")');
    resolve({ ok: status === 200, status });
    await pending;
    assert.equal(env.elements.get('dashboard').hidden, true);
    assert.equal(env.elements.get('auth-status').textContent, 'Sessão encerrada.');
  }
  console.log('PASS resposta atrasada não desfaz logout');
  console.log('D4_SESSAO_INTERFACE_OK');
})().catch(error => { console.error(error); process.exitCode = 1; });
