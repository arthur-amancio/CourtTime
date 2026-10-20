const fs = require('fs');
const path = require('path');
const { verificarAmbienteIsolado } = require('../seguranca-ambiente.cjs');

function carregarConfiguracao() {
  const baseUrl = process.env.COURTTIME_E2E_BASE_URL || 'http://127.0.0.1:18081';
  const marcadorInstancia = process.env.COURTTIME_E2E_INSTANCE_TOKEN;
  const emailAdmin = process.env.COURTTIME_E2E_ADMIN_EMAIL;
  verificarAmbienteIsolado({
    baseUrl,
    consentimento: process.env.COURTTIME_E2E_ACK_ISOLATED_DB,
    nomeBanco: process.env.COURTTIME_E2E_DATABASE_NAME,
    marcadorInstancia,
    emailAdmin,
  });

  const senha = process.env.COURTTIME_E2E_PASSWORD;
  if (!senha) throw new Error('Defina COURTTIME_E2E_PASSWORD somente no ambiente da execucao.');

  const evidencias = process.env.COURTTIME_E2E_EVIDENCE_DIR || '/tmp/courttime-e2e-evidence';
  fs.mkdirSync(evidencias, { recursive: true });
  return Object.freeze({
    baseUrl,
    marcadorInstancia,
    senha,
    evidencias,
    targetSha: process.env.COURTTIME_E2E_TARGET_SHA || 'nao-informado',
    chromiumExecutable: process.env.COURTTIME_E2E_CHROMIUM_PATH,
    usuarios: Object.freeze({
      admin: [emailAdmin, senha],
      alunoA: [`aluno.a.${marcadorInstancia}@e2e.invalid`, senha],
      alunoB: [`aluno.b.${marcadorInstancia}@e2e.invalid`, senha],
      secretaria: [`secretaria.${marcadorInstancia}@e2e.invalid`, senha],
      seguranca: [`seguranca.${marcadorInstancia}@e2e.invalid`, senha],
    }),
  });
}

function caminhoEvidencia(configuracao, nome) {
  return path.join(configuracao.evidencias, nome);
}

module.exports = { carregarConfiguracao, caminhoEvidencia };
