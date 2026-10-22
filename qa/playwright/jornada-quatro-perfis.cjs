const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('node:child_process');

const { carregarConfiguracao } = require('./suporte/configuracao.cjs');
const {
  autenticar,
  postComCsrf,
  postSemCsrf,
  selecionarHorario,
  listarHorariosDisponiveis,
} = require('./suporte/sessoes.cjs');

// Interrompe antes de gerar evidencias, autenticar ou modificar o banco.
const configuracao = carregarConfiguracao();
const BASE = configuracao.baseUrl;
const EVIDENCE = configuracao.evidencias;
const TARGET_SHA = configuracao.targetSha;
const CHROMIUM_EXECUTABLE = configuracao.chromiumExecutable;
const PASSWORD = configuracao.senha;
const users = configuracao.usuarios;

const results = [];
function pass(name, detail = '') {
  results.push({ status: 'PASS', name, detail });
  process.stdout.write(`PASS | ${name}${detail ? ` | ${detail}` : ''}\n`);
}
function fail(name, detail = '') {
  results.push({ status: 'FAIL', name, detail });
  throw new Error(`${name}${detail ? `: ${detail}` : ''}`);
}
function assert(condition, name, detail = '') {
  if (!condition) fail(name, detail);
  pass(name, detail);
}
function check(condition, name, detail = '') {
  if (!condition) {
    results.push({ status: 'FAIL', name, detail });
    process.stdout.write(`FAIL | ${name}${detail ? ` | ${detail}` : ''}\n`);
    return;
  }
  pass(name, detail);
}
function isoInSaoPaulo(offsetDays) {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-US', {
      timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit'
    }).formatToParts(new Date()).filter(p => p.type !== 'literal').map(p => [p.type, p.value])
  );
  const date = new Date(Date.UTC(Number(parts.year), Number(parts.month) - 1, Number(parts.day) + offsetDays));
  return date.toISOString().slice(0, 10);
}
const enumDays = ['DOMINGO', 'SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO'];
const ptDays = ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'];
function dayFor(iso) { return enumDays[new Date(`${iso}T12:00:00Z`).getUTCDay()]; }
function ptDayFor(iso) { return ptDays[new Date(`${iso}T12:00:00Z`).getUTCDay()]; }
function definirDataDeDemonstracaoHoje(reservaId) {
  if (process.env.COURTTIME_E2E_ACK_ISOLATED_DB !== 'YES'
      || process.env.COURTTIME_E2E_DATABASE_NAME !== 'courttime_e2e') {
    throw new Error('A simulação de reserva de hoje exige o banco descartável courttime_e2e.');
  }
  const resultado = execFileSync('psql', [
    '-h', 'localhost', '-U', 'courttime_e2e', '-d', 'courttime_e2e', '-v', 'ON_ERROR_STOP=1',
    '-qAtc', `UPDATE reservas SET data_utilizacao = '${today}' WHERE id = ${reservaId} AND status = 'APROVADA' RETURNING id`,
  ], { encoding: 'utf8', env: process.env }).trim();
  if (resultado !== String(reservaId)) {
    throw new Error(`A reserva aprovada #${reservaId} não foi atualizada no banco descartável.`);
  }
}
function proximaDataDoDia(diaSemana, deslocamentoMinimo = 7) {
  for (let deslocamento = deslocamentoMinimo; deslocamento < deslocamentoMinimo + 14; deslocamento += 1) {
    const candidata = isoInSaoPaulo(deslocamento);
    if (dayFor(candidata) === diaSemana) return candidata;
  }
  throw new Error(`Nao foi possivel determinar uma data futura para ${diaSemana}.`);
}
const yesterday = isoInSaoPaulo(-1);
const today = isoInSaoPaulo(0);
const requestDate = proximaDataDoDia('SEGUNDA');
const blockedDate = proximaDataDoDia('TERCA');

async function login(page, role) {
  await autenticar(page, configuracao, role, { comprovarInstancia: role === 'admin' });
  pass(`login ${role}`);
}

async function createUser(page, name, email, profile) {
  await page.goto(`${BASE}/admin/usuarios/novo`);
  await page.locator('#nome').fill(name);
  await page.locator('#email').fill(email);
  await page.locator('#perfil').selectOption(profile);
  await page.locator('#senha').fill(PASSWORD);
  await page.locator('#confirmacaoSenha').fill(PASSWORD);
  await Promise.all([
    page.waitForURL(url => url.pathname === '/admin/usuarios'),
    page.getByRole('button', { name: 'Cadastrar usuário' }).click(),
  ]);
  assert((await page.locator('body').innerText()).includes('Usuário cadastrado com sucesso.'), `cadastro ${profile}`, email);
}

async function createSchedule(page, day) {
  await page.goto(`${BASE}/admin/horarios/novo`);
  await page.locator('#diaSemana').selectOption(day);
  await page.locator('#horarioInicio').fill('08:00');
  await page.locator('#horarioFim').fill('12:00');
  await Promise.all([
    page.waitForURL(url => url.pathname === '/admin/horarios'),
    page.getByRole('button', { name: 'Cadastrar horário' }).click(),
  ]);
  assert((await page.locator('body').innerText()).includes('Horário semanal cadastrado com sucesso.'), `grade ${day}`);
}

async function csrfFetch(page, url, fields = {}, method = 'POST') {
  return postComCsrf(page, url, fields, method);
}

async function noCsrfFetch(page, url, fields = {}) {
  return postSemCsrf(page, url, fields);
}

async function createRequest(page, date, time, purpose) {
  await page.goto(`${BASE}/solicitacoes/nova?data=${date}`);
  await selecionarHorario(page, time);
  await page.locator('#finalidade').fill(purpose);
  await Promise.all([
    page.waitForURL(url => /^\/solicitacoes\/\d+$/.test(url.pathname)),
    page.getByRole('button', { name: 'Registrar solicitação' }).click(),
  ]);
  const id = Number(page.url().match(/\/solicitacoes\/(\d+)$/)[1]);
  assert((await page.locator('body').innerText()).includes('Solicitação registrada.'), 'criação da solicitação', `#${id}`);
  return id;
}

async function addTenParticipants(page, requestId, prefix) {
  for (let i = 1; i <= 10; i += 1) {
    await page.goto(`${BASE}/solicitacoes/${requestId}/participantes/novo`);
    await page.locator('#nomeCompleto').fill(`${prefix} ${String(i).padStart(2, '0')}`);
    await page.locator('#rmOuRg').fill(`RM-${requestId}-${String(i).padStart(3, '0')}`);
    const categoria = i <= 8 ? 'ALUNO' : 'OUTRO';
    await page.locator('#categoria').selectOption(categoria);
    if (categoria === 'OUTRO') await page.locator('#vinculo').selectOption('PAI_OU_MAE');
    await Promise.all([
      page.waitForURL(url => url.pathname === `/solicitacoes/${requestId}/participantes`),
      page.getByRole('button', { name: 'Salvar participante' }).click(),
    ]);
  }
  const body = await page.locator('body').innerText();
  assert(body.includes('Mínimo de participantes atendido') && body.includes('8\nAlunos') && body.includes('2\nDemais participantes'), 'mínimo inicial conta dez pessoas de categorias diferentes', `solicitação #${requestId}`);
}

async function upload(page, requestId, name, buffer, mime = 'application/pdf') {
  await page.goto(`${BASE}/solicitacoes/${requestId}/documentos`);
  await page.locator('#arquivo').setInputFiles({ name, mimeType: mime, buffer });
  await Promise.all([
    page.waitForLoadState('networkidle'),
    page.getByRole('button', { name: 'Enviar nova versão' }).click(),
  ]);
  return page.locator('body').innerText();
}

async function responsiveAudit(page, label, url) {
  const sizes = [[390, 844], [430, 932], [768, 1024], [1440, 1000]];
  for (const [width, height] of sizes) {
    await page.setViewportSize({ width, height });
    await page.goto(url);
    const audit = await page.evaluate(() => {
      const controls = [...document.querySelectorAll('input:not([type="hidden"]), select, textarea')];
      const unlabeled = controls.filter(el => {
        const explicit = el.id && document.querySelector(`label[for="${CSS.escape(el.id)}"]`);
        return !explicit && !el.closest('label') && !el.getAttribute('aria-label') && !el.getAttribute('aria-labelledby');
      }).map(el => `${el.tagName.toLowerCase()}#${el.id}`);
      return {
        overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
        unlabeled,
        positiveTabIndex: [...document.querySelectorAll('[tabindex]')]
          .filter(el => Number(el.getAttribute('tabindex')) > 0)
          .map(el => `${el.tagName.toLowerCase()}#${el.id}`),
        lang: document.documentElement.lang,
        h1: document.querySelectorAll('h1').length,
      };
    });
    assert(!audit.overflow, `responsividade ${label} ${width}px`, 'sem rolagem horizontal global');
    assert(audit.unlabeled.length === 0, `rótulos ${label} ${width}px`, JSON.stringify(audit.unlabeled));
    assert(audit.positiveTabIndex.length === 0, `ordem de foco ${label} ${width}px`, 'sem tabindex positivo');
    assert(audit.lang === 'pt-BR' && audit.h1 === 1, `estrutura ${label} ${width}px`, `lang=${audit.lang}, h1=${audit.h1}`);
    await page.screenshot({ path: path.join(EVIDENCE, `${label}-${width}.png`), fullPage: true });
  }
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(CHROMIUM_EXECUTABLE ? { executablePath: CHROMIUM_EXECUTABLE } : {}),
    args: ['--no-sandbox']
  });
  const contexts = {};
  let approvedId;
  try {
    const pdfContext = await browser.newContext();
    const pdfPage = await pdfContext.newPage();
    await pdfPage.setContent('<!doctype html><html lang="pt-BR"><body><h1>Documento sintético de teste</h1><p>Não é termo oficial. Não representa assinatura.</p></body></html>');
    const syntheticPdf = await pdfPage.pdf({ format: 'A4' });
    await pdfContext.close();
    assert(syntheticPdf.subarray(0, 5).toString() === '%PDF-', 'PDF sintético tecnicamente válido');

    contexts.admin = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const admin = await contexts.admin.newPage();
    await responsiveAudit(admin, 'login', `${BASE}/login`);
    await login(admin, 'admin');

    const csrfResult = await noCsrfFetch(admin, '/admin/horarios', { diaSemana: 'SEGUNDA', horarioInicio: '13:00', horarioFim: '14:00' });
    assert(csrfResult.status === 403, 'CSRF rejeita POST administrativo sem token', `HTTP ${csrfResult.status}`);

    await createUser(admin, 'Aluna Sintética A', users.alunoA[0], 'ALUNO');
    await createUser(admin, 'Aluno Sintético B', users.alunoB[0], 'ALUNO');
    await createUser(admin, 'Secretaria Sintética', users.secretaria[0], 'SECRETARIA');
    await createUser(admin, 'Segurança Sintética', users.seguranca[0], 'SEGURANCA');
    for (const day of ['SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA']) await createSchedule(admin, day);
    await admin.goto(`${BASE}/admin/horarios`);
    const gradeText = await admin.locator('body').innerText();
    for (const dia of ['Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira']) {
      assert(gradeText.includes(dia), `grade controlada contem ${dia}`);
    }
    await responsiveAudit(admin, 'admin-painel', `${BASE}/admin`);
    await responsiveAudit(admin, 'admin-usuarios', `${BASE}/admin/usuarios`);
    await responsiveAudit(admin, 'admin-horarios', `${BASE}/admin/horarios`);

    await admin.goto(`${BASE}/admin/bloqueios/novo`);
    await admin.locator('#data').fill(blockedDate);
    await admin.locator('#horarioInicio').fill('08:00');
    await admin.locator('#horarioFim').fill('09:00');
    await admin.locator('#motivo').fill('Manutenção sintética para teste da banca');
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/bloqueios'),
      admin.getByRole('button', { name: 'Cadastrar bloqueio' }).click(),
    ]);
    assert((await admin.locator('body').innerText()).includes('Bloqueio cadastrado com sucesso.'), 'cadastro de bloqueio futuro');

    await admin.goto(`${BASE}/admin/bloqueios/novo`);
    await admin.locator('#data').fill(blockedDate);
    await admin.locator('#horarioInicio').fill('10:00');
    await admin.locator('#horarioFim').fill('11:00');
    await admin.locator('#motivo').fill('Bloqueio sintético que será desativado');
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/bloqueios'),
      admin.getByRole('button', { name: 'Cadastrar bloqueio' }).click(),
    ]);
    const inactiveRow = admin.locator('tbody tr').filter({ hasText: 'Bloqueio sintético que será desativado' });
    admin.once('dialog', dialog => dialog.accept());
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/bloqueios'),
      inactiveRow.getByRole('button', { name: 'Desativar' }).click(),
    ]);
    assert((await admin.locator('body').innerText()).includes('Bloqueio desativado.'), 'bloqueio inativo preservado sem impedir a grade');
    await responsiveAudit(admin, 'admin-bloqueios', `${BASE}/admin/bloqueios`);

    contexts.alunoA = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    contexts.alunoB = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const alunoA = await contexts.alunoA.newPage();
    const alunoB = await contexts.alunoB.newPage();
    await login(alunoA, 'alunoA');
    await login(alunoB, 'alunoB');
    await responsiveAudit(alunoA, 'aluno-quadra', `${BASE}/quadra?data=${requestDate}`);
    await responsiveAudit(alunoA, 'aluno-solicitacoes', `${BASE}/solicitacoes`);

    await alunoA.goto(`${BASE}/solicitacoes/nova`);
    const minDate = await alunoA.locator('#data-consulta').getAttribute('min');
    assert(minDate === isoInSaoPaulo(1), 'timezone America/Sao_Paulo e antecedência mínima', `min=${minDate}`);

    const pastAttempt = await csrfFetch(alunoA, '/solicitacoes', { data: yesterday, horarioInicio: '09:00', finalidade: 'Tentativa no passado' });
    assert(pastAttempt.text.includes('pelo menos um dia de antecedência'), 'servidor rejeita solicitação para data passada');
    const todayAttempt = await csrfFetch(alunoA, '/solicitacoes', { data: today, horarioInicio: '09:00', finalidade: 'Tentativa hoje' });
    assert(todayAttempt.text.includes('pelo menos um dia de antecedência'), 'servidor rejeita solicitação para hoje');
    const noGridAttempt = await csrfFetch(alunoA, '/solicitacoes', { data: requestDate, horarioInicio: '12:00', finalidade: 'Fora da grade' });
    assert(noGridAttempt.text.includes('horário semanal ativo') || noGridAttempt.text.includes('não está disponível'), 'servidor rejeita período fora da grade', `HTTP ${noGridAttempt.status}: ${noGridAttempt.text.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').slice(0, 500)}`);
    const blockedAttempt = await csrfFetch(alunoA, '/solicitacoes', { data: blockedDate, horarioInicio: '08:00', finalidade: 'Período bloqueado' });
    assert(blockedAttempt.text.includes('bloqueio') || blockedAttempt.text.includes('não está disponível'), 'servidor rejeita período bloqueado', `HTTP ${blockedAttempt.status}: ${blockedAttempt.text.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').slice(0, 500)}`);

    await alunoA.goto(`${BASE}/solicitacoes/nova?data=${blockedDate}`);
    const horariosAposDesativacao = await listarHorariosDisponiveis(alunoA);
    assert(horariosAposDesativacao.includes('10:00'), 'bloqueio inativo não impede horário válido', horariosAposDesativacao.join(', '));

    const semJavascript = await browser.newContext({ viewport: { width: 430, height: 932 }, javaScriptEnabled: false });
    const paginaSemJavascript = await semJavascript.newPage();
    await login(paginaSemJavascript, 'alunoA');
    await paginaSemJavascript.goto(`${BASE}/solicitacoes/nova?data=${blockedDate}`);
    await selecionarHorario(paginaSemJavascript, '10:00');
    await paginaSemJavascript.locator('#finalidade').fill('Solicitação sintética sem JavaScript');
    await Promise.all([
      paginaSemJavascript.waitForURL(url => /^\/solicitacoes\/\d+$/.test(url.pathname)),
      paginaSemJavascript.getByRole('button', { name: 'Registrar solicitação' }).click(),
    ]);
    const detalheSemJavascript = await paginaSemJavascript.locator('body').innerText();
    assert(/10:00\s*(às|–|-)\s*11:00/.test(detalheSemJavascript), 'backend define duração de uma hora sem campo horarioFim');
    assert(!detalheSemJavascript.includes('Reserva autorizada'), 'criação não produz aprovação automática');
    await semJavascript.close();

    const requestA = await createRequest(alunoA, requestDate, '09:00', 'Atividade esportiva sintética A');
    const requestB = await createRequest(alunoB, requestDate, '09:00', 'Atividade esportiva sintética B');

    let response = await alunoB.goto(`${BASE}/solicitacoes/${requestA}`);
    assert(response.status() === 404, 'IDOR de solicitação alheia', `HTTP ${response.status()}`);

    await addTenParticipants(alunoA, requestA, 'Participante Sintético A');
    await addTenParticipants(alunoB, requestB, 'Participante Sintético B');
    await responsiveAudit(alunoA, 'aluno-participantes', `${BASE}/solicitacoes/${requestA}/participantes`);

    // Formulário oficial preenchido: CPF e curso só no momento da geração.
    await alunoA.goto(`${BASE}/solicitacoes/${requestA}/documentos`);
    await alunoA.locator('#cpf').fill('123.456.789-09');
    await alunoA.locator('#curso').fill('Técnico em Informática para Internet');
    const [formularioOficial] = await Promise.all([
      alunoA.waitForEvent('download'),
      alunoA.getByRole('button', { name: 'Gerar formulário preenchido' }).click(),
    ]);
    const formularioBytes = fs.readFileSync(await formularioOficial.path());
    assert(formularioBytes.subarray(0, 5).toString() === '%PDF-' && formularioOficial.suggestedFilename().endsWith('.pdf'),
      'formulário oficial preenchido é baixado como PDF', formularioOficial.suggestedFilename());
    assert(!alunoA.url().includes('123.456.789-09') && !alunoA.url().includes('12345678909'), 'CPF não aparece na URL');
    await alunoA.locator('#cpf').fill('111.111.111-11');
    await alunoA.locator('#curso').fill('Informática');
    await Promise.all([
      alunoA.waitForURL(url => url.pathname === `/solicitacoes/${requestA}/documentos`),
      alunoA.getByRole('button', { name: 'Gerar formulário preenchido' }).click(),
    ]);
    assert((await alunoA.locator('body').innerText()).includes('Informe um CPF válido'), 'CPF inválido é recusado com mensagem');

    let body = await upload(alunoA, requestA, 'nao-e-pdf.pdf', Buffer.from('conteudo inválido'), 'application/pdf');
    assert(body.includes('conteúdo enviado não corresponde') || body.includes('arquivo PDF válido'), 'upload rejeita conteúdo malformado');

    body = await upload(alunoA, requestA, 'excessivo.pdf', Buffer.concat([Buffer.from('%PDF-1.7\n'), Buffer.alloc(10 * 1024 * 1024 + 1)]), 'application/pdf');
    assert(body.includes('no máximo 10 MB'), 'upload rejeita arquivo excessivo');

    body = await upload(alunoA, requestA, 'documento-sintetico-nao-oficial-v1.pdf', syntheticPdf);
    assert(body.includes('PDF enviado como nova versão') && body.includes('v1'), 'upload válido preserva versão 1');
    await responsiveAudit(alunoA, 'aluno-documentos', `${BASE}/solicitacoes/${requestA}/documentos`);
    const downloadHrefA = await alunoA.getByRole('link', { name: 'Baixar original' }).first().getAttribute('href');
    const documentA1 = Number(downloadHrefA.match(/documentos\/(\d+)\/baixar/)[1]);
    const lockedParticipantChange = await csrfFetch(alunoA, `/solicitacoes/${requestA}/participantes`, {
      nomeCompleto: 'Alteração Sintética Bloqueada',
      rmOuRg: `RM-${requestA}-999`,
      categoria: 'ALUNO'
    });
    assert(lockedParticipantChange.text.includes('só podem ser alterados'), 'lista de participantes fica bloqueada após o envio');
    response = await alunoB.goto(`${BASE}${downloadHrefA}`);
    assert(response.status() === 404, 'IDOR de download privado', `HTTP ${response.status()}`);

    body = await upload(alunoB, requestB, 'documento-sintetico-nao-oficial-b.pdf', syntheticPdf);
    assert(body.includes('PDF enviado como nova versão'), 'upload válido da solicitação conflitante');
    const downloadHrefB = await alunoB.getByRole('link', { name: 'Baixar original' }).first().getAttribute('href');
    const documentB = Number(downloadHrefB.match(/documentos\/(\d+)\/baixar/)[1]);

    contexts.secretaria = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const secretaria = await contexts.secretaria.newPage();
    await login(secretaria, 'secretaria');
    response = await secretaria.goto(`${BASE}/admin`);
    assert(response.status() === 403, 'secretaria não acessa administração', `HTTP ${response.status()}`);
    await secretaria.goto(`${BASE}/secretaria/documentos/${documentA1}`);
    await responsiveAudit(secretaria, 'secretaria-documento', `${BASE}/secretaria/documentos/${documentA1}`);
    const allowedDownload = await secretaria.context().request.get(`${BASE}/secretaria/documentos/${documentA1}/baixar`);
    const allowedBytes = await allowedDownload.body();
    assert(allowedDownload.status() === 200
      && allowedDownload.headers()['content-type']?.includes('application/pdf')
      && allowedBytes.subarray(0, 5).toString() === '%PDF-', 'secretaria baixa o PDF privado permitido');
    await secretaria.locator('#justificativa').evaluate(element => element.removeAttribute('required'));
    await Promise.all([
      secretaria.waitForURL(url => url.pathname === `/secretaria/documentos/${documentA1}`),
      secretaria.getByRole('button', { name: 'Devolver para correção' }).click(),
    ]);
    const blankReturnBody = await secretaria.locator('body').innerText();
    check(blankReturnBody.includes('justificativa') && blankReturnBody.includes('obrigatória'), 'devolução documental exige justificativa com mensagem visível', `HTTP 200`);
    await secretaria.screenshot({ path: path.join(EVIDENCE, 'secretaria-devolucao-sem-justificativa-1440.png'), fullPage: true });
    if (process.env.COURTTIME_E2E_STOP_AFTER_SECRETARIA_FEEDBACK === 'true') {
      fs.writeFileSync(path.join(EVIDENCE, 'resultados-feedback-secretaria.json'), JSON.stringify({
        sha: TARGET_SHA,
        results
      }, null, 2));
      const passed = results.filter(result => result.status === 'PASS').length;
      const failed = results.filter(result => result.status === 'FAIL').length;
      process.stdout.write(`SUMMARY FEEDBACK | ${passed} aprovadas | ${failed} reprovadas\n`);
      process.exitCode = failed ? 1 : 0;
      return;
    }
    await secretaria.locator('#justificativa').fill('Correção sintética: reenviar para validar o histórico de versões.');
    await Promise.all([
      secretaria.waitForURL(url => url.pathname === '/secretaria/documentos'),
      secretaria.getByRole('button', { name: 'Devolver para correção' }).click(),
    ]);
    assert((await secretaria.locator('body').innerText()).includes('Documento devolvido para correção.'), 'devolução documental justificada');

    await alunoA.goto(`${BASE}/solicitacoes/${requestA}`);
    assert((await alunoA.locator('body').innerText()).includes('Documento devolvido para correção'), 'aluno acompanha devolução');
    body = await upload(alunoA, requestA, 'documento-sintetico-nao-oficial-v2.pdf', syntheticPdf);
    assert(body.includes('v2') && body.includes('Devolvido para correção'), 'substituição preserva histórico e cria versão 2');
    const downloadHrefA2 = await alunoA.getByRole('link', { name: 'Baixar original' }).first().getAttribute('href');
    const documentA2 = Number(downloadHrefA2.match(/documentos\/(\d+)\/baixar/)[1]);

    await secretaria.goto(`${BASE}/secretaria/documentos/${documentB}`);
    const prematureContribution = await csrfFetch(secretaria, `/secretaria/contribuicoes/solicitacoes/${requestB}`, { valor: '20.00', observacoes: 'Tentativa antes da conferência' });
    assert(prematureContribution.text.includes('documento') && (prematureContribution.text.includes('validada') || prematureContribution.text.includes('conferido')), 'contribuição recusada antes da conferência', `HTTP ${prematureContribution.status}`);

    for (const doc of [documentA2, documentB]) {
      await secretaria.goto(`${BASE}/secretaria/documentos/${doc}`);
      const semConfirmacao = await csrfFetch(secretaria, `/secretaria/documentos/${doc}/validar`);
      assert(semConfirmacao.text.includes('Confirme que conferiu a assinatura gov.br'), 'R07: validar sem confirmar a assinatura gov.br é recusado', `documento #${doc}`);
      await secretaria.goto(`${BASE}/secretaria/documentos/${doc}`);
      assert((await secretaria.locator('body').innerText()).includes('validar.iti.gov.br'), 'R07: orientação do verificador do ITI', `documento #${doc}`);
      await secretaria.getByLabel('Conferi a assinatura gov.br no verificador oficial do ITI').check();
      await Promise.all([
        secretaria.waitForURL(url => url.pathname === '/secretaria/documentos'),
        secretaria.getByRole('button', { name: 'Validar documento' }).click(),
      ]);
      assert((await secretaria.locator('body').innerText()).includes('Documento conferido e validado.'), 'conferência documental', `documento #${doc}`);
    }
    await secretaria.goto(`${BASE}/secretaria/documentos`);
    const repeatedValidation = await csrfFetch(secretaria, `/secretaria/documentos/${documentB}/validar`, { assinaturaGovBrConferida: 'true' });
    assert(repeatedValidation.text.includes('já possui uma conferência') || repeatedValidation.text.includes('já foi conferido') || repeatedValidation.text.includes('não pode ser conferido'), 'conferência repetida é recusada');

    await secretaria.goto(`${BASE}/secretaria/contribuicoes`);
    await responsiveAudit(secretaria, 'secretaria-contribuicoes', `${BASE}/secretaria/contribuicoes`);
    for (const requestId of [requestA, requestB]) {
      const operacao = secretaria.getByRole('heading', { name: `Solicitação #${requestId}` }).locator('..');
      const textoOperacao = await operacao.innerText();
      assert(textoOperacao.includes('participantes × R$ 2,00 = R$ '), 'R03: valor devido calculado na tela', `solicitação #${requestId}`);
      assert(await operacao.locator('input[name="valor"]').count() === 0, 'R03: secretaria não digita o valor', `solicitação #${requestId}`);
      await operacao.getByLabel('Observações').fill('Recebimento sintético para validação da banca');
      await Promise.all([
        secretaria.waitForURL(url => url.pathname === `/secretaria/contribuicoes/solicitacoes/${requestId}`),
        operacao.getByRole('button', { name: 'Registrar recebimento' }).click(),
      ]);
      assert((await secretaria.locator('body').innerText()).includes('Contribuição presencial registrada.'), 'registro de contribuição presencial', `solicitação #${requestId}`);
      await secretaria.goto(`${BASE}/secretaria/contribuicoes`);
    }
    const repeatedContribution = await csrfFetch(secretaria, `/secretaria/contribuicoes/solicitacoes/${requestB}`, { valor: '20.00', observacoes: 'Duplicidade' });
    assert(repeatedContribution.text.includes('já possui contribuição') || repeatedContribution.text.includes('só pode ser registrada enquanto a documentação está pendente'), 'contribuição duplicada é recusada');

    const secretaryApprove = await csrfFetch(secretaria, `/admin/solicitacoes/${requestA}/aprovar`);
    assert(secretaryApprove.status === 403, 'secretaria não aprova solicitações', `HTTP ${secretaryApprove.status}`);

    contexts.admin2 = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const admin2 = await contexts.admin2.newPage();
    await login(admin2, 'admin');
    await admin.goto(`${BASE}/admin/solicitacoes/${requestA}`);
    await admin2.goto(`${BASE}/admin/solicitacoes/${requestB}`);
    const [approveA, approveB] = await Promise.all([
      csrfFetch(admin, `/admin/solicitacoes/${requestA}/aprovar`),
      csrfFetch(admin2, `/admin/solicitacoes/${requestB}/aprovar`),
    ]);
    const aApproved = approveA.text.includes('Solicitação aprovada e reserva autorizada.');
    const bApproved = approveB.text.includes('Solicitação aprovada e reserva autorizada.');
    assert(Number(aApproved) + Number(bApproved) === 1, 'concorrência entre aprovações permite exatamente uma', `A=${aApproved}, B=${bApproved}`);
    approvedId = aApproved ? requestA : requestB;
    const refusedId = aApproved ? requestB : requestA;
    const refusedBody = aApproved ? approveB.text : approveA.text;
    assert(refusedBody.includes('não está mais disponível') || refusedBody.includes('conflito'), 'aprovação concorrente perdedora informa conflito', `recusada #${refusedId}`);

    await admin.goto(`${BASE}/admin/solicitacoes/${approvedId}`);
    const repeatedApproval = await csrfFetch(admin, `/admin/solicitacoes/${approvedId}/aprovar`);
    assert(repeatedApproval.text.includes('documentação conferida') || repeatedApproval.text.includes('não está pendente') || repeatedApproval.text.includes('não pode ser aprovada'), 'aprovação repetida é recusada');

    await admin.goto(`${BASE}/admin/solicitacoes/${refusedId}`);
    const blankRejection = await csrfFetch(admin, `/admin/solicitacoes/${refusedId}/rejeitar`, { motivo: '' });
    assert(blankRejection.text.includes('motivo') && blankRejection.text.includes('obrigatório'), 'rejeição exige motivo');

    await alunoA.goto(`${BASE}/solicitacoes/nova?data=${requestDate}`);
    const availableOptions = await listarHorariosDisponiveis(alunoA);
    assert(!availableOptions.includes('09:00'), 'horário aprovado deixa de aparecer disponível');

    await admin.goto(`${BASE}/admin/horarios`);
    const scheduleRow = admin.locator('tbody tr').filter({ hasText: ptDayFor(requestDate) }).first();
    admin.once('dialog', dialog => dialog.accept());
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/horarios'),
      scheduleRow.getByRole('button', { name: 'Desativar' }).click(),
    ]);
    assert((await admin.locator('body').innerText()).includes('reserva aprovada'), 'grade não pode ser desativada sobre reserva aprovada');

    await admin.goto(`${BASE}/admin/bloqueios/novo`);
    await admin.locator('#data').fill(requestDate);
    await admin.locator('#horarioInicio').fill('09:00');
    await admin.locator('#horarioFim').fill('10:00');
    await admin.locator('#motivo').fill('Tentativa sintética sobre reserva aprovada');
    await admin.getByRole('button', { name: 'Cadastrar bloqueio' }).click();
    assert((await admin.locator('body').innerText()).includes('reserva aprovada'), 'bloqueio não pode comprometer reserva aprovada');

    await responsiveAudit(admin, 'admin-solicitacao-aprovada', `${BASE}/admin/solicitacoes/${approvedId}`);
    await responsiveAudit(admin, 'admin-solicitacoes', `${BASE}/admin/solicitacoes`);

    contexts.seguranca = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const seguranca = await contexts.seguranca.newPage();
    await login(seguranca, 'seguranca');
    await seguranca.goto(`${BASE}/seguranca/reservas`);
    await responsiveAudit(seguranca, 'seguranca-reservas', `${BASE}/seguranca/reservas`);
    let securityBody = await seguranca.locator('body').innerText();
    const securityLinks = await seguranca.getByRole('link', { name: 'Conferir' }).evaluateAll(links => links.map(link => link.getAttribute('href')));
    assert(securityLinks.includes(`/seguranca/reservas/${approvedId}`) && !securityLinks.includes(`/seguranca/reservas/${refusedId}`), 'segurança vê somente reserva autorizada');
    await seguranca.getByRole('link', { name: 'Conferir' }).click();
    securityBody = await seguranca.locator('body').innerText();
    assert(securityBody.includes('Identificação dos participantes') && securityBody.includes('RM-'), 'segurança consulta identificação dos participantes');
    assert(!securityBody.includes('Contribuição') && !securityBody.includes('Documento') && !securityBody.includes('@e2e.invalid'), 'segurança não recebe dados internos desnecessários');
    assert(securityBody.includes('A presença só pode ser marcada no dia da reserva.') && await seguranca.locator('input[type="radio"][name^="presenca-"]').count() === 0, 'R08: presença só no dia da reserva');
    const presencaForaDoDia = await csrfFetch(seguranca, `/seguranca/reservas/${approvedId}/presencas`, { 'presenca-1': 'PRESENTE' });
    assert(presencaForaDoDia.text.includes('A presença só pode ser registrada no dia da reserva.'), 'R08: marcação fora do dia é recusada no servidor');

    // Jornada real de portaria: 2 ausências substituídas não geram cobrança;
    // 2 acréscimos líquidos geram R$ 4,00, pagos à segurança e repassados à Secretaria.
    // A data é deslocada somente no PostgreSQL descartável, sem cadastrar
    // horários institucionais fictícios nem afetar outros ambientes.
    definirDataDeDemonstracaoHoje(approvedId);
    await seguranca.goto(`${BASE}/seguranca/reservas/${approvedId}`);
    const ausentes = await seguranca.locator('input[type="radio"][name^="presenca-"][value="AUSENTE"]').evaluateAll(inputs => inputs.slice(0, 2).map(input => input.name));
    const nomesPresenca = await seguranca.locator('input[type="radio"][name^="presenca-"][value="PRESENTE"]').evaluateAll(inputs => inputs.map(input => input.name));
    for (const name of nomesPresenca) {
      const presenca = ausentes.includes(name) ? 'AUSENTE' : 'PRESENTE';
      await seguranca.locator(`input[type="radio"][name="${name}"][value="${presenca}"]`).check();
    }
    await Promise.all([
      seguranca.waitForNavigation({ waitUntil: 'domcontentloaded' }),
      seguranca.getByRole('button', { name: 'Registrar presença' }).click(),
    ]);
    const primeiraPessoa = { nome: 'Substituto sintético A', identificador: `PORTARIA-${approvedId}-A` };
    const segundaPessoa = { nome: 'Substituto sintético B', identificador: `PORTARIA-${approvedId}-B` };
    for (const pessoa of [primeiraPessoa, segundaPessoa]) {
      await seguranca.locator('#nomeCompleto').fill(pessoa.nome);
      await seguranca.locator('#rmOuRg').fill(pessoa.identificador);
      await seguranca.locator('#categoria').selectOption('OUTRO');
      await seguranca.locator('#vinculo').selectOption('PAI_OU_MAE');
      await Promise.all([
        seguranca.waitForNavigation({ waitUntil: 'domcontentloaded' }),
        seguranca.getByRole('button', { name: 'Acrescentar pessoa' }).click(),
      ]);
    }
    securityBody = await seguranca.locator('body').innerText();
    const presentesNaTroca = await seguranca.locator('.indicador').filter({ hasText: 'Presentes (mínimo 10)' }).locator('.indicador__valor').innerText();
    assert(presentesNaTroca === '10' && !securityBody.includes('Falta pagar R$'), 'troca de duas pessoas não cobra valor adicional', `presentes=${presentesNaTroca}`);
    for (let i = 1; i <= 2; i += 1) {
      await seguranca.locator('#nomeCompleto').fill(`Pessoa adicional sintética ${i}`);
      await seguranca.locator('#rmOuRg').fill(`PORTARIA-${approvedId}-EXTRA-${i}`);
      await seguranca.locator('#categoria').selectOption('OUTRO');
      await seguranca.locator('#vinculo').selectOption('PAI_OU_MAE');
      await Promise.all([
        seguranca.waitForNavigation({ waitUntil: 'domcontentloaded' }),
        seguranca.getByRole('button', { name: 'Acrescentar pessoa' }).click(),
      ]);
    }
    securityBody = await seguranca.locator('body').innerText();
    assert(securityBody.includes('Falta pagar R$ 4,00.') && !securityBody.includes('Recebido por'), 'segurança vê somente o saldo devido, sem histórico de pagamentos');
    await Promise.all([
      seguranca.waitForNavigation({ waitUntil: 'domcontentloaded' }),
      seguranca.getByRole('button', { name: 'Recebi o valor na portaria' }).click(),
    ]);
    await Promise.all([
      seguranca.waitForNavigation({ waitUntil: 'domcontentloaded' }),
      seguranca.getByRole('button', { name: 'Liberar quadra' }).click(),
    ]);
    securityBody = await seguranca.locator('body').innerText();
    assert(securityBody.includes('A conferência foi encerrada') && await seguranca.locator('input[type="radio"][name^="presenca-"]').count() === 0 && await seguranca.locator('#nomeCompleto').count() === 0, 'liberação encerra conferência e bloqueia alterações na interface');
    const alteracaoAposLiberacao = await csrfFetch(seguranca, `/seguranca/reservas/${approvedId}/presencas`, { [nomesPresenca[0]]: 'AUSENTE' });
    assert(alteracaoAposLiberacao.text.includes('A conferência foi encerrada'), 'servidor rejeita alteração de presença após liberação');
    const inclusaoAposLiberacao = await csrfFetch(seguranca, `/seguranca/reservas/${approvedId}/pessoas`, {
      nomeCompleto: 'Inclusão tardia sintética',
      rmOuRg: `PORTARIA-${approvedId}-TARDIA`,
      categoria: 'OUTRO',
      vinculo: 'PAI_OU_MAE',
    });
    assert(inclusaoAposLiberacao.text.includes('A conferência foi encerrada'), 'servidor rejeita inclusão de pessoa após liberação');

    await secretaria.goto(`${BASE}/secretaria/contribuicoes`);
    const repassesTabela = secretaria.getByRole('table', { name: 'Recebido pela portaria, a confirmar' });
    const repasse = repassesTabela.locator('tbody tr').filter({ hasText: `Solicitação #${approvedId}` });
    assert(await repasse.count() === 1 && (await repasse.innerText()).includes('R$ 4,00'), 'secretaria confere valor e operador do repasse da portaria');
    await Promise.all([
      secretaria.waitForNavigation({ waitUntil: 'domcontentloaded' }),
      repasse.getByRole('button', { name: 'Confirmar repasse' }).click(),
    ]);
    assert((await secretaria.locator('body').innerText()).includes('Repasse da portaria confirmado.'), 'secretaria registra repasse recebido');
    response = await seguranca.goto(`${BASE}/secretaria/contribuicoes`);
    assert(response.status() === 403, 'segurança não acessa dados financeiros', `HTTP ${response.status()}`);
    response = await seguranca.goto(`${BASE}/secretaria/documentos`);
    assert(response.status() === 403, 'segurança não acessa documentos', `HTTP ${response.status()}`);
    await responsiveAudit(seguranca, 'seguranca-reserva', `${BASE}/seguranca/reservas/${approvedId}`);

    await admin.goto(`${BASE}/admin/solicitacoes/${approvedId}`);
    await admin.locator('#motivoCancelamento').fill('Cancelamento institucional sintético para validar a banca.');
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/solicitacoes'),
      admin.getByRole('button', { name: 'Cancelar autorização' }).click(),
    ]);
    assert((await admin.locator('body').innerText()).includes('Reserva cancelada por motivo institucional.'), 'cancelamento institucional justificado');
    await seguranca.goto(`${BASE}/seguranca/reservas`);
    securityBody = await seguranca.locator('body').innerText();
    const postCancelLinks = await seguranca.getByRole('link', { name: 'Conferir' }).evaluateAll(links => links.map(link => link.getAttribute('href')));
    assert(!postCancelLinks.includes(`/seguranca/reservas/${approvedId}`), 'reserva cancelada desaparece da segurança');
    await seguranca.screenshot({ path: path.join(EVIDENCE, 'seguranca-pos-cancelamento-1440.png'), fullPage: true });

    // Remarcação: o dono da reserva cancelada pede outro horário pela própria tela.
    const donoCancelada = aApproved ? alunoA : alunoB;
    await donoCancelada.goto(`${BASE}/solicitacoes/${approvedId}`);
    await Promise.all([
      donoCancelada.waitForURL(url => url.pathname === '/solicitacoes/nova'),
      donoCancelada.getByRole('link', { name: 'Solicitar outro horário' }).click(),
    ]);
    assert((await donoCancelada.locator('body').innerText()).includes('Remarcação de reserva cancelada'), 'botão abre a remarcação da reserva cancelada');
    const urlRemarcacao = `${BASE}/solicitacoes/nova?origem=${approvedId}&data=${requestDate}`;
    await responsiveAudit(donoCancelada, 'aluno-remarcacao', urlRemarcacao);
    await donoCancelada.setViewportSize({ width: 1440, height: 1000 });
    await donoCancelada.goto(urlRemarcacao);
    assert((await listarHorariosDisponiveis(donoCancelada)).includes('09:00'), 'cancelamento libera o período para a remarcação');
    assert(await donoCancelada.locator('input[name="copiarParticipantes"][type="checkbox"]').isChecked(), 'cópia dos participantes vem marcada');
    await selecionarHorario(donoCancelada, '09:00');
    await Promise.all([
      donoCancelada.waitForURL(url => /^\/solicitacoes\/\d+$/.test(url.pathname)),
      donoCancelada.getByRole('button', { name: 'Registrar solicitação' }).click(),
    ]);
    const remarcadaId = Number(donoCancelada.url().match(/\/solicitacoes\/(\d+)$/)[1]);
    assert((await donoCancelada.locator('body').innerText()).includes('Esta solicitação remarca a reserva de'), 'nova solicitação mostra a reserva de origem', `#${remarcadaId}`);
    await donoCancelada.goto(`${BASE}/solicitacoes/${remarcadaId}/participantes`);
    assert((await donoCancelada.locator('body').innerText()).includes('Mínimo de participantes atendido'), 'participantes copiados para a remarcação');
    await donoCancelada.goto(`${BASE}/solicitacoes/${approvedId}`);
    const canceladaBody = await donoCancelada.locator('body').innerText();
    assert(!canceladaBody.includes('Solicitar outro horário') && canceladaBody.includes('ver a nova solicitação'), 'reserva cancelada aponta para a remarcação e não oferece outra');
    await donoCancelada.screenshot({ path: path.join(EVIDENCE, 'aluno-cancelada-remarcada-1440.png'), fullPage: true });

    await alunoB.goto(`${BASE}/inicio`);
    await admin.goto(`${BASE}/admin/usuarios?busca=${encodeURIComponent(users.alunoB[0])}`);
    const alunoBRow = admin.locator('tbody tr').filter({ hasText: users.alunoB[0] });
    admin.once('dialog', dialog => dialog.accept());
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/usuarios'),
      alunoBRow.getByRole('button', { name: 'Desativar' }).click(),
    ]);
    await alunoB.goto(`${BASE}/inicio`);
    assert(new URL(alunoB.url()).pathname === '/login' && alunoB.url().includes('expirada'), 'sessão é invalidada após bloqueio da conta');

    const keyboardContext = await browser.newContext({ viewport: { width: 430, height: 932 } });
    const keyboardPage = await keyboardContext.newPage();
    await keyboardPage.goto(`${BASE}/login`);
    await keyboardPage.evaluate(() => {
      document.body.setAttribute('tabindex', '-1');
      document.body.focus();
    });
    await keyboardPage.keyboard.press('Tab');
    const focused = await keyboardPage.evaluate(() => {
      const el = document.activeElement;
      const style = getComputedStyle(el);
      return { text: el.textContent.trim(), outline: style.outlineStyle, outlineWidth: style.outlineWidth, top: el.getBoundingClientRect().top };
    });
    assert(focused.text === 'Pular para o conteúdo' && focused.top >= 0 && (focused.outline !== 'none' || focused.outlineWidth !== '0px'), 'navegação por teclado e foco visível', JSON.stringify(focused));
    await keyboardPage.keyboard.press('Enter');
    assert(await keyboardPage.evaluate(() => location.hash === '#conteudo' && document.querySelector('#conteudo').getBoundingClientRect().top < innerHeight), 'atalho pula para o conteúdo');
    await keyboardContext.close();

    fs.writeFileSync(path.join(EVIDENCE, 'resultados-e2e.json'), JSON.stringify({
      sha: TARGET_SHA,
      dates: { yesterday, today, requestDate, blockedDate, day: dayFor(requestDate) },
      ids: { requestA, requestB, documentA1, documentA2, documentB, approvedId },
      results
    }, null, 2));
    const passed = results.filter(result => result.status === 'PASS').length;
    const failed = results.filter(result => result.status === 'FAIL').length;
    process.stdout.write(`SUMMARY | ${passed} aprovadas | ${failed} reprovadas | reserva autorizada #${approvedId}\n`);
    process.exitCode = failed ? 1 : 0;
  } catch (error) {
    fs.writeFileSync(path.join(EVIDENCE, 'resultados-e2e-parcial.json'), JSON.stringify({ sha: TARGET_SHA, error: error.stack, results, approvedId }, null, 2));
    process.stderr.write(`${error.stack}\n`);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();
