// Capturas e verificacoes de layout da aplicacao REAL, somente leitura.
// Deve rodar depois de jornada-quatro-perfis.cjs, que cria os usuarios
// sinteticos e os registros usados aqui. Este script apenas faz login e
// abre paginas (GET); nao envia formularios que alterem dados.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { verificarAmbienteIsolado } = require('./seguranca-ambiente.cjs');

const BASE = process.env.COURTTIME_E2E_BASE_URL || 'http://127.0.0.1:18081';
verificarAmbienteIsolado({
  baseUrl: BASE,
  consentimento: process.env.COURTTIME_E2E_ACK_ISOLATED_DB,
  nomeBanco: process.env.COURTTIME_E2E_DATABASE_NAME,
});
const EVIDENCE = process.env.COURTTIME_E2E_EVIDENCE_DIR || '/tmp/courttime-banca-evidence';
const TARGET_SHA = process.env.COURTTIME_E2E_TARGET_SHA || 'nao-informado';
const CHROMIUM_EXECUTABLE = process.env.COURTTIME_E2E_CHROMIUM_PATH;
const PASSWORD = process.env.COURTTIME_E2E_PASSWORD;
if (!PASSWORD) throw new Error('Defina COURTTIME_E2E_PASSWORD somente no ambiente da execução.');
const PASTA = path.join(EVIDENCE, 'telas');
fs.mkdirSync(PASTA, { recursive: true });

const CONTAS = {
  aluno: 'aluno.a@exemplo.local',
  secretaria: 'secretaria.banca@exemplo.local',
  administrador: 'admin.banca@exemplo.local',
  seguranca: 'seguranca.banca@exemplo.local',
};
const VIEWPORTS = [
  { width: 390, height: 844 },
  { width: 430, height: 932 },
  { width: 768, height: 1024 },
  { width: 1440, height: 1000 },
];

let ids = {};
const jornada = path.join(EVIDENCE, 'resultados-e2e.json');
if (fs.existsSync(jornada)) ids = JSON.parse(fs.readFileSync(jornada, 'utf8')).ids || {};
const comId = (modelo, chave) => (ids[chave] ? modelo.replace('{id}', ids[chave]) : null);

const PAGINAS = {
  publico: [['pagina-inicial', '/'], ['login', '/login'], ['login-erro', '/login?erro'], ['login-saida', '/login?logout']],
  aluno: [
    ['inicio', '/inicio'], ['quadra', '/quadra'], ['nova-solicitacao', '/solicitacoes/nova'], ['solicitacoes', '/solicitacoes'],
    ['detalhe', comId('/solicitacoes/{id}', 'requestA')],
    ['participantes', comId('/solicitacoes/{id}/participantes', 'requestA')],
    ['documentos', comId('/solicitacoes/{id}/documentos', 'requestA')],
  ],
  administrador: [
    ['inicio', '/inicio'], ['painel', '/admin'], ['solicitacoes', '/admin/solicitacoes'],
    ['analise', comId('/admin/solicitacoes/{id}', 'requestB')],
    ['usuarios', '/admin/usuarios'], ['horarios', '/admin/horarios'], ['bloqueios', '/admin/bloqueios'],
  ],
  secretaria: [
    ['inicio', '/inicio'], ['documentos', '/secretaria/documentos'], ['contribuicoes', '/secretaria/contribuicoes'],
    ['documento-validado', comId('/secretaria/documentos/{id}', 'documentA2')],
    ['documento-devolvido', comId('/secretaria/documentos/{id}', 'documentA1')],
    ['contribuicao', comId('/secretaria/contribuicoes/solicitacoes/{id}', 'requestA')],
  ],
  seguranca: [['inicio', '/inicio'], ['reservas', '/seguranca/reservas'], ['quadra', '/quadra']],
};

const resultados = [];
function registrar(status, nome, detalhe = '') {
  resultados.push({ status, nome, detalhe });
  process.stdout.write(`${status} | ${nome}${detalhe ? ` | ${detalhe}` : ''}\n`);
  // No GitHub Actions, falhas viram anotacoes legiveis pela API da execucao.
  if (status === 'FAIL' && process.env.GITHUB_ACTIONS === 'true') {
    process.stdout.write(`::error title=Layout::${nome}${detalhe ? ` (${detalhe})` : ''}\n`);
  }
}

async function entrar(page, email) {
  await page.goto(`${BASE}/login`);
  await page.locator('#email').fill(email);
  await page.locator('#senha').fill(PASSWORD);
  await Promise.all([
    page.waitForURL(url => url.pathname === '/inicio'),
    page.getByRole('button', { name: 'Entrar' }).click(),
  ]);
}

async function auditarPagina(page) {
  return page.evaluate(() => {
    const logout = [...document.querySelectorAll('form')].find(f => (f.getAttribute('action') || '').endsWith('/logout'));
    return {
      overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
      h1: document.querySelectorAll('h1').length,
      logoutPost: logout ? (logout.method || '').toLowerCase() === 'post' && !!logout.querySelector('input[name="_csrf"]') : null,
    };
  });
}

async function verificarMenuMovel(page, perfil, viewport) {
  const rotulo = `menu móvel ${perfil} ${viewport.width}px`;
  const botao = page.getByRole('button', { name: 'Menu' });
  if (!(await botao.count()) || !(await botao.first().isVisible())) {
    registrar('INFO', rotulo, 'botão Menu ausente nesta versão');
    return;
  }
  await botao.first().focus();
  await page.keyboard.press('Enter');
  await page.waitForTimeout(300);
  const aberto = await botao.first().getAttribute('aria-expanded');
  const dentro = async () => page.evaluate(() => {
    const menu = document.getElementById(document.querySelector('[aria-controls][aria-expanded]')?.getAttribute('aria-controls'));
    return !!menu && menu.contains(document.activeElement);
  });
  let escapou = 0;
  if (!(await dentro())) escapou++;
  for (let i = 0; i < 12; i++) { await page.keyboard.press('Tab'); if (!(await dentro())) escapou++; }
  for (let i = 0; i < 12; i++) { await page.keyboard.press('Shift+Tab'); if (!(await dentro())) escapou++; }
  await page.screenshot({ path: path.join(PASTA, `${perfil}-menu-aberto-${viewport.width}.jpg`), type: 'jpeg', quality: 72 });
  await page.keyboard.press('Escape');
  await page.waitForTimeout(300);
  const fechado = await botao.first().getAttribute('aria-expanded');
  const focoNoBotao = await page.evaluate(() => !!document.activeElement && document.activeElement.hasAttribute('aria-expanded') && document.activeElement.textContent.includes('Menu'));
  const ok = aberto === 'true' && escapou === 0 && fechado === 'false' && focoNoBotao;
  registrar(ok ? 'PASS' : 'FAIL', rotulo, `aberto=${aberto} foraDoMenu=${escapou} fechadoComEsc=${fechado === 'false'} focoRestaurado=${focoNoBotao}`);
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(CHROMIUM_EXECUTABLE ? { executablePath: CHROMIUM_EXECUTABLE } : {}),
    args: ['--no-sandbox'],
  });
  try {
    for (const [perfil, paginas] of Object.entries(PAGINAS)) {
      for (const viewport of VIEWPORTS) {
        const context = await browser.newContext({ viewport, locale: 'pt-BR' });
        const page = await context.newPage();
        if (perfil !== 'publico') await entrar(page, CONTAS[perfil]);
        for (const [nome, url] of paginas) {
          if (!url) { registrar('INFO', `${perfil} ${nome}`, 'sem identificador da jornada'); continue; }
          await page.goto(`${BASE}${url}`);
          const a = await auditarPagina(page);
          const rotulo = `${perfil} ${nome} ${viewport.width}px`;
          registrar(a.overflow ? 'FAIL' : 'PASS', `sem rolagem horizontal — ${rotulo}`);
          registrar(a.h1 === 1 ? 'PASS' : 'FAIL', `um h1 — ${rotulo}`, `h1=${a.h1}`);
          if (perfil !== 'publico') registrar(a.logoutPost ? 'PASS' : 'FAIL', `logout POST com CSRF — ${rotulo}`);
          await page.screenshot({ path: path.join(PASTA, `${perfil}-${nome}-${viewport.width}.jpg`), fullPage: true, type: 'jpeg', quality: 72 });
        }
        if (perfil !== 'publico' && viewport.width < 1025) {
          await page.goto(`${BASE}/inicio`);
          await verificarMenuMovel(page, perfil, viewport);
        }
        await context.close();
      }
    }

    // Sem JavaScript: a navegacao do perfil precisa continuar acessivel.
    for (const perfil of ['aluno', 'administrador', 'secretaria', 'seguranca']) {
      const context = await browser.newContext({ viewport: { width: 390, height: 844 }, javaScriptEnabled: false, locale: 'pt-BR' });
      const page = await context.newPage();
      await entrar(page, CONTAS[perfil]);
      const links = await page.evaluate(() => [...document.querySelectorAll('nav a[href]')]
        .filter(a => a.offsetParent !== null).map(a => a.getAttribute('href')));
      registrar(links.length ? 'PASS' : 'FAIL', `navegação sem JavaScript — ${perfil} 390px`, links.join(' '));
      await page.screenshot({ path: path.join(PASTA, `${perfil}-inicio-sem-js-390.jpg`), fullPage: true, type: 'jpeg', quality: 72 });
      await context.close();
    }

    // Zoom de 200% e 400% (viewport de 1440 px reduzido pela escala).
    for (const [rotulo, viewport, escala] of [['zoom200', { width: 720, height: 500 }, 2], ['zoom400', { width: 360, height: 250 }, 4]]) {
      const context = await browser.newContext({ viewport, deviceScaleFactor: escala, locale: 'pt-BR' });
      const page = await context.newPage();
      await entrar(page, CONTAS.aluno);
      for (const url of ['/inicio', '/quadra', '/solicitacoes/nova']) {
        await page.goto(`${BASE}${url}`);
        const a = await auditarPagina(page);
        registrar(a.overflow ? 'FAIL' : 'PASS', `${rotulo} sem rolagem horizontal — aluno ${url}`);
        await page.screenshot({ path: path.join(PASTA, `aluno-${rotulo}${url.replace(/\//g, '-')}.jpg`), type: 'jpeg', quality: 72 });
      }
      await context.close();
    }
  } finally {
    await browser.close();
  }
  fs.writeFileSync(path.join(EVIDENCE, 'resultados-telas.json'), JSON.stringify({ sha: TARGET_SHA, resultados }, null, 2));
  const falhas = resultados.filter(r => r.status === 'FAIL').length;
  const resumo = `${resultados.filter(r => r.status === 'PASS').length} aprovadas | ${falhas} reprovadas`;
  process.stdout.write(`SUMMARY | ${resumo}\n`);
  if (process.env.GITHUB_ACTIONS === 'true') process.stdout.write(`::notice title=Capturas de layout::${resumo}\n`);
  process.exitCode = falhas ? 1 : 0;
})();
