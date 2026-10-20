const assert = require('node:assert/strict');

async function autenticar(page, configuracao, perfil, { comprovarInstancia = false } = {}) {
  const credenciais = configuracao.usuarios[perfil];
  if (!credenciais) throw new Error(`Perfil de teste desconhecido: ${perfil}`);
  await page.goto(`${configuracao.baseUrl}/login`);
  await page.getByLabel(/e-?mail/i).fill(credenciais[0]);
  await page.getByLabel(/senha/i).fill(credenciais[1]);
  await Promise.all([
    page.waitForURL(url => url.pathname === '/inicio'),
    page.getByRole('button', { name: 'Entrar' }).click(),
  ]);
  const corpo = await page.locator('body').innerText();
  assert.match(corpo, /Perfil:/, `Login do perfil ${perfil} nao concluiu.`);
  if (comprovarInstancia) {
    assert.match(
      corpo,
      new RegExp(configuracao.marcadorInstancia, 'i'),
      'A aplicacao respondeu, mas nao exibiu o marcador da instancia efemera.'
    );
  }
}

async function criarSessao(browser, configuracao, perfil, opcoes = {}) {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1000 },
    ...opcoes,
  });
  const page = await context.newPage();
  await autenticar(page, configuracao, perfil);
  return { context, page };
}

async function postComCsrf(page, url, fields = {}, method = 'POST') {
  return page.evaluate(async ({ url, fields, method }) => {
    const token = document.querySelector('input[name="_csrf"]')?.value;
    if (!token) throw new Error('A pagina atual nao contem token CSRF.');
    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ ...fields, _csrf: token }),
      redirect: 'follow',
    });
    return { status: response.status, url: response.url, text: await response.text() };
  }, { url, fields, method });
}

async function postSemCsrf(page, url, fields = {}) {
  return page.evaluate(async ({ url, fields }) => {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(fields),
      redirect: 'manual',
    });
    return { status: response.status, text: await response.text() };
  }, { url, fields });
}

async function selecionarHorario(page, horario) {
  assert.match(horario, /^([01]\d|2[0-3]):[0-5]\d$/, 'Horario deve permanecer no contrato HH:mm.');
  const select = page.locator('select[name="horarioInicio"]');
  if (await select.count()) {
    await select.selectOption(horario);
    return;
  }
  const radio = page.locator(`input[type="radio"][name="horarioInicio"][value="${horario}"]`);
  assert.equal(await radio.count(), 1, `Horario ${horario} nao encontrado no formulario.`);
  await radio.check();
}

async function listarHorariosDisponiveis(page) {
  const select = page.locator('select[name="horarioInicio"]');
  if (await select.count()) {
    return select.locator('option').evaluateAll(opcoes => opcoes.map(opcao => opcao.value).filter(Boolean));
  }
  return page.locator('input[name="horarioInicio"]').evaluateAll(controles => controles.map(controle => controle.value));
}

async function sairPorPost(page) {
  const formulario = page.locator('form[action="/logout"]');
  assert.equal(await formulario.count(), 1, 'Formulario de logout POST nao encontrado.');
  assert.equal((await formulario.getAttribute('method'))?.toLowerCase(), 'post');
  assert.equal(await formulario.locator('input[name="_csrf"]').count(), 1, 'Logout sem token CSRF.');
  await Promise.all([
    page.waitForURL(url => url.pathname === '/login'),
    formulario.getByRole('button', { name: /sair/i }).click(),
  ]);
}

module.exports = {
  autenticar,
  criarSessao,
  postComCsrf,
  postSemCsrf,
  selecionarHorario,
  listarHorariosDisponiveis,
  sairPorPost,
};
