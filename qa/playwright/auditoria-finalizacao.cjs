// Sondas complementares da finalização do TCC, executadas DEPOIS da jornada
// principal (usa a grade e as contas de secretaria e segurança criadas por ela).
// Cobre o que a jornada não exercita: edição e remoção de participantes,
// cabeçalhos e autorização do PDF oficial, mais de 30 participantes, regras da
// remarcação, matriz de acesso negado, histórico administrativo e mensagens.
// Usa somente o banco descartável courttime_e2e (verificado pela configuração).
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const { carregarConfiguracao } = require('./suporte/configuracao.cjs');
const { autenticar, postComCsrf, selecionarHorario } = require('./suporte/sessoes.cjs');

const configuracao = carregarConfiguracao();
const BASE = configuracao.baseUrl;
const EVIDENCE = configuracao.evidencias;
const SENHA = configuracao.senha;
const TOKEN = configuracao.marcadorInstancia;
const CHROMIUM_EXECUTABLE = configuracao.chromiumExecutable;

const results = [];
function pass(name, detail = '') {
  results.push({ status: 'PASS', name, detail });
  process.stdout.write(`PASS | ${name}${detail ? ` | ${detail}` : ''}\n`);
}
function check(condition, name, detail = '') {
  if (!condition) {
    results.push({ status: 'FAIL', name, detail });
    process.stdout.write(`FAIL | ${name}${detail ? ` | ${detail}` : ''}\n`);
    return false;
  }
  pass(name, detail);
  return true;
}
function exigir(condition, name, detail = '') {
  if (!check(condition, name, detail)) throw new Error(`Pré-condição falhou: ${name} ${detail}`);
}
const texto = html => html.replace(/<script[\s\S]*?<\/script>/g, ' ').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ');

function isoInSaoPaulo(offsetDays) {
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date()).filter(p => p.type !== 'literal').map(p => [p.type, p.value]));
  return new Date(Date.UTC(Number(parts.year), Number(parts.month) - 1, Number(parts.day) + offsetDays))
    .toISOString().slice(0, 10);
}
const dias = ['DOMINGO', 'SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO'];
function proxima(dia, minimo) {
  for (let d = minimo; d < minimo + 14; d += 1) {
    const iso = isoInSaoPaulo(d);
    if (dias[new Date(`${iso}T12:00:00Z`).getUTCDay()] === dia) return iso;
  }
  throw new Error(`sem data para ${dia}`);
}
const hoje = isoInSaoPaulo(0);
const dataR1 = proxima('QUARTA', 21);
const dataRemarcacao = proxima('QUINTA', 21);
const dataRejeicao = proxima('QUARTA', 28);
const dataGrande = proxima('QUINTA', 28);

const contas = {
  alunoC: `aluno.c.${TOKEN}@e2e.invalid`,
  alunoD: `aluno.d.${TOKEN}@e2e.invalid`,
};

async function entrar(browser, perfil, email, largura = 1440) {
  const context = await browser.newContext({ viewport: { width: largura, height: 1000 }, acceptDownloads: true });
  const page = await context.newPage();
  if (email) {
    await page.goto(`${BASE}/login`);
    await page.getByLabel(/e-?mail/i).fill(email);
    await page.getByLabel(/senha/i).fill(SENHA);
    await Promise.all([
      page.waitForURL(url => url.pathname === '/inicio'),
      page.getByRole('button', { name: 'Entrar' }).click(),
    ]);
  } else {
    await autenticar(page, configuracao, perfil);
  }
  return page;
}

async function criarConta(admin, nome, email) {
  await admin.goto(`${BASE}/admin/usuarios/novo`);
  await admin.locator('#nome').fill(nome);
  await admin.locator('#email').fill(email);
  await admin.locator('#perfil').selectOption('ALUNO');
  await admin.locator('#senha').fill(SENHA);
  await admin.locator('#confirmacaoSenha').fill(SENHA);
  await Promise.all([
    admin.waitForURL(url => url.pathname === '/admin/usuarios'),
    admin.getByRole('button', { name: 'Cadastrar usuário' }).click(),
  ]);
  exigir((await admin.locator('body').innerText()).includes('Usuário cadastrado com sucesso.'), `conta ${nome}`);
}

async function criarSolicitacao(page, data, hora, finalidade) {
  await page.goto(`${BASE}/solicitacoes/nova?data=${data}`);
  await selecionarHorario(page, hora);
  await page.locator('#finalidade').fill(finalidade);
  await Promise.all([
    page.waitForURL(url => /^\/solicitacoes\/\d+$/.test(url.pathname)),
    page.getByRole('button', { name: 'Registrar solicitação' }).click(),
  ]);
  return Number(page.url().match(/\/solicitacoes\/(\d+)$/)[1]);
}

// Inclusão rápida pela mesma rota POST do formulário, com o token CSRF da página.
async function incluirParticipantes(page, reservaId, quantidade, prefixo, categoria = 'ALUNO', inicio = 1) {
  await page.goto(`${BASE}/solicitacoes/${reservaId}/participantes/novo`);
  for (let i = inicio; i < inicio + quantidade; i += 1) {
    const r = await postComCsrf(page, `/solicitacoes/${reservaId}/participantes`, {
      nomeCompleto: `${prefixo} ${String(i).padStart(2, '0')}`,
      rmOuRg: `RM-F${reservaId}-${String(i).padStart(3, '0')}`,
      categoria,
    });
    if (r.status !== 200) throw new Error(`participante ${i}: HTTP ${r.status}`);
  }
}

async function enviarPdf(page, reservaId, nome, conteudo) {
  await page.goto(`${BASE}/solicitacoes/${reservaId}/documentos`);
  await page.locator('#arquivo').setInputFiles({ name: nome, mimeType: 'application/pdf', buffer: conteudo });
  await Promise.all([
    page.waitForLoadState('networkidle'),
    page.getByRole('button', { name: 'Enviar nova versão' }).click(),
  ]);
  const href = await page.getByRole('link', { name: 'Baixar original' }).first().getAttribute('href');
  return Number(href.match(/documentos\/(\d+)\/baixar/)[1]);
}

async function validarEContribuir(secretaria, documentoId, reservaId) {
  await secretaria.goto(`${BASE}/secretaria/documentos/${documentoId}`);
  await Promise.all([
    secretaria.waitForURL(url => url.pathname === '/secretaria/documentos'),
    secretaria.getByRole('button', { name: 'Validar documento' }).click(),
  ]);
  await secretaria.goto(`${BASE}/secretaria/contribuicoes`);
  const r = await postComCsrf(secretaria, `/secretaria/contribuicoes/solicitacoes/${reservaId}`, {
    valor: '22.00', observacoes: 'Recebimento fictício da auditoria',
  });
  exigir(texto(r.text).includes('Contribuição presencial registrada.'), `contribuição #${reservaId}`);
}

// Gera o formulário por fetch para inspecionar os cabeçalhos da resposta.
async function gerarFormulario(page, reservaId, campos, { comCsrf = true } = {}) {
  return page.evaluate(async ({ reservaId, campos, comCsrf }) => {
    const token = document.querySelector('input[name="_csrf"]')?.value;
    const corpo = new URLSearchParams(comCsrf ? { ...campos, _csrf: token } : campos);
    const resposta = await fetch(`/solicitacoes/${reservaId}/formulario-oficial`, {
      method: 'POST', body: corpo, redirect: 'follow',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });
    const bytes = new Uint8Array(await resposta.arrayBuffer());
    let binario = '';
    for (let i = 0; i < bytes.length; i += 1) binario += String.fromCharCode(bytes[i]);
    return {
      status: resposta.status,
      url: resposta.url,
      cabecalhos: Object.fromEntries(resposta.headers.entries()),
      base64: btoa(binario),
    };
  }, { reservaId, campos, comCsrf });
}

async function semRolagemHorizontal(page, rotulo, url) {
  for (const largura of [360, 390]) {
    await page.setViewportSize({ width: largura, height: 800 });
    await page.goto(url);
    const estoura = await page.evaluate(() => {
      const raiz = document.documentElement;
      const fora = [...document.querySelectorAll('main *')].filter(el => {
        const r = el.getBoundingClientRect();
        // Faixas com rolagem própria (como a dos dias no celular) são intencionais.
        let pai = el.parentElement;
        while (pai && pai !== document.body) {
          if (['auto', 'scroll', 'hidden', 'clip'].includes(getComputedStyle(pai).overflowX)) return false;
          pai = pai.parentElement;
        }
        return r.width > 0 && r.right > raiz.clientWidth + 1 && getComputedStyle(el).position !== 'fixed';
      }).slice(0, 3).map(el => `${el.tagName.toLowerCase()}.${[...el.classList].join('.')}`);
      return { rolagem: raiz.scrollWidth > raiz.clientWidth + 1, fora };
    });
    check(!estoura.rolagem && estoura.fora.length === 0, `celular ${largura}px sem conteúdo para fora: ${rotulo}`, JSON.stringify(estoura));
    await page.screenshot({ path: path.join(EVIDENCE, `final-${rotulo}-${largura}.png`), fullPage: true });
  }
  await page.setViewportSize({ width: 1440, height: 1000 });
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(CHROMIUM_EXECUTABLE ? { executablePath: CHROMIUM_EXECUTABLE } : {}),
    args: ['--no-sandbox'],
  });
  const ids = {};
  try {
    const pdfCtx = await browser.newContext();
    const pdfPage = await pdfCtx.newPage();
    await pdfPage.setContent('<!doctype html><html lang="pt-BR"><body><h1>Documento fictício da auditoria</h1></body></html>');
    const pdfFicticio = await pdfPage.pdf({ format: 'A4' });
    await pdfCtx.close();

    const admin = await entrar(browser, 'admin');
    await criarConta(admin, 'Carla Auditoria Exemplo', contas.alunoC);
    await criarConta(admin, 'Davi Auditoria Exemplo', contas.alunoD);
    const alunoC = await entrar(browser, null, contas.alunoC);
    const alunoD = await entrar(browser, null, contas.alunoD);
    const secretaria = await entrar(browser, 'secretaria');
    const seguranca = await entrar(browser, 'seguranca');
    const anonimo = await (await browser.newContext()).newPage();

    // ---------------------------------------------------------- acesso negado
    const matriz = [
      [alunoC, 'aluno', ['/admin', '/admin/solicitacoes', '/secretaria/documentos', '/secretaria/contribuicoes', '/seguranca/reservas']],
      [secretaria, 'secretaria', ['/admin', '/solicitacoes', '/seguranca/reservas']],
      [seguranca, 'segurança', ['/admin', '/solicitacoes', '/secretaria/documentos', '/secretaria/contribuicoes']],
      [admin, 'administrador', ['/solicitacoes', '/secretaria/documentos', '/seguranca/reservas']],
    ];
    for (const [page, perfil, rotas] of matriz) {
      for (const rota of rotas) {
        const r = await page.goto(`${BASE}${rota}`);
        check(r.status() === 403, `${perfil} não acessa ${rota}`, `HTTP ${r.status()}`);
      }
    }
    const pagina403 = await alunoC.locator('body').innerText();
    check(pagina403.includes('Acesso não permitido') && !/whitelabel|exception|stack/i.test(pagina403), 'página 403 em português e sem detalhes técnicos');
    for (const rota of ['/inicio', '/solicitacoes', '/admin', '/seguranca/reservas']) {
      await anonimo.goto(`${BASE}${rota}`);
      check(new URL(anonimo.url()).pathname === '/login', `visitante é levado ao login: ${rota}`);
    }
    const r404 = await alunoC.goto(`${BASE}/solicitacoes/999999`);
    const pagina404 = await alunoC.locator('body').innerText();
    check(r404.status() === 404 && pagina404.includes('Não encontrado') && !/whitelabel|exception/i.test(pagina404), 'página 404 em português');

    // ---------------------------------------------- participantes: CRUD real
    ids.r1 = await criarSolicitacao(alunoC, dataR1, '09:00', 'Treino fictício da auditoria');
    await incluirParticipantes(alunoC, ids.r1, 10, 'Aluno Auditoria');
    await alunoC.goto(`${BASE}/solicitacoes/${ids.r1}/participantes/novo`);
    await alunoC.locator('#nomeCompleto').fill('Responsável Auditoria Exemplo');
    await alunoC.locator('#rmOuRg').fill(`RM-F${ids.r1}-001`);
    await alunoC.locator('#categoria').selectOption('OUTRO');
    await alunoC.getByRole('button', { name: 'Salvar participante' }).click();
    check((await alunoC.locator('body').innerText()).includes('Já existe um participante com este RM ou RG'), 'RM ou RG repetido é recusado com mensagem');
    await alunoC.locator('#rmOuRg').fill('RG 00.000.000-0');
    await Promise.all([
      alunoC.waitForURL(url => url.pathname === `/solicitacoes/${ids.r1}/participantes`),
      alunoC.getByRole('button', { name: 'Salvar participante' }).click(),
    ]);
    let corpo = await alunoC.locator('body').innerText();
    check(corpo.includes('11\nTotal'), 'participante da categoria Outro incluído', '10 alunos + 1 outro');
    await alunoC.getByRole('link', { name: 'Editar Aluno Auditoria 01' }).click();
    await alunoC.locator('#nomeCompleto').fill('Aluna Auditoria Editada');
    await Promise.all([
      alunoC.waitForURL(url => url.pathname === `/solicitacoes/${ids.r1}/participantes`),
      alunoC.getByRole('button', { name: /Salvar/ }).click(),
    ]);
    corpo = await alunoC.locator('body').innerText();
    check(corpo.includes('Aluna Auditoria Editada') && !corpo.includes('Aluno Auditoria 01'), 'edição de participante');
    alunoC.once('dialog', d => d.accept());
    await Promise.all([
      alunoC.waitForURL(url => url.pathname === `/solicitacoes/${ids.r1}/participantes`),
      alunoC.getByRole('button', { name: 'Remover Responsável Auditoria Exemplo' }).click(),
    ]);
    corpo = await alunoC.locator('body').innerText();
    check(!corpo.includes('Responsável Auditoria Exemplo') && corpo.includes('10\nTotal'), 'remoção de participante');
    await alunoD.goto(`${BASE}/inicio`);
    const intrusao = await postComCsrf(alunoD, `/solicitacoes/${ids.r1}/participantes`, { nomeCompleto: 'Intruso', rmOuRg: 'RM-X', categoria: 'ALUNO' });
    check(intrusao.status === 404, 'aluno não altera participantes de outra pessoa', `HTTP ${intrusao.status}`);

    // ---------------------------------------------------- formulário oficial
    await alunoC.goto(`${BASE}/solicitacoes/${ids.r1}/documentos`);
    corpo = await alunoC.locator('body').innerText();
    check(corpo.includes('Imprima, assine e envie') && corpo.includes('não ficam gravados'), 'tela explica como gerar, assinar e enviar');
    const cpf = '123.456.789-09';
    let pdf = await gerarFormulario(alunoC, ids.r1, { cpf, curso: 'Técnico em Informática para Internet' });
    const bytes = Buffer.from(pdf.base64, 'base64');
    fs.writeFileSync(path.join(EVIDENCE, 'final-formulario-11.pdf'), bytes);
    check(pdf.status === 200 && bytes.subarray(0, 5).toString() === '%PDF-', 'formulário oficial gerado em PDF');
    check((pdf.cabecalhos['content-type'] || '').startsWith('application/pdf'), 'cabeçalho Content-Type do PDF', pdf.cabecalhos['content-type']);
    check(/attachment; filename="formulario-reserva-quadra-solicitacao-\d+\.pdf"/.test(pdf.cabecalhos['content-disposition'] || ''), 'cabeçalho Content-Disposition para download', pdf.cabecalhos['content-disposition']);
    check((pdf.cabecalhos['cache-control'] || '').includes('no-store'), 'PDF não fica em cache', pdf.cabecalhos['cache-control']);
    check(pdf.cabecalhos['x-content-type-options'] === 'nosniff', 'cabeçalho nosniff');
    check(!pdf.url.includes('123') && !pdf.url.includes('cpf'), 'CPF não vai na URL', pdf.url);
    const semCsrf = await gerarFormulario(alunoC, ids.r1, { cpf, curso: 'Informática' }, { comCsrf: false });
    check(semCsrf.status === 403, 'geração do PDF exige token CSRF', `HTTP ${semCsrf.status}`);
    await alunoD.goto(`${BASE}/inicio`);
    const alheio = await gerarFormulario(alunoD, ids.r1, { cpf, curso: 'Informática' });
    check(alheio.status === 404, 'aluno não gera o formulário de outra pessoa', `HTTP ${alheio.status}`);
    const cursoLongo = await gerarFormulario(alunoC, ids.r1, { cpf, curso: 'X'.repeat(81) });
    check(cursoLongo.url.endsWith(`/solicitacoes/${ids.r1}/documentos`) && Buffer.from(cursoLongo.base64, 'base64').toString().includes('80 caracteres'), 'curso acima de 80 caracteres é recusado com mensagem');
    const semCpf = await gerarFormulario(alunoC, ids.r1, { cpf: '', curso: 'Informática' });
    check(Buffer.from(semCpf.base64, 'base64').toString().includes('CPF'), 'CPF em branco é recusado com mensagem');

    // Mais de 30 participantes e nome longo: o PDF ganha páginas extras.
    ids.grande = await criarSolicitacao(alunoD, dataGrande, '10:00', 'Torneio fictício com muitos participantes');
    await incluirParticipantes(alunoD, ids.grande, 30, 'Participante Grande');
    await alunoD.goto(`${BASE}/solicitacoes/${ids.grande}/participantes/novo`);
    const nomeLongo = 'Maria Eduarda Albuquerque de Vasconcelos Cavalcanti Figueiredo Nascimento Exemplo Auditoria';
    await postComCsrf(alunoD, `/solicitacoes/${ids.grande}/participantes`, { nomeCompleto: nomeLongo, rmOuRg: 'RM-LONGO-031', categoria: 'ALUNO' });
    await alunoD.goto(`${BASE}/solicitacoes/${ids.grande}/documentos`);
    pdf = await gerarFormulario(alunoD, ids.grande, { cpf: '529.982.247-25', curso: 'Técnico em Administração' });
    fs.writeFileSync(path.join(EVIDENCE, 'final-formulario-31.pdf'), Buffer.from(pdf.base64, 'base64'));
    check(pdf.status === 200, 'formulário com 31 participantes gerado', `HTTP ${pdf.status}`);

    // ------------------------------------------- fluxo até a aprovação de R1
    const doc1 = await enviarPdf(alunoC, ids.r1, 'termo-auditoria.pdf', bytes);
    await validarEContribuir(secretaria, doc1, ids.r1);
    await admin.goto(`${BASE}/admin/solicitacoes/${ids.r1}`);
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/solicitacoes'),
      admin.getByRole('button', { name: 'Aprovar e autorizar reserva' }).click(),
    ]);
    exigir((await admin.locator('body').innerText()).includes('Solicitação aprovada e reserva autorizada.'), 'aprovação de R1');
    await alunoC.goto(`${BASE}/solicitacoes/${ids.r1}`);
    corpo = await alunoC.locator('body').innerText();
    check(!corpo.includes('Solicitar outro horário'), 'reserva aprovada não oferece remarcação');
    const origemAprovada = await alunoC.goto(`${BASE}/solicitacoes/nova?origem=${ids.r1}`);
    check(new URL(alunoC.url()).pathname === `/solicitacoes/${ids.r1}` && (await alunoC.locator('body').innerText()).includes('Somente reservas canceladas'), 'remarcação de reserva aprovada é recusada', `HTTP ${origemAprovada.status()}`);
    await alunoC.goto(`${BASE}/solicitacoes/${ids.r1}/documentos`);
    const aposAprovacao = await gerarFormulario(alunoC, ids.r1, { cpf, curso: 'Informática' });
    check(aposAprovacao.url.endsWith('/documentos') && aposAprovacao.status === 200 && !Buffer.from(aposAprovacao.base64, 'base64').subarray(0, 5).toString().startsWith('%PDF'), 'formulário não é gerado depois da aprovação');

    // Segurança: vê a aprovada, sem documentos nem dados financeiros.
    await seguranca.goto(`${BASE}/seguranca/reservas/${ids.r1}`);
    corpo = await seguranca.locator('body').innerText();
    check(corpo.includes('Aluna Auditoria Editada') && !corpo.includes(cpf) && !corpo.includes('R$') && !corpo.includes('@e2e.invalid'), 'segurança vê participantes sem CPF, valores ou e-mails');
    let r = await seguranca.goto(`${BASE}/admin/solicitacoes/${ids.r1}/documentos/${doc1}/baixar`);
    check(r.status() === 403, 'segurança não baixa o PDF pela rota administrativa', `HTTP ${r.status()}`);
    r = await seguranca.goto(`${BASE}/solicitacoes/${ids.r1}/documentos/${doc1}/baixar`);
    check(r.status() === 403, 'segurança não baixa o PDF pela rota do aluno', `HTTP ${r.status()}`);

    // ------------------------------------------------ cancelamento e remarcação
    await admin.goto(`${BASE}/admin/solicitacoes/${ids.r1}`);
    corpo = await admin.locator('body').innerText();
    check(/decisão registrada/i.test(corpo) && /responsável/i.test(corpo), 'detalhe administrativo mostra quem aprovou');
    await admin.locator('#motivoCancelamento').fill('Manutenção fictícia do piso (auditoria).');
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/solicitacoes'),
      admin.getByRole('button', { name: 'Cancelar autorização' }).click(),
    ]);
    r = await seguranca.goto(`${BASE}/seguranca/reservas/${ids.r1}`);
    check(r.status() === 404, 'reserva cancelada não abre para a segurança', `HTTP ${r.status()}`);

    r = await alunoD.goto(`${BASE}/solicitacoes/nova?origem=${ids.r1}`);
    check(r.status() === 404, 'aluno não abre a remarcação da reserva de outra pessoa', `HTTP ${r.status()}`);
    await alunoD.goto(`${BASE}/solicitacoes/nova`);
    const remarcacaoAlheia = await postComCsrf(alunoD, '/solicitacoes', { origemId: String(ids.r1), data: dataRemarcacao, horarioInicio: '10:00', finalidade: 'Intrusão' });
    check(remarcacaoAlheia.status === 404, 'aluno não remarca a reserva de outra pessoa', `HTTP ${remarcacaoAlheia.status}`);

    await alunoC.goto(`${BASE}/solicitacoes/nova?origem=${ids.r1}`);
    const hojeRemarcacao = await postComCsrf(alunoC, '/solicitacoes', { origemId: String(ids.r1), data: hoje, horarioInicio: '09:00', finalidade: 'Hoje' });
    check(texto(hojeRemarcacao.text).includes('pelo menos um dia de antecedência'), 'remarcação respeita a antecedência mínima');
    const foraGrade = await postComCsrf(alunoC, '/solicitacoes', { origemId: String(ids.r1), data: dataRemarcacao, horarioInicio: '15:00', finalidade: 'Fora da grade' });
    check(/não está disponível|horário semanal ativo/.test(texto(foraGrade.text)), 'remarcação respeita a grade atual');

    await alunoC.goto(`${BASE}/solicitacoes/nova?origem=${ids.r1}&data=${dataRemarcacao}`);
    await semRolagemHorizontal(alunoC, 'remarcacao', `${BASE}/solicitacoes/nova?origem=${ids.r1}&data=${dataRemarcacao}`);
    await selecionarHorario(alunoC, '10:00');
    await alunoC.locator('input[name="copiarParticipantes"][type="checkbox"]').uncheck();
    await Promise.all([
      alunoC.waitForURL(url => /^\/solicitacoes\/\d+$/.test(url.pathname)),
      alunoC.getByRole('button', { name: 'Registrar solicitação' }).click(),
    ]);
    ids.r3 = Number(alunoC.url().match(/\/solicitacoes\/(\d+)$/)[1]);
    corpo = await alunoC.locator('body').innerText();
    check(corpo.includes('Esta solicitação remarca a reserva de') && corpo.includes('Pendente de documentação'), 'remarcação nasce pendente e vinculada', `#${ids.r3}`);
    await alunoC.goto(`${BASE}/solicitacoes/${ids.r3}/participantes`);
    check((await alunoC.locator('body').innerText()).includes('0\nTotal'), 'sem a opção marcada, nenhum participante é copiado');
    await alunoC.goto(`${BASE}/solicitacoes/${ids.r3}/documentos`);
    check((await alunoC.locator('body').innerText()).includes('Nenhum PDF enviado'), 'remarcação não reaproveita o PDF');
    await admin.goto(`${BASE}/admin/solicitacoes/${ids.r1}`);
    corpo = await admin.locator('body').innerText();
    check(corpo.includes('10 aluno(s)') && corpo.includes('Cancelamento institucional registrado'), 'reserva cancelada mantém participantes e registro');
    await admin.goto(`${BASE}/admin/solicitacoes/${ids.r3}`);
    corpo = await admin.locator('body').innerText();
    check(corpo.includes('Nenhum') || corpo.includes('ainda não'), 'remarcação não reaproveita contribuição', corpo.match(/Contribuição[^\n]*\n[^\n]*/)?.[0]);

    const segunda = await alunoC.goto(`${BASE}/solicitacoes/nova?origem=${ids.r1}`);
    check(new URL(alunoC.url()).pathname === `/solicitacoes/${ids.r1}` && (await alunoC.locator('body').innerText()).includes('já possui uma nova solicitação'), 'segunda remarcação pela tela é recusada', `HTTP ${segunda.status()}`);
    await alunoC.goto(`${BASE}/solicitacoes/nova`);
    const segundaPost = await postComCsrf(alunoC, '/solicitacoes', { origemId: String(ids.r1), data: dataRemarcacao, horarioInicio: '11:00', finalidade: 'Segunda tentativa' });
    check(texto(segundaPost.text).includes('já possui uma nova solicitação'), 'segunda remarcação por envio direto é recusada');

    await alunoC.goto(`${BASE}/solicitacoes`);
    corpo = await alunoC.locator('body').innerText();
    check(!/abaixo/i.test(corpo.split('Minhas solicitações').pop()), 'lista de solicitações não manda o aluno olhar "abaixo"');
    await alunoC.goto(`${BASE}/inicio`);
    // a solicitação mais recente é a remarcação; o texto de cancelada aparece na lista.

    // --------------------------------------------- rejeição e histórico admin
    ids.r5 = await criarSolicitacao(alunoD, dataRejeicao, '11:00', 'Atividade fictícia que será rejeitada');
    await incluirParticipantes(alunoD, ids.r5, 10, 'Aluno Rejeição');
    const doc5 = await enviarPdf(alunoD, ids.r5, 'termo-rejeicao.pdf', pdfFicticio);
    await validarEContribuir(secretaria, doc5, ids.r5);
    await admin.goto(`${BASE}/admin/solicitacoes/${ids.r5}`);
    await admin.locator('#motivo').fill('Motivo fictício de rejeição da auditoria.');
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/solicitacoes'),
      admin.getByRole('button', { name: 'Rejeitar solicitação' }).click(),
    ]);
    await admin.goto(`${BASE}/admin/solicitacoes/${ids.r5}`);
    corpo = await admin.locator('body').innerText();
    check(corpo.includes('Motivo fictício de rejeição da auditoria.'), 'detalhe administrativo mostra o motivo da rejeição');
    await admin.goto(`${BASE}/admin/solicitacoes`);
    corpo = await admin.locator('body').innerText();
    const linksDecididas = await admin.locator('a[href^="/admin/solicitacoes/"]').evaluateAll(as => as.map(a => a.getAttribute('href')));
    check(linksDecididas.includes(`/admin/solicitacoes/${ids.r5}`) && linksDecididas.includes(`/admin/solicitacoes/${ids.r1}`), 'administração encontra rejeitadas e canceladas', JSON.stringify(linksDecididas.slice(0, 8)));
    await semRolagemHorizontal(admin, 'admin-solicitacoes', `${BASE}/admin/solicitacoes`);

    await admin.goto(`${BASE}/admin`);
    const indicadores = await admin.locator('.indicador').evaluateAll(els => els.map(el => el.innerText.replace(/\s+/g, ' ').trim()));
    const numero = rotulo => Number((indicadores.find(t => t.includes(rotulo)) || '').match(/\d+/)?.[0] ?? NaN);
    const total = numero('Total');
    const soma = ['Aguardando documentação', 'Aguardando análise', 'Aprovadas', 'Rejeitadas', 'Canceladas']
      .map(numero).filter(n => !Number.isNaN(n)).reduce((a, b) => a + b, 0);
    check(total === soma, 'painel: o total é a soma das situações', JSON.stringify(indicadores));

    // ------------------------------------------- mensagens de entrada inválida
    await admin.goto(`${BASE}/admin/bloqueios/novo`);
    const dataInvalida = await postComCsrf(admin, '/admin/bloqueios', { data: '31/31/2026', horarioInicio: '8h', horarioFim: '09:00', motivo: 'Teste' });
    const dataInvalidaTexto = texto(dataInvalida.text);
    check(!/Failed to convert|java\.|NumberFormat|DateTimeParse/.test(dataInvalidaTexto) && /data válida|horário válido/i.test(dataInvalidaTexto), 'data ou horário inválido mostra mensagem compreensível', dataInvalidaTexto.match(/(Failed[^.]*\.|Informe[^.]*\.)/)?.[0]);
    await alunoD.goto(`${BASE}/solicitacoes/nova`);
    const horaInvalida = await postComCsrf(alunoD, '/solicitacoes', { data: dataGrande, horarioInicio: 'nove', finalidade: 'Teste' });
    const horaInvalidaTexto = texto(horaInvalida.text);
    check(!/Failed to convert|java\./.test(horaInvalidaTexto), 'horário adulterado na solicitação não mostra erro técnico', horaInvalidaTexto.match(/Failed[^.]*\./)?.[0]);

    // --------------------------------------- usuários: edição e redefinição
    await admin.goto(`${BASE}/admin/usuarios?busca=${encodeURIComponent(contas.alunoD)}`);
    await admin.getByRole('link', { name: /Editar/ }).first().click();
    await admin.locator('#nome').fill('Davi Auditoria Editado');
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/usuarios'),
      admin.getByRole('button', { name: /Salvar/ }).click(),
    ]);
    check((await admin.locator('body').innerText()).includes('Davi Auditoria Editado'), 'edição de usuário');
    await admin.goto(`${BASE}/admin/usuarios?busca=${encodeURIComponent(contas.alunoD)}`);
    await admin.getByRole('link', { name: /Redefinir senha/ }).first().click();
    const novaSenha = `${SENHA}N`;
    await admin.locator('#novaSenha').fill(novaSenha);
    await admin.locator('#confirmacaoSenha').fill(novaSenha);
    await Promise.all([
      admin.waitForURL(url => url.pathname === '/admin/usuarios'),
      admin.getByRole('button', { name: /Redefinir|Salvar/ }).click(),
    ]);
    await alunoD.goto(`${BASE}/inicio`);
    check(new URL(alunoD.url()).pathname === '/login', 'redefinição de senha encerra a sessão anterior');
    const alunoDNova = await (await browser.newContext()).newPage();
    await alunoDNova.goto(`${BASE}/login`);
    await alunoDNova.getByLabel(/e-?mail/i).fill(contas.alunoD);
    await alunoDNova.getByLabel(/senha/i).fill(novaSenha);
    await Promise.all([alunoDNova.waitForLoadState('networkidle'), alunoDNova.getByRole('button', { name: 'Entrar' }).click()]);
    check(new URL(alunoDNova.url()).pathname === '/inicio', 'login com a senha redefinida');

    // ----------------------------------------------------- celular: telas novas
    await semRolagemHorizontal(alunoC, 'documentos', `${BASE}/solicitacoes/${ids.r3}/documentos`);
    await semRolagemHorizontal(alunoC, 'detalhe-cancelada', `${BASE}/solicitacoes/${ids.r1}`);
    await semRolagemHorizontal(admin, 'admin-detalhe-cancelada', `${BASE}/admin/solicitacoes/${ids.r1}`);
    await semRolagemHorizontal(admin, 'admin-painel', `${BASE}/admin`);

    fs.writeFileSync(path.join(EVIDENCE, 'resultados-finalizacao.json'), JSON.stringify({ sha: configuracao.targetSha, ids, results }, null, 2));
  } catch (error) {
    results.push({ status: 'FAIL', name: 'execução interrompida', detail: String(error.stack).split('\n').slice(0, 3).join(' ') });
    process.stderr.write(`${error.stack}\n`);
    fs.writeFileSync(path.join(EVIDENCE, 'resultados-finalizacao.json'), JSON.stringify({ sha: configuracao.targetSha, ids, error: error.stack, results }, null, 2));
  } finally {
    await browser.close();
  }
  const falhas = results.filter(x => x.status === 'FAIL');
  process.stdout.write(`SUMMARY FINALIZACAO | ${results.length - falhas.length} aprovadas | ${falhas.length} reprovadas\n`);
  process.exitCode = falhas.length ? 1 : 0;
})();
