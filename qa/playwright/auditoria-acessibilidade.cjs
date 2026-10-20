const { chromium } = require('playwright');
const fs = require('fs');

const BASE = process.env.COURTTIME_E2E_BASE_URL || 'http://127.0.0.1:18081';
const AXE = require.resolve('axe-core/axe.min.js');
const EVIDENCE = process.env.COURTTIME_E2E_EVIDENCE_DIR || '/tmp/courttime-banca-evidence';
const TARGET_SHA = process.env.COURTTIME_E2E_TARGET_SHA || 'nao-informado';
const CHROMIUM_EXECUTABLE = process.env.COURTTIME_E2E_CHROMIUM_PATH;
const PASSWORD = process.env.COURTTIME_E2E_PASSWORD;
if (!PASSWORD) throw new Error('Defina COURTTIME_E2E_PASSWORD somente no ambiente da execução.');
fs.mkdirSync(EVIDENCE, { recursive: true });

async function login(page, email) {
  await page.goto(`${BASE}/login`);
  await page.locator('#email').fill(email);
  await page.locator('#senha').fill(PASSWORD);
  await Promise.all([
    page.waitForURL(url => url.pathname === '/inicio'),
    page.getByRole('button', { name: 'Entrar' }).click(),
  ]);
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(CHROMIUM_EXECUTABLE ? { executablePath: CHROMIUM_EXECUTABLE } : {}),
    args: ['--no-sandbox']
  });
  const scans = [];
  async function scan(name, url, email, viewport) {
    const context = await browser.newContext({ viewport });
    const page = await context.newPage();
    if (email) await login(page, email);
    await page.goto(`${BASE}${url}`);
    await page.addScriptTag({ path: AXE });
    const result = await page.evaluate(async () => axe.run(document, {
      runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] }
    }));
    const violations = result.violations.map(v => ({
      id: v.id,
      impact: v.impact,
      help: v.help,
      nodes: v.nodes.map(n => n.target)
    }));
    scans.push({ name, url, viewport, violations });
    process.stdout.write(`${violations.length ? 'WARN' : 'PASS'} | axe ${name} ${viewport.width}px | ${violations.length} violações\n`);
    for (const violation of violations) {
      process.stdout.write(`  ${violation.impact} ${violation.id}: ${violation.help} (${violation.nodes.length} nós)\n`);
    }
    await context.close();
  }

  try {
    const viewports = [
      { width: 390, height: 844 },
      { width: 430, height: 932 },
      { width: 768, height: 1024 },
      { width: 1440, height: 1000 },
    ];
    const paginas = [
      ['login', '/login', null],
      ['aluno', '/solicitacoes', 'aluno.a@exemplo.local'],
      ['quadra', '/quadra', 'aluno.a@exemplo.local'],
      ['secretaria', '/secretaria/contribuicoes', 'secretaria.banca@exemplo.local'],
      ['administracao', '/admin/solicitacoes', 'admin.banca@exemplo.local'],
      ['seguranca', '/seguranca/reservas', 'seguranca.banca@exemplo.local'],
    ];
    const resultadoJornada = `${EVIDENCE}/resultados-e2e.json`;
    if (fs.existsSync(resultadoJornada)) {
      const { ids } = JSON.parse(fs.readFileSync(resultadoJornada, 'utf8'));
      paginas.push(
        ['participantes', `/solicitacoes/${ids.requestA}/participantes`, 'aluno.a@exemplo.local'],
        ['documentos', `/solicitacoes/${ids.requestA}/documentos`, 'aluno.a@exemplo.local'],
        ['conferencia-documental', `/secretaria/documentos/${ids.documentA2}`, 'secretaria.banca@exemplo.local'],
        ['analise-administrativa', `/admin/solicitacoes/${ids.approvedId}`, 'admin.banca@exemplo.local'],
      );
    }
    for (const [name, url, email] of paginas) {
      for (const viewport of viewports) await scan(name, url, email, viewport);
    }
    fs.writeFileSync(`${EVIDENCE}/resultados-axe.json`, JSON.stringify({ sha: TARGET_SHA, scans }, null, 2));
    const total = scans.reduce((sum, scan) => sum + scan.violations.length, 0);
    const serious = scans.flatMap(scan => scan.violations).filter(v => ['serious', 'critical'].includes(v.impact)).length;
    process.stdout.write(`SUMMARY | ${scans.length} varreduras | ${total} violações | ${serious} sérias/críticas\n`);
    process.exitCode = serious ? 1 : 0;
  } finally {
    await browser.close();
  }
})();
