// Protecao preventiva para testes que CRIAM e ALTERAM dados via interface.
// O marcador de instancia deve existir tanto no banco efemero quanto na pagina
// autenticada. Essa segunda verificacao e feita pelo helper de autenticacao.
function verificarAmbienteIsolado({ baseUrl, consentimento, nomeBanco, marcadorInstancia, emailAdmin }) {
  if (consentimento !== 'YES') {
    throw new Error(
      'Execucao bloqueada. Defina COURTTIME_E2E_ACK_ISOLATED_DB=YES somente apos verificar um banco descartavel e vazio.'
    );
  }
  if (nomeBanco !== 'courttime_e2e'
      && !/^courttime_e2e_p0p1(?:_run[2-9][0-9]*)?$/.test(nomeBanco || '')) {
    throw new Error(
      'Execucao bloqueada. COURTTIME_E2E_DATABASE_NAME deve identificar um banco E2E dedicado (nunca courttime_demo ou courttime_test).'
    );
  }

  if (!/^[a-z0-9]{8,32}$/.test(marcadorInstancia || '')) {
    throw new Error(
      'Execucao bloqueada. COURTTIME_E2E_INSTANCE_TOKEN deve conter de 8 a 32 caracteres minusculos ou numeros.'
    );
  }
  const emailEsperado = `admin.${marcadorInstancia}@e2e.invalid`;
  if (emailAdmin !== emailEsperado) {
    throw new Error(
      `Execucao bloqueada. COURTTIME_E2E_ADMIN_EMAIL deve ser exatamente ${emailEsperado}.`
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
