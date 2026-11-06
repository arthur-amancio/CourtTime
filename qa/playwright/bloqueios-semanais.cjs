const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { chromium } = require('playwright');
const { carregarConfiguracao } = require('./suporte/configuracao.cjs');
const { autenticar, postComCsrf, postSemCsrf, listarHorariosDisponiveis } = require('./suporte/sessoes.cjs');

// Executar depois da jornada dos quatro perfis; todas as mutações usam a aplicação.
const config = carregarConfiguracao();
const results = [];
const scans = [];
function check(ok, name) {
  results.push({ status: ok ? 'PASS' : 'FAIL', name });
  assert.ok(ok, name);
  process.stdout.write(`PASS | ${name}\n`);
}
function sql(query) {
  return execFileSync('psql', ['-h', '127.0.0.1', '-p', process.env.PGPORT || '5432',
    '-U', process.env.COURTTIME_DB_USERNAME, '-d', process.env.COURTTIME_E2E_DATABASE_NAME,
    '-v', 'ON_ERROR_STOP=1', '-qAtc', query], { encoding: 'utf8', env: process.env }).trim();
}
// O nome efetivamente conectado é conferido antes de login, captura ou escrita.
assert.equal(sql('SELECT current_database()'), process.env.COURTTIME_E2E_DATABASE_NAME);
const jornada = JSON.parse(fs.readFileSync(path.join(config.evidencias, 'resultados-e2e.json')));
assert.equal(jornada.sha, config.targetSha, 'A jornada deve pertencer ao mesmo candidato.');
// Reservar outro intervalo sintético em uma repetição, sem limpar dados já consumidos.
const sequencia = Number(sql('SELECT count(*) FROM bloqueios_semanais_quadra'));
const inicio = adicionarDias(jornada.dates.requestDate, 140 + 35 * sequencia);
const fim = adicionarDias(inicio, 21);
const fields = { diaSemana: 'SEGUNDA', horarioInicio: '08:30', horarioFim: '09:30',
  vigenciaInicio: inicio, vigenciaFim: fim, motivo: `Aula de Educação Física ${sequencia} <script>window.invasao=1</script>` };
function adicionarDias(iso, dias) {
  const data = new Date(`${iso}T12:00:00Z`);
  data.setUTCDate(data.getUTCDate() + dias);
  return data.toISOString().slice(0, 10);
}

(async () => {
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox'],
    ...(config.chromiumExecutable ? { executablePath: config.chromiumExecutable } : {}) });
  try {
    const admin = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    await autenticar(admin, config, 'admin', { comprovarInstancia: true });
    await admin.goto(`${config.baseUrl}/admin/bloqueios`);
    check(await admin.getByRole('link', { name: 'Novo bloqueio semanal', exact: true }).count() === 1,
      'painel oferece cadastro semanal e mantém cadastro pontual');
    check(await admin.getByRole('link', { name: 'Novo bloqueio', exact: true }).count() === 1,
      'link pontual anterior continua acessível');
    check((await postSemCsrf(admin, '/admin/bloqueios/semanais', fields)).status === 403,
      'cadastro semanal exige CSRF');
    const antes = Number(sql('SELECT count(*) FROM bloqueios_semanais_quadra'));
    const pontuaisAntes = Number(sql('SELECT count(*) FROM bloqueios_quadra'));
    await admin.getByRole('link', { name: 'Novo bloqueio semanal', exact: true }).click();
    for (const [campo, valor] of Object.entries(fields)) {
      if (campo === 'diaSemana') await admin.locator(`#${campo}`).selectOption(valor);
      else await admin.locator(`#${campo}`).fill(valor);
    }
    await Promise.all([admin.waitForURL(url => url.pathname === '/admin/bloqueios'),
      admin.getByRole('button', { name: 'Cadastrar regra semanal' }).click()]);
    check((await admin.locator('body').innerText()).includes('Bloqueio semanal cadastrado com sucesso.'),
      'administrador cadastra regra pela interface');
    const row = admin.locator('tr').filter({ hasText: fields.motivo });
    check(await row.count() === 1, 'consulta apresenta dia, vigência, motivo e estado da regra');
    check(await admin.evaluate(() => window.invasao === undefined), 'motivo é exibido como texto, sem executar script');
    const action = await row.locator('form').getAttribute('action');
    check((await postSemCsrf(admin, action)).status === 403, 'desativação semanal exige CSRF');

    const aluno = await browser.newPage();
    await autenticar(aluno, config, 'alunoA');
    for (const data of [inicio, adicionarDias(inicio, 7), fim]) {
      await aluno.goto(`${config.baseUrl}/solicitacoes/nova?data=${data}`);
      const horarios = await listarHorariosDisponiveis(aluno);
      check(!horarios.includes('08:00') && !horarios.includes('09:00'), `recorrência bloqueia sobreposições em ${data}`);
      check(horarios.includes('10:00'), `intervalo adjacente permanece disponível em ${data}`);
    }
    for (const data of [adicionarDias(inicio, -7), adicionarDias(fim, 7), adicionarDias(inicio, 1)]) {
      await aluno.goto(`${config.baseUrl}/solicitacoes/nova?data=${data}`);
      const horarios = await listarHorariosDisponiveis(aluno);
      check(horarios.includes('08:00') && horarios.includes('09:00'), `fora do dia ou vigência permanece livre em ${data}`);
    }
    await aluno.goto(`${config.baseUrl}/solicitacoes/nova?data=${inicio}`);
    const forjada = await postComCsrf(aluno, '/solicitacoes', { data: inicio, horarioInicio: '08:00',
      finalidade: 'Tentativa sintética em bloqueio semanal' });
    check(forjada.text.includes('não está disponível'), 'servidor recusa solicitação forjada em período semanal bloqueado');
    check(Number(sql('SELECT count(*) FROM bloqueios_semanais_quadra')) === antes + 1,
      'várias consultas mantêm uma única regra persistida');
    check(Number(sql('SELECT count(*) FROM bloqueios_quadra')) === pontuaisAntes,
      'consultas não geram ocorrências nem alteram bloqueios pontuais');

    await admin.goto(`${config.baseUrl}/admin/bloqueios/semanais/novo`);
    const invalida = await postComCsrf(admin, '/admin/bloqueios/semanais', {
      ...fields, vigenciaInicio: fim, vigenciaFim: inicio, motivo: 'Vigência invertida' });
    check(invalida.text.includes('A vigência final não pode ser anterior'), 'vigência invertida é recusada no servidor');
    const semOcorrencia = await postComCsrf(admin, '/admin/bloqueios/semanais', {
      ...fields, vigenciaInicio: adicionarDias(inicio, 1), vigenciaFim: adicionarDias(inicio, 2) });
    check(semOcorrencia.text.includes('ocorrência atual ou futura'), 'vigência sem o dia informado é recusada');

    // Ler somente data/período de uma reserva sintética aprovada pela QA anterior.
    const aprovada = sql("SELECT data_utilizacao || '|' || horario_inicio || '|' || horario_fim FROM reservas WHERE status='APROVADA' AND data_utilizacao > CURRENT_DATE ORDER BY data_utilizacao LIMIT 1");
    assert.ok(aprovada, 'A QA anterior deve preparar uma reserva futura aprovada.');
    const [dataAprovada, horaInicio, horaFim] = aprovada.split('|');
    const dias = ['DOMINGO', 'SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO'];
    const conflito = await postComCsrf(admin, '/admin/bloqueios/semanais', { ...fields,
      diaSemana: dias[new Date(`${dataAprovada}T12:00:00Z`).getUTCDay()], horarioInicio: horaInicio,
      horarioFim: horaFim, vigenciaInicio: dataAprovada, vigenciaFim: dataAprovada, motivo: 'Tentativa sobre aprovada' });
    check(conflito.text.includes('atinge uma reserva aprovada'), 'regra que atinge reserva aprovada é recusada na interface');
    check(Number(sql('SELECT count(*) FROM bloqueios_semanais_quadra')) === antes + 1,
      'cadastros inválidos não deixam regras persistidas');

    for (const role of ['alunoA', 'secretaria', 'seguranca']) {
      const page = role === 'alunoA' ? aluno : await browser.newPage();
      if (role !== 'alunoA') await autenticar(page, config, role);
      check((await page.goto(`${config.baseUrl}/admin/bloqueios/semanais/novo`)).status() === 403,
        `${role} não abre formulário administrativo`);
      await page.goto(`${config.baseUrl}/inicio`);
      check((await postComCsrf(page, '/admin/bloqueios/semanais', fields)).status === 403,
        `${role} não cadastra regra por POST direto`);
      check((await postComCsrf(page, action)).status === 403, `${role} não desativa regra de outro perfil`);
    }
    const anon = await browser.newPage();
    await anon.goto(`${config.baseUrl}/admin/bloqueios/semanais/novo`);
    check(new URL(anon.url()).pathname === '/login', 'anônimo precisa autenticar');

    // Um bloqueio pontual continua valendo mesmo após desativar a regra semanal.
    await admin.goto(`${config.baseUrl}/admin/bloqueios/novo`);
    await postComCsrf(admin, '/admin/bloqueios', { data: inicio, horarioInicio: '10:00',
      horarioFim: '11:00', motivo: 'Pontual preservado com regra semanal' });
    await admin.goto(`${config.baseUrl}/admin/bloqueios`);
    for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }]) {
      await admin.setViewportSize(viewport);
      for (const [pagina, nome] of [['/admin/bloqueios', 'lista'], ['/admin/bloqueios/semanais/novo', 'formulario']]) {
        await admin.goto(config.baseUrl + pagina);
        check(await admin.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
          `${nome} sem overflow global em ${viewport.width}px`);
        if (nome === 'lista') {
          check(await admin.locator('tr').filter({ hasText: fields.motivo }).locator('[data-label="Motivo"]')
            .evaluate(el => el.scrollWidth <= el.clientWidth + 1), `motivo longo legível dentro da célula em ${viewport.width}px`);
        }
        await admin.addScriptTag({ path: require.resolve('axe-core/axe.min.js') });
        const auditoria = await admin.evaluate(async () => (await axe.run()).violations);
        scans.push({ pagina, width: viewport.width, violations: auditoria.map(v => ({ id: v.id, impact: v.impact })) });
        check(auditoria.length === 0, `${nome}: axe sem violações em ${viewport.width}px`);
        await admin.screenshot({ path: path.join(config.evidencias, `semanal-${nome}-${viewport.width}.png`), fullPage: true });
      }
    }
    await admin.goto(`${config.baseUrl}/admin/bloqueios`);
    admin.once('dialog', dialog => dialog.accept());
    await Promise.all([admin.waitForURL(url => url.pathname === '/admin/bloqueios'),
      admin.locator(`form[action="${action}"]`).getByRole('button').click()]);
    check((await admin.locator('body').innerText()).includes('Bloqueio semanal desativado.'), 'desativação inteira confirmada pela interface');
    check((await admin.locator('tr').filter({ hasText: fields.motivo }).innerText()).includes('Inativo'),
      'regra desativada permanece consultável');
    for (const data of [inicio, fim]) {
      await aluno.goto(`${config.baseUrl}/solicitacoes/nova?data=${data}`);
      const horarios = await listarHorariosDisponiveis(aluno);
      check(horarios.includes('08:00') && horarios.includes('09:00'), `desativação libera a recorrência em ${data}`);
      if (data === inicio) check(!horarios.includes('10:00'), 'bloqueio pontual permanece após desativação semanal');
    }
  } catch (error) {
    results.push({ status: 'FAIL', name: error.message });
    process.exitCode = 1;
  } finally {
    fs.writeFileSync(path.join(config.evidencias, 'resultados-bloqueios-semanais.json'), JSON.stringify({
      sha: config.targetSha, inicio, fim, axeScans: scans.length, scans, results }, null, 2));
    await browser.close();
    process.stdout.write(`SUMMARY SEMANAIS | ${results.filter(r => r.status === 'PASS').length} aprovadas | ${results.filter(r => r.status === 'FAIL').length} reprovadas\n`);
  }
})();
