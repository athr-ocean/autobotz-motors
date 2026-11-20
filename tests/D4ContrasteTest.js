const assert = require('node:assert/strict');
const fs = require('node:fs');

const css = fs.readFileSync('portal-treinamento/public/style.css', 'utf8');
const palettes = [...css.matchAll(/:root\{([^}]+)\}/g)].map(match =>
  Object.fromEntries([...match[1].matchAll(/--([a-z]+):(#(?:[0-9a-f]{6}|[0-9a-f]{3}))(?=[;}]|$)/gi)]
    .map(token => [token[1], token[2]])));
assert.equal(palettes.length, 2, 'paletas clara e escura disponíveis');

function luminance(hex) {
  const full = hex.length === 4 ? hex.slice(1).split('').map(char => char + char).join('') : hex.slice(1);
  const linear = full.match(/../g).map(pair => parseInt(pair, 16) / 255)
    .map(value => value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4);
  return linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722;
}

function contrast(first, second) {
  const values = [luminance(first), luminance(second)].sort((a, b) => a - b);
  return (values[1] + 0.05) / (values[0] + 0.05);
}

for (const [index, palette] of palettes.entries()) {
  const theme = index === 0 ? 'claro' : 'escuro';
  const colors = { ...palettes[0], ...palette };
  for (const [role, minimum] of [['fg', 4.5], ['link', 4.5], ['bd', 3], ['foco', 3]]) {
    const ratio = contrast(colors[role], colors.bg);
    assert.ok(ratio >= minimum, `${theme} ${role}: ${ratio.toFixed(2)} < ${minimum}`);
    console.log(`PASS ${theme} ${role} ${ratio.toFixed(2)}:1 (mínimo ${minimum}:1)`);
  }
  const heading = contrast('#fff', colors.topo);
  assert.ok(heading >= 4.5);
  console.log(`PASS ${theme} título no cabeçalho ${heading.toFixed(2)}:1`);
}
console.log('D4_CONTRASTE_OK');
