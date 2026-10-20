const { test } = require('node:test');
const assert = require('node:assert/strict');
const { verificarAmbienteIsolado } = require('./seguranca-ambiente.cjs');

const correto = {
  baseUrl: 'http://127.0.0.1:18081',
  consentimento: 'YES',
  nomeBanco: 'courttime_e2e',
  marcadorInstancia: 'qa8c00b35',
  emailAdmin: 'admin.qa8c00b35@e2e.invalid'
};

test('recusa execucao sem confirmacao explicita', () => {
  assert.throws(() => verificarAmbienteIsolado({ ...correto, consentimento: undefined }), /bloqueada/);
  assert.throws(() => verificarAmbienteIsolado({ ...correto, consentimento: 'yes' }), /bloqueada/);
});
test('recusa banco demo e banco de testes da CI', () => {
  assert.throws(() => verificarAmbienteIsolado({ ...correto, nomeBanco: 'courttime_demo' }), /bloqueada/);
  assert.throws(() => verificarAmbienteIsolado({ ...correto, nomeBanco: 'courttime_test' }), /bloqueada/);
});
test('recusa marcador ausente, malformado ou inconsistente com o administrador', () => {
  assert.throws(() => verificarAmbienteIsolado({ ...correto, marcadorInstancia: undefined }), /bloqueada/);
  assert.throws(() => verificarAmbienteIsolado({ ...correto, marcadorInstancia: 'curto' }), /bloqueada/);
  assert.throws(() => verificarAmbienteIsolado({ ...correto, marcadorInstancia: 'QA_8C00B35' }), /bloqueada/);
  assert.throws(() => verificarAmbienteIsolado({ ...correto, emailAdmin: 'admin@e2e.invalid' }), /bloqueada/);
  assert.throws(() => verificarAmbienteIsolado({ ...correto, emailAdmin: 'admin.qa8c00b35@example.com' }), /bloqueada/);
});
test('recusa URLs remotas, publicas e com porta errada', () => {
  for (const baseUrl of [
    'https://example.com:18081/',
    'http://192.168.1.15:18081/',
    'http://127.0.0.1:8080/',
    'https://127.0.0.1:18081/',
    'http://127.0.0.1:18081/login',
    'http://usuario:senha@127.0.0.1:18081/',
    'nao-url'
  ]) assert.throws(() => verificarAmbienteIsolado({ ...correto, baseUrl }), /bloqueada/);
});
test('aceita apenas a instancia de loopback com confirmacao de banco efemero', () => {
  assert.equal(verificarAmbienteIsolado(correto), true);
  assert.equal(verificarAmbienteIsolado({ ...correto, baseUrl: 'http://localhost:18081/' }), true);
});
