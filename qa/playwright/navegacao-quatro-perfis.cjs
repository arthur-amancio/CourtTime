const fs = require('fs');
const { chromium } = require('playwright');

const { carregarConfiguracao, caminhoEvidencia } = require('./suporte/configuracao.cjs');
const { criarSessao, postSemCsrf, sairPorPost } = require('./suporte/sessoes.cjs');
const { Relatorio } = require('./suporte/relatorio.cjs');

const configuracao = carregarConfiguracao();
const relatorio = new Relatorio('Contratos de navegacao dos quatro perfis', configuracao.targetSha);
const resultadosJornada = process.env.COURTTIME_E2E_RESULTS
  || caminhoEvidencia(configuracao, 'resultados-e2e.json');

if (!fs.existsSync(resultadosJornada)) {
  throw new Error(`Execute primeiro a jornada funcional. Resultado nao encontrado: ${resultadosJornada}`);
}
const dados = JSON.parse(fs.readFileSync(resultadosJornada, 'utf8'));
const { requestA, requestB, approvedId } = dados.ids || {};
if (![requestA, requestB, approvedId].every(Number.isInteger)) {
  throw new Error('O resultado da jornada nao contem os identificadores necessarios.');
}

async function verificarLink(page, rota, nome, perfil) {
  const link = page.locator(`a[href="${rota}"]`).filter({ hasText: nome }).first();
  relatorio.verificar(await link.count() === 1, `${perfil}: navegacao ${rota}`, `link ${nome}`);
}

async function verificarRecusa(page, rota, perfil) {
  const resposta = await page.goto(`${configuracao.baseUrl}${rota}`);
  relatorio.verificar(
    resposta?.status() === 403,
    `${perfil}: backend recusa rota de outro perfil`,
    `${rota} -> HTTP ${resposta?.status()}`,
    { perfil, rota }
  );
}

async function verificarLogout(page, perfil) {
  await page.goto(`${configuracao.baseUrl}/inicio`);
  const semCsrf = await postSemCsrf(page, '/logout');
  relatorio.verificar(semCsrf.status === 403, `${perfil}: logout sem CSRF recusado`, `HTTP ${semCsrf.status}`);

  const getLogout = await page.goto(`${configuracao.baseUrl}/logout`);
  relatorio.verificar(
    getLogout?.status() === 404 || getLogout?.status() === 405,
    `${perfil}: logout por GET nao encerra a sessao`,
    `HTTP ${getLogout?.status()}`
  );
  await page.goto(`${configuracao.baseUrl}/inicio`);
  relatorio.verificar(new URL(page.url()).pathname === '/inicio', `${perfil}: sessao preservada apos GET /logout`);

  await sairPorPost(page);
  relatorio.registrar('PASS', `${perfil}: logout POST com token CSRF`);
  await page.goto(`${configuracao.baseUrl}/inicio`);
  relatorio.verificar(
    new URL(page.url()).pathname === '/login',
    `${perfil}: sessao encerrada nao acessa area autenticada`,
    page.url()
  );
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(configuracao.chromiumExecutable ? { executablePath: configuracao.chromiumExecutable } : {}),
    args: ['--no-sandbox'],
  });
  const contextos = [];
  try {
    const aluno = await criarSessao(browser, configuracao, 'alunoA');
    contextos.push(aluno.context);
    await aluno.page.goto(`${configuracao.baseUrl}/inicio`);
    await verificarLink(aluno.page, '/quadra', /Acessar|Quadra|Calend/i, 'ALUNO');
    await verificarLink(aluno.page, '/solicitacoes/nova', /Solicitar|Nova/i, 'ALUNO');
    await verificarLink(aluno.page, '/solicitacoes', /Consultar|Minhas/i, 'ALUNO');
    await aluno.page.goto(`${configuracao.baseUrl}/solicitacoes/${requestA}`);
    await verificarLink(aluno.page, `/solicitacoes/${requestA}/participantes`, /Participantes/i, 'ALUNO');
    await verificarLink(aluno.page, `/solicitacoes/${requestA}/documentos`, /Documento/i, 'ALUNO');
    let resposta = await aluno.page.goto(`${configuracao.baseUrl}/solicitacoes/${requestB}`);
    relatorio.verificar(resposta?.status() === 404, 'ALUNO: recurso de outro proprietario ocultado', `HTTP ${resposta?.status()}`);
    for (const rota of ['/admin', '/secretaria/documentos', '/seguranca/reservas']) {
      await verificarRecusa(aluno.page, rota, 'ALUNO');
    }
    await verificarLogout(aluno.page, 'ALUNO');

    const admin = await criarSessao(browser, configuracao, 'admin');
    contextos.push(admin.context);
    await admin.page.goto(`${configuracao.baseUrl}/admin`);
    await verificarLink(admin.page, '/admin/solicitacoes', /solicita/i, 'ADMINISTRADOR');
    await verificarLink(admin.page, '/admin/usuarios', /usu.rio/i, 'ADMINISTRADOR');
    await verificarLink(admin.page, '/admin/horarios', /Hor.rios semanais/i, 'ADMINISTRADOR');
    await verificarLink(admin.page, '/admin/bloqueios', /Bloqueios/i, 'ADMINISTRADOR');
    for (const rota of ['/secretaria/documentos', '/secretaria/contribuicoes', '/seguranca/reservas']) {
      await verificarRecusa(admin.page, rota, 'ADMINISTRADOR');
    }
    await verificarLogout(admin.page, 'ADMINISTRADOR');

    const secretaria = await criarSessao(browser, configuracao, 'secretaria');
    contextos.push(secretaria.context);
    await secretaria.page.goto(`${configuracao.baseUrl}/inicio`);
    await verificarLink(secretaria.page, '/secretaria/documentos', /fila|document/i, 'SECRETARIA');
    await verificarLink(secretaria.page, '/secretaria/contribuicoes', /contribui/i, 'SECRETARIA');
    for (const rota of ['/admin', '/admin/solicitacoes', '/seguranca/reservas', '/solicitacoes']) {
      await verificarRecusa(secretaria.page, rota, 'SECRETARIA');
    }
    await verificarLogout(secretaria.page, 'SECRETARIA');

    const seguranca = await criarSessao(browser, configuracao, 'seguranca');
    contextos.push(seguranca.context);
    await seguranca.page.goto(`${configuracao.baseUrl}/inicio`);
    await verificarLink(seguranca.page, '/seguranca/reservas', /reservas/i, 'SEGURANCA');
    const corpoInicio = await seguranca.page.locator('body').innerText();
    relatorio.verificar(!/Contribui..es|Gerenciar usu.rios|Bloqueios/.test(corpoInicio), 'SEGURANCA: sem links administrativos ou financeiros');
    resposta = await seguranca.page.goto(`${configuracao.baseUrl}/seguranca/reservas/${approvedId}`);
    relatorio.verificar(resposta?.status() === 404, 'SEGURANCA: reserva cancelada nao e autorizacao', `HTTP ${resposta?.status()}`);
    for (const rota of ['/admin', '/secretaria/documentos', '/secretaria/contribuicoes', '/solicitacoes']) {
      await verificarRecusa(seguranca.page, rota, 'SEGURANCA');
    }
    await verificarLogout(seguranca.page, 'SEGURANCA');

    const contagem = relatorio.salvar(caminhoEvidencia(configuracao, 'resultados-navegacao.json'), {
      fonteIds: resultadosJornada,
    });
    process.stdout.write(`SUMMARY | ${contagem.pass} aprovadas | ${contagem.fail} reprovadas | ${contagem.pending} pendentes\n`);
  } catch (error) {
    relatorio.registrar('FAIL', 'Execucao interrompida', error.message);
    relatorio.salvar(caminhoEvidencia(configuracao, 'resultados-navegacao-parcial.json'), { erro: error.stack });
    process.stderr.write(`${error.stack}\n`);
    process.exitCode = 1;
  } finally {
    await Promise.allSettled(contextos.map(contexto => contexto.close()));
    await browser.close();
  }
})();
