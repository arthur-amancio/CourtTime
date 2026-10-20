const fs = require('fs');
const { chromium } = require('playwright');

const { carregarConfiguracao, caminhoEvidencia } = require('./suporte/configuracao.cjs');
const { autenticar } = require('./suporte/sessoes.cjs');
const { Relatorio } = require('./suporte/relatorio.cjs');

const configuracao = carregarConfiguracao();
const relatorio = new Relatorio('Interacoes, responsividade e acessibilidade do redesign V2', configuracao.targetSha);
const resultadosJornada = process.env.COURTTIME_E2E_RESULTS
  || caminhoEvidencia(configuracao, 'resultados-e2e.json');
if (!fs.existsSync(resultadosJornada)) {
  throw new Error(`Execute primeiro a jornada funcional. Resultado nao encontrado: ${resultadosJornada}`);
}
const dados = JSON.parse(fs.readFileSync(resultadosJornada, 'utf8'));
const dataControlada = dados.dates?.requestDate;
if (!/^\d{4}-\d{2}-\d{2}$/.test(dataControlada || '')) {
  throw new Error('A jornada nao registrou uma data controlada valida.');
}

const telas = [
  { perfil: 'alunoA', nome: 'aluno-calendario', rota: `/quadra?data=${dataControlada}` },
  { perfil: 'alunoA', nome: 'aluno-nova-solicitacao', rota: `/solicitacoes/nova?data=${dataControlada}` },
  { perfil: 'admin', nome: 'administrador-painel', rota: '/admin' },
  { perfil: 'secretaria', nome: 'secretaria-documentos', rota: '/secretaria/documentos' },
  { perfil: 'seguranca', nome: 'seguranca-reservas', rota: '/seguranca/reservas' },
];
const viewports = [
  { nome: '390', width: 390, height: 844, tipo: 'viewport' },
  { nome: '430', width: 430, height: 932, tipo: 'viewport' },
  { nome: '768', width: 768, height: 1024, tipo: 'viewport' },
  { nome: '1440', width: 1440, height: 1000, tipo: 'viewport' },
  { nome: 'zoom-200', width: 720, height: 500, tipo: 'reflow equivalente a 200% sobre 1440px' },
  { nome: 'zoom-400', width: 360, height: 500, tipo: 'reflow equivalente a 400% sobre 1440px' },
];

async function auditarEstrutura(page, tela, viewport) {
  const resultado = await page.evaluate(() => {
    const visivel = elemento => {
      const estilo = getComputedStyle(elemento);
      const caixa = elemento.getBoundingClientRect();
      return estilo.visibility !== 'hidden' && estilo.display !== 'none' && caixa.width > 0 && caixa.height > 0;
    };
    const controles = [...document.querySelectorAll('input:not([type="hidden"]), select, textarea')].filter(visivel);
    const semRotulo = controles.filter(elemento => {
      const rotuloExplicito = elemento.id && document.querySelector(`label[for="${CSS.escape(elemento.id)}"]`);
      return !rotuloExplicito
        && !elemento.closest('label')
        && !elemento.getAttribute('aria-label')
        && !elemento.getAttribute('aria-labelledby');
    }).map(elemento => `${elemento.tagName.toLowerCase()}#${elemento.id}`);
    return {
      overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
      h1: document.querySelectorAll('h1').length,
      idioma: document.documentElement.lang,
      semRotulo,
      tabindexPositivo: [...document.querySelectorAll('[tabindex]')]
        .filter(elemento => Number(elemento.getAttribute('tabindex')) > 0)
        .map(elemento => elemento.outerHTML.slice(0, 120)),
    };
  });
  const sufixo = `${tela.nome} ${viewport.nome}`;
  relatorio.conferir(!resultado.overflow, `Sem overflow horizontal: ${sufixo}`, viewport.tipo, { tela: tela.nome, viewport });
  relatorio.conferir(resultado.h1 === 1, `Um H1 principal: ${sufixo}`, `h1=${resultado.h1}`, { tela: tela.nome, viewport });
  relatorio.conferir(resultado.idioma === 'pt-BR', `Idioma da pagina: ${sufixo}`, `lang=${resultado.idioma}`, { tela: tela.nome, viewport });
  relatorio.conferir(resultado.semRotulo.length === 0, `Controles rotulados: ${sufixo}`, JSON.stringify(resultado.semRotulo), { tela: tela.nome, viewport });
  relatorio.conferir(resultado.tabindexPositivo.length === 0, `Ordem natural de foco: ${sufixo}`, JSON.stringify(resultado.tabindexPositivo), { tela: tela.nome, viewport });
}

async function auditarMenu(page, tela) {
  const botao = page.getByRole('button', { name: /^menu$/i });
  if (await botao.count() === 0) {
    relatorio.pendente(`Menu V2: ${tela.nome}`, 'Fase B ainda nao detectada nesta tela.', { tela: tela.nome });
    return;
  }
  relatorio.conferir(await botao.getAttribute('aria-expanded') === 'false', `Menu fechado declara aria-expanded: ${tela.nome}`);
  const idPainel = await botao.getAttribute('aria-controls');
  relatorio.conferir(Boolean(idPainel), `Menu referencia painel: ${tela.nome}`, `aria-controls=${idPainel}`);
  await botao.focus();
  await page.keyboard.press('Enter');
  relatorio.conferir(await botao.getAttribute('aria-expanded') === 'true', `Menu abre pelo teclado: ${tela.nome}`);
  const painel = page.locator(`#${idPainel}`);
  relatorio.conferir(await painel.count() === 1 && await painel.isVisible(), `Painel do menu visivel: ${tela.nome}`);
  relatorio.conferir(await page.locator('[inert]').count() > 0, `Fundo inerte com menu aberto: ${tela.nome}`);

  for (const tecla of ['Tab', 'Shift+Tab']) {
    await page.keyboard.press(tecla);
    const focoDentro = await page.evaluate(id => document.querySelector(`#${CSS.escape(id)}`)?.contains(document.activeElement), idPainel);
    relatorio.conferir(Boolean(focoDentro), `Foco contido no menu (${tecla}): ${tela.nome}`);
  }
  await page.keyboard.press('Escape');
  relatorio.conferir(await botao.getAttribute('aria-expanded') === 'false', `Escape fecha menu: ${tela.nome}`);
  relatorio.conferir(await botao.evaluate(elemento => elemento === document.activeElement), `Foco restaurado ao botao Menu: ${tela.nome}`);
}

async function auditarSequenciaDeFoco(page, tela) {
  await page.locator('body').evaluate(elemento => {
    elemento.setAttribute('tabindex', '-1');
    elemento.focus();
  });
  const itens = [];
  const problemas = [];
  for (let indice = 0; indice < 16; indice += 1) {
    await page.keyboard.press('Tab');
    const estado = await page.evaluate(() => {
      const ativo = document.activeElement;
      if (!ativo || ativo === document.body) return null;
      const caixa = ativo.getBoundingClientRect();
      const centroX = Math.max(0, Math.min(innerWidth - 1, caixa.left + caixa.width / 2));
      const centroY = Math.max(0, Math.min(innerHeight - 1, caixa.top + caixa.height / 2));
      const sobreposto = document.elementsFromPoint(centroX, centroY).some(elemento => {
        if (elemento === ativo || elemento.contains(ativo) || ativo.contains(elemento)) return false;
        const posicao = getComputedStyle(elemento).position;
        return posicao === 'fixed' || posicao === 'sticky';
      });
      return {
        identificador: ativo.getAttribute('aria-label') || ativo.textContent?.trim().slice(0, 80)
          || ativo.getAttribute('name') || ativo.id || ativo.tagName,
        dentroDaJanela: caixa.top >= -1 && caixa.bottom <= innerHeight + 1,
        sobreposto,
      };
    });
    if (!estado) break;
    itens.push(estado.identificador);
    if (!estado.dentroDaJanela || estado.sobreposto) problemas.push(estado);
  }
  relatorio.conferir(itens.length >= 2, `Sequencia de foco alcancavel: ${tela.nome}`, itens.join(' -> '));
  relatorio.conferir(problemas.length === 0, `Foco nao fica oculto por barra fixa: ${tela.nome}`, JSON.stringify(problemas));
  await page.evaluate(() => document.activeElement?.setAttribute('data-qa-foco-anterior', 'true'));
  await page.keyboard.press('Shift+Tab');
  const retrocedeu = await page.evaluate(() => {
    const anterior = document.querySelector('[data-qa-foco-anterior]');
    const mudou = document.activeElement !== anterior;
    anterior?.removeAttribute('data-qa-foco-anterior');
    return mudou;
  });
  relatorio.conferir(retrocedeu, `Shift+Tab retrocede o foco: ${tela.nome}`);
}

async function auditarCalendario(page) {
  const calendario = page.locator('[role="grid"], [aria-label*="calend" i], [data-calendar]').first();
  if (await calendario.count() === 0) {
    relatorio.pendente('Calendario V2 navegavel por teclado', 'Componente da Fase D ainda nao detectado.');
    return;
  }
  const texto = await calendario.innerText();
  relatorio.conferir(!/\d+\s+hor.rios?\s+dispon.ve/i.test(texto), 'Calendario opcao A sem contadores por dia');
  const focoAntes = await page.evaluate(() => document.activeElement?.outerHTML);
  await calendario.focus();
  await page.keyboard.press('ArrowRight');
  const focoDepois = await page.evaluate(() => document.activeElement?.outerHTML);
  relatorio.conferir(focoAntes !== focoDepois, 'Calendario responde a navegacao por teclado');
  const passadasHabilitadas = await calendario.locator('[data-date]').evaluateAll(elementos => {
    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);
    return elementos.filter(elemento => {
      const data = new Date(`${elemento.getAttribute('data-date')}T12:00:00`);
      const desabilitado = elemento.matches(':disabled,[aria-disabled="true"]');
      return data < hoje && !desabilitado;
    }).map(elemento => elemento.getAttribute('data-date'));
  });
  relatorio.conferir(passadasHabilitadas.length === 0, 'Calendario nao habilita datas passadas', JSON.stringify(passadasHabilitadas));
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(configuracao.chromiumExecutable ? { executablePath: configuracao.chromiumExecutable } : {}),
    args: ['--no-sandbox'],
  });
  try {
    for (const tela of telas) {
      const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
      const page = await context.newPage();
      await autenticar(page, configuracao, tela.perfil);
      for (const viewport of viewports) {
        await page.setViewportSize({ width: viewport.width, height: viewport.height });
        await page.goto(`${configuracao.baseUrl}${tela.rota}`);
        await auditarEstrutura(page, tela, viewport);
        await page.screenshot({
          path: caminhoEvidencia(configuracao, `${tela.nome}-${viewport.nome}.png`),
          fullPage: true,
        });
      }
      await page.setViewportSize({ width: 390, height: 844 });
      await page.goto(`${configuracao.baseUrl}${tela.rota}`);
      await auditarSequenciaDeFoco(page, tela);
      await auditarMenu(page, tela);
      if (tela.nome === 'aluno-calendario') await auditarCalendario(page);
      await context.close();
    }
    const contagem = relatorio.salvar(caminhoEvidencia(configuracao, 'resultados-redesign-v2.json'), {
      notaZoom: 'Zoom avaliado pelo equivalente de reflow WCAG: viewport CSS de 720px (200%) e 360px (400%) sobre referencia de 1440px.',
    });
    process.stdout.write(`SUMMARY | ${contagem.pass} aprovadas | ${contagem.fail} reprovadas | ${contagem.pending} pendentes\n`);
    process.exitCode = contagem.fail ? 1 : 0;
  } catch (error) {
    relatorio.registrar('FAIL', 'Execucao interrompida', error.message);
    relatorio.salvar(caminhoEvidencia(configuracao, 'resultados-redesign-v2-parcial.json'), { erro: error.stack });
    process.stderr.write(`${error.stack}\n`);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();
