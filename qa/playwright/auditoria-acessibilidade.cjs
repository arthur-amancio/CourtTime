const { chromium } = require('playwright');
const fs = require('fs');

const { carregarConfiguracao, caminhoEvidencia } = require('./suporte/configuracao.cjs');
const { autenticar } = require('./suporte/sessoes.cjs');

const configuracao = carregarConfiguracao();
const BASE = configuracao.baseUrl;
const AXE = require.resolve('axe-core/axe.min.js');
const EVIDENCE = configuracao.evidencias;
const TARGET_SHA = configuracao.targetSha;
const CHROMIUM_EXECUTABLE = configuracao.chromiumExecutable;

(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(CHROMIUM_EXECUTABLE ? { executablePath: CHROMIUM_EXECUTABLE } : {}),
    args: ['--no-sandbox']
  });
  const scans = [];
  async function scan(name, url, perfil, viewport) {
    const context = await browser.newContext({ viewport });
    const page = await context.newPage();
    if (perfil) await autenticar(page, configuracao, perfil);
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
      ['pagina-publica', '/', null],
      ['login', '/login', null],
      ['aluno', '/solicitacoes', 'alunoA'],
      ['quadra', '/quadra', 'alunoA'],
      ['secretaria', '/secretaria/contribuicoes', 'secretaria'],
      ['administracao', '/admin/solicitacoes', 'admin'],
      ['seguranca', '/seguranca/reservas', 'seguranca'],
    ];
    const resultadoJornada = `${EVIDENCE}/resultados-e2e.json`;
    if (fs.existsSync(resultadoJornada)) {
      const { ids } = JSON.parse(fs.readFileSync(resultadoJornada, 'utf8'));
      paginas.push(
        ['participantes', `/solicitacoes/${ids.requestA}/participantes`, 'alunoA'],
        ['documentos', `/solicitacoes/${ids.requestA}/documentos`, 'alunoA'],
        ['conferencia-documental', `/secretaria/documentos/${ids.documentA2}`, 'secretaria'],
        ['analise-administrativa', `/admin/solicitacoes/${ids.approvedId}`, 'admin'],
      );
    }
    for (const [name, url, email] of paginas) {
      for (const viewport of viewports) await scan(name, url, email, viewport);
    }
    fs.writeFileSync(caminhoEvidencia(configuracao, 'resultados-axe.json'), JSON.stringify({ sha: TARGET_SHA, scans }, null, 2));
    const total = scans.reduce((sum, scan) => sum + scan.violations.length, 0);
    const serious = scans.flatMap(scan => scan.violations).filter(v => ['serious', 'critical'].includes(v.impact)).length;
    process.stdout.write(`SUMMARY | ${scans.length} varreduras | ${total} violações | ${serious} sérias/críticas\n`);
    process.exitCode = total ? 1 : 0;
  } finally {
    await browser.close();
  }
})();
