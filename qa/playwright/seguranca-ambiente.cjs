// Protecao preventiva para testes que CRIAM e ALTERAM dados via interface.
// IMPORTANTE: esta validacao NAO consulta o banco real usado pelo servidor.
// Quem executa precisa confirmar a configuracao e o isolamento da instancia.
function verificarAmbienteIsolado({ baseUrl, consentimento, nomeBanco }) {
  if (consentimento !== 'YES') {
    throw new Error(
      'Execucao bloqueada. Defina COURTTIME_E2E_ACK_ISOLATED_DB=YES somente apos verificar um banco descartavel e vazio.'
    );
  }
  if (nomeBanco !== 'courttime_e2e') {
    throw new Error(
      'Execucao bloqueada. COURTTIME_E2E_DATABASE_NAME deve ser courttime_e2e (nunca courttime_demo ou courttime_test).'
    );
  }

  let alvo;
  try {
    alvo = new URL(baseUrl);
  } catch {
    throw new Error('Execucao bloqueada. COURTTIME_E2E_BASE_URL nao e uma URL valida.');
  }

  const hostsPermitidos = ['localhost', '127.0.0.1', '[::1]'];
  if (
    alvo.protocol !== 'http:' ||
    !hostsPermitidos.includes(alvo.hostname) ||
    alvo.port !== '18081' ||
    alvo.username ||
    alvo.password ||
    alvo.pathname !== '/' ||
    alvo.search ||
    alvo.hash
  ) {
    throw new Error(
      'Execucao bloqueada. Alvo deve ser exclusivamente http://127.0.0.1:18081/ ou equivalente loopback (porta 18081).'
    );
  }

  return true;
}

module.exports = { verificarAmbienteIsolado };
