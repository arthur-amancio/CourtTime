// Capturas reais da versao final do CourtTime, com as regras da ETEC (R01 a R09).
//
// Percorre UMA reserva do inicio ao fim, com contas e dados de demonstracao,
// navegador em pt-BR e fuso de Sao Paulo. Nada aqui altera o codigo da
// aplicacao: so usa a interface, como uma pessoa faria.
//
// Roda somente contra o banco descartavel courttime_e2e do GitHub Actions.
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const { selecionarHorario } = require('./suporte/sessoes.cjs');

// ---------- protecao: nunca rodar fora do banco descartavel ----------
const BASE = process.env.COURTTIME_E2E_BASE_URL;
if (process.env.COURTTIME_E2E_ACK_ISOLATED_DB !== 'YES'
    || process.env.COURTTIME_E2E_DATABASE_NAME !== 'courttime_e2e'
    || !/^http:\/\/(127\.0\.0\.1|localhost):18081$/.test(BASE || '')) {
  throw new Error('Bloqueado: estas capturas so rodam no banco descartavel courttime_e2e do CI.');
}
const SENHA = process.env.COURTTIME_E2E_PASSWORD;
if (!SENHA) throw new Error('Defina COURTTIME_E2E_PASSWORD no ambiente da execucao.');
const SAIDA = process.env.COURTTIME_APRESENTACAO_DIR;
fs.mkdirSync(SAIDA, { recursive: true });

const CONTAS = {
  admin: process.env.COURTTIME_E2E_ADMIN_EMAIL,
  aluna: 'ana.beatriz@exemplo.invalid',
  aluno: 'bruno.henrique@exemplo.invalid',
  secretaria: 'secretaria@exemplo.invalid',
  seguranca: 'portaria@exemplo.invalid',
};

const PARTICIPANTES_ANA = [
  'Ana Beatriz Exemplo', 'Bruno Costa Exemplo', 'Camila Rocha Exemplo', 'Diego Martins Exemplo',
  'Eduarda Lima Exemplo', 'Felipe Souza Exemplo', 'Gabriela Alves Exemplo', 'Henrique Dias Exemplo',
  'Isabela Nunes Exemplo', 'João Pedro Exemplo',
];
const PARTICIPANTES_BRUNO = [
  'Bruno Henrique Exemplo', 'Carlos Eduardo Exemplo', 'Daniela Freitas Exemplo', 'Elias Moreira Exemplo',
  'Fernanda Castro Exemplo', 'Gustavo Pires Exemplo', 'Heloísa Ramos Exemplo', 'Igor Teixeira Exemplo',
  'Júlia Campos Exemplo', 'Kauã Ribeiro Exemplo',
];

// ---------- datas: proximas segunda e quarta no fuso de Sao Paulo ----------
function hojeEmSaoPaulo() {
  const partes = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date());
  return new Date(`${partes}T12:00:00Z`);
}
function proxima(diaDaSemana, depoisDe) {
  const d = new Date(depoisDe);
  do { d.setUTCDate(d.getUTCDate() + 1); } while (d.getUTCDay() !== diaDaSemana);
  return d;
}
const iso = d => d.toISOString().slice(0, 10);
const hoje = hojeEmSaoPaulo();
const DATA_RESERVA = proxima(1, new Date(hoje.getTime() + 2 * 86400000)); // segunda
const DATA_BLOQUEIO = proxima(2, DATA_RESERVA); // terca seguinte
const DATA_REMARCACAO = proxima(3, DATA_RESERVA); // quarta seguinte

// ---------- utilitarios ----------
const registro = [];
let sequencia = 0;
async function capturar(page, nome, legenda, { inteira = true } = {}) {
  await page.evaluate(() => {
    if (document.activeElement && document.activeElement !== document.body) document.activeElement.blur();
    window.scrollTo(0, 0);
  });
  await page.waitForTimeout(250);
  sequencia += 1;
  const arquivo = `${String(sequencia).padStart(2, '0')}-${nome}.png`;
  await page.screenshot({ path: path.join(SAIDA, arquivo), fullPage: inteira });
  const v = page.viewportSize();
  registro.push({ arquivo, legenda, url: new URL(page.url()).pathname + new URL(page.url()).search, largura: v.width });
  process.stdout.write(`captura ${arquivo}\n`);
}
async function texto(page) { return page.locator('body').innerText(); }
function exigir(condicao, descricao) {
  if (!condicao) throw new Error(`Falha no roteiro: ${descricao}`);
}
async function clicarEEsperar(page, nomeDoBotao, caminho) {
  await Promise.all([
    page.waitForURL(url => (caminho instanceof RegExp ? caminho.test(url.pathname) : url.pathname === caminho)),
    page.getByRole('button', { name: nomeDoBotao }).click(),
  ]);
}

async function novoContexto(browser, largura = 1440, altura = 900) {
  const context = await browser.newContext({
    viewport: { width: largura, height: altura },
    deviceScaleFactor: 2,
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    acceptDownloads: true,
  });
  return { context, page: await context.newPage() };
}
async function entrar(page, email) {
  await page.goto(`${BASE}/login`);
  await page.locator('#email').fill(email);
  await page.locator('#senha').fill(SENHA);
  await clicarEEsperar(page, 'Entrar', '/inicio');
}

(async () => {
  // Chromium completo (não o headless shell) respeita o idioma pt-BR no
  // campo de data, exibindo dd/mm/aaaa como no navegador do aluno.
  const browser = await chromium.launch({ channel: 'chromium', args: ['--lang=pt-BR', '--accept-lang=pt-BR'] });
  try {
    // ================= PUBLICO =================
    const visitante = await novoContexto(browser);
    await visitante.page.goto(`${BASE}/`);
    await capturar(visitante.page, 'publico-pagina-inicial', 'Página pública');
    await visitante.page.goto(`${BASE}/login`);
    await capturar(visitante.page, 'publico-login', 'Acesso ao sistema', { inteira: false });

    // ================= PREPARO PELA ADMINISTRACAO =================
    const admin = await novoContexto(browser);
    await entrar(admin.page, CONTAS.admin);
    const pessoas = [
      ['Ana Beatriz Exemplo', CONTAS.aluna, 'ALUNO'],
      ['Bruno Henrique Exemplo', CONTAS.aluno, 'ALUNO'],
      ['Secretaria Acadêmica (demonstração)', CONTAS.secretaria, 'SECRETARIA'],
      ['Portaria (demonstração)', CONTAS.seguranca, 'SEGURANCA'],
    ];
    for (const [nome, email, perfil] of pessoas) {
      await admin.page.goto(`${BASE}/admin/usuarios/novo`);
      await admin.page.locator('#nome').fill(nome);
      await admin.page.locator('#email').fill(email);
      await admin.page.locator('#perfil').selectOption(perfil);
      await admin.page.locator('#senha').fill(SENHA);
      await admin.page.locator('#confirmacaoSenha').fill(SENHA);
      if (perfil === 'SEGURANCA') await capturar(admin.page, 'admin-novo-usuario', 'Cadastro de conta pela administração');
      await clicarEEsperar(admin.page, 'Cadastrar usuário', '/admin/usuarios');
    }
    for (const dia of ['SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA']) {
      await admin.page.goto(`${BASE}/admin/horarios/novo`);
      await admin.page.locator('#diaSemana').selectOption(dia);
      await admin.page.locator('#horarioInicio').fill('08:00');
      await admin.page.locator('#horarioFim').fill('12:00');
      await clicarEEsperar(admin.page, 'Cadastrar horário', '/admin/horarios');
    }
    await admin.page.goto(`${BASE}/admin/bloqueios/novo`);
    await admin.page.locator('#data').fill(iso(DATA_BLOQUEIO));
    await admin.page.locator('#horarioInicio').fill('08:00');
    await admin.page.locator('#horarioFim').fill('10:00');
    await admin.page.locator('#motivo').fill('Evento da escola na quadra (exemplo)');
    await clicarEEsperar(admin.page, 'Cadastrar bloqueio', '/admin/bloqueios');

    // ================= ALUNA: PEDIDO =================
    const ana = await novoContexto(browser);
    await entrar(ana.page, CONTAS.aluna);
    await capturar(ana.page, 'aluna-inicio', 'Início da aluna, antes do primeiro pedido');
    await ana.page.goto(`${BASE}/quadra?data=${iso(DATA_BLOQUEIO)}`);
    await capturar(ana.page, 'aluna-consulta-com-bloqueio', 'Consulta de horários com um bloqueio');
    await ana.page.goto(`${BASE}/solicitacoes/nova?data=${iso(DATA_RESERVA)}`);
    await selecionarHorario(ana.page, '09:00');
    await ana.page.locator('#finalidade').fill('Treino de vôlei da turma do 3º INFONET (exemplo)');
    await capturar(ana.page, 'aluna-nova-solicitacao-preenchida', 'Nova solicitação preenchida');
    await clicarEEsperar(ana.page, 'Registrar solicitação', /^\/solicitacoes\/\d+$/);
    const reservaId = Number(ana.page.url().match(/(\d+)$/)[1]);
    exigir((await texto(ana.page)).includes('Solicitação registrada.'), 'solicitação registrada');
    await capturar(ana.page, 'aluna-solicitacao-registrada', 'Solicitação registrada');

    // participantes: 10 alunos e 1 "Outro"
    const adicionar = async (page, id, nome, rm, categoria, capturarForm = false) => {
      await page.goto(`${BASE}/solicitacoes/${id}/participantes/novo`);
      await page.locator('#nomeCompleto').fill(nome);
      await page.locator('#rmOuRg').fill(rm);
      await page.locator('#categoria').selectOption(categoria);
      if (categoria === 'OUTRO') await page.locator('#vinculo').selectOption('PAI_OU_MAE');
      if (capturarForm) await capturar(page, 'aluna-participante-formulario', 'Cadastro de participante');
      await clicarEEsperar(page, 'Salvar participante', `/solicitacoes/${id}/participantes`);
    };
    for (let i = 0; i < PARTICIPANTES_ANA.length; i += 1) {
      await adicionar(ana.page, reservaId, PARTICIPANTES_ANA[i], `RM 24${String(i + 1).padStart(3, '0')}`, 'ALUNO', i === 0);
    }
    await ana.page.goto(`${BASE}/solicitacoes/${reservaId}/participantes/novo`);
    await ana.page.locator('#nomeCompleto').fill('Marcos Responsável Exemplo');
    await ana.page.locator('#rmOuRg').fill('RG 00.000.000-0');
    await ana.page.locator('#categoria').selectOption('OUTRO');
    await ana.page.locator('#vinculo').selectOption('PAI_OU_MAE');
    await capturar(ana.page, 'aluna-participante-outro-vinculo', 'Participante da categoria Outro com vínculo (R09)');
    await clicarEEsperar(ana.page, 'Salvar participante', `/solicitacoes/${reservaId}/participantes`);
    exigir((await texto(ana.page)).includes('(Pai ou mãe de aluno)'), 'vínculo na lista');
    exigir((await texto(ana.page)).includes('Mínimo de alunos atendido'), 'mínimo de alunos');
    await capturar(ana.page, 'aluna-participantes', 'Lista de participantes, com o vínculo do Outro');

    // formulario oficial preenchido
    await ana.page.goto(`${BASE}/solicitacoes/${reservaId}/documentos`);
    await ana.page.locator('#cpf').fill('123.456.789-09');
    await ana.page.locator('#curso').fill('Técnico em Informática para Internet');
    await capturar(ana.page, 'aluna-documentos-formulario-oficial', 'Geração do formulário oficial');
    const [download] = await Promise.all([
      ana.page.waitForEvent('download'),
      ana.page.getByRole('button', { name: 'Gerar formulário preenchido' }).click(),
    ]);
    const formularioGerado = path.join(SAIDA, 'formulario-oficial-gerado.pdf');
    await download.saveAs(formularioGerado);
    const formularioBytes = fs.readFileSync(formularioGerado);
    exigir(formularioBytes.subarray(0, 5).toString() === '%PDF-', 'formulário oficial é PDF');

    // envio: na demonstracao, o proprio formulario gerado, sem assinatura real
    const enviar = async (page, id, nomeArquivo, capturarAntes) => {
      await page.goto(`${BASE}/solicitacoes/${id}/documentos`);
      await page.locator('#arquivo').setInputFiles({ name: nomeArquivo, mimeType: 'application/pdf', buffer: formularioBytes });
      if (capturarAntes) await capturar(page, capturarAntes, 'Envio do PDF, com a data-limite do dia anterior (R01)');
      await Promise.all([
        page.waitForLoadState('networkidle'),
        page.getByRole('button', { name: 'Enviar nova versão' }).click(),
      ]);
      exigir((await texto(page)).includes('PDF enviado como nova versão'), `envio ${nomeArquivo}`);
    };
    await enviar(ana.page, reservaId, 'termo-reserva-exemplo-v1.pdf', 'aluna-envio-pdf');
    await capturar(ana.page, 'aluna-documento-aguardando', 'Documento aguardando conferência');
    const docV1 = Number((await ana.page.getByRole('link', { name: 'Baixar original' }).first().getAttribute('href')).match(/documentos\/(\d+)\/baixar/)[1]);

    // ================= SECRETARIA: DEVOLUCAO =================
    const secretaria = await novoContexto(browser);
    await entrar(secretaria.page, CONTAS.secretaria);
    await secretaria.page.goto(`${BASE}/secretaria/documentos`);
    await capturar(secretaria.page, 'secretaria-fila-conferencia', 'Fila de conferência');
    await secretaria.page.goto(`${BASE}/secretaria/documentos/${docV1}`);
    await secretaria.page.locator('#justificativa').fill('Falta a assinatura do solicitante na página 1 do termo (exemplo).');
    await capturar(secretaria.page, 'secretaria-devolucao-justificada', 'Devolução com justificativa');
    await clicarEEsperar(secretaria.page, 'Devolver para correção', '/secretaria/documentos');
    exigir((await texto(secretaria.page)).includes('Documento devolvido para correção.'), 'devolução');
    await capturar(secretaria.page, 'secretaria-devolucao-registrada', 'Devolução registrada');

    await ana.page.goto(`${BASE}/solicitacoes/${reservaId}`);
    await capturar(ana.page, 'aluna-ve-devolucao', 'A aluna vê a devolução');
    await enviar(ana.page, reservaId, 'termo-reserva-exemplo-v2.pdf', null);
    await capturar(ana.page, 'aluna-historico-versoes', 'Histórico de versões');
    const docV2 = Number((await ana.page.getByRole('link', { name: 'Baixar original' }).first().getAttribute('href')).match(/documentos\/(\d+)\/baixar/)[1]);

    // ================= SECRETARIA: VALIDACAO E CONTRIBUICAO =================
    await secretaria.page.goto(`${BASE}/secretaria/documentos/${docV2}`);
    exigir((await texto(secretaria.page)).includes('validar.iti.gov.br'), 'orientação do ITI');
    await secretaria.page.getByLabel('Conferi a assinatura gov.br no verificador oficial do ITI').check();
    await capturar(secretaria.page, 'secretaria-conferir-govbr', 'Conferência da assinatura gov.br (R07)');
    await clicarEEsperar(secretaria.page, 'Validar documento', '/secretaria/documentos');
    exigir((await texto(secretaria.page)).includes('Documento conferido e validado.'), 'validação');
    await capturar(secretaria.page, 'secretaria-documento-validado', 'Documento validado');

    const registrarContribuicao = async (id, capturarAntes, legenda = 'Contribuição calculada em R$ 2,00 por participante (R03)') => {
      await secretaria.page.goto(`${BASE}/secretaria/contribuicoes`);
      const bloco = secretaria.page.getByRole('heading', { name: `Solicitação #${id}` }).locator('..');
      await bloco.getByLabel('Observações').fill('Recebido em dinheiro na secretaria (exemplo).');
      if (capturarAntes) await capturar(secretaria.page, capturarAntes, legenda);
      await Promise.all([
        secretaria.page.waitForURL(url => url.pathname === `/secretaria/contribuicoes/solicitacoes/${id}`),
        bloco.getByRole('button', { name: /Registrar recebimento|Aplicar crédito/ }).click(),
      ]);
      exigir((await texto(secretaria.page)).includes('Contribuição presencial registrada.'), 'contribuição');
    };
    await registrarContribuicao(reservaId, 'secretaria-contribuicao-calculada');
    await capturar(secretaria.page, 'secretaria-contribuicao-registrada', 'Contribuição registrada');

    // ================= SEGUNDO PEDIDO (sera rejeitado) =================
    const bruno = await novoContexto(browser);
    await entrar(bruno.page, CONTAS.aluno);
    await bruno.page.goto(`${BASE}/solicitacoes/nova?data=${iso(DATA_RESERVA)}`);
    await selecionarHorario(bruno.page, '10:00');
    await bruno.page.locator('#finalidade').fill('Campeonato interno de futsal (exemplo)');
    await clicarEEsperar(bruno.page, 'Registrar solicitação', /^\/solicitacoes\/\d+$/);
    const reservaBruno = Number(bruno.page.url().match(/(\d+)$/)[1]);
    for (let i = 0; i < PARTICIPANTES_BRUNO.length; i += 1) {
      await adicionar(bruno.page, reservaBruno, PARTICIPANTES_BRUNO[i], `RM 25${String(i + 1).padStart(3, '0')}`, 'ALUNO');
    }
    await enviar(bruno.page, reservaBruno, 'termo-reserva-exemplo.pdf', null);
    const docBruno = Number((await bruno.page.getByRole('link', { name: 'Baixar original' }).first().getAttribute('href')).match(/documentos\/(\d+)\/baixar/)[1]);
    await secretaria.page.goto(`${BASE}/secretaria/documentos/${docBruno}`);
    await secretaria.page.getByLabel('Conferi a assinatura gov.br no verificador oficial do ITI').check();
    await clicarEEsperar(secretaria.page, 'Validar documento', '/secretaria/documentos');
    await registrarContribuicao(reservaBruno, null);

    // ================= ADMINISTRACAO: ANALISE =================
    await admin.page.goto(`${BASE}/admin/solicitacoes`);
    await capturar(admin.page, 'admin-fila-analise', 'Fila de análise');
    await admin.page.goto(`${BASE}/admin/solicitacoes/${reservaId}`);
    await capturar(admin.page, 'admin-analise-detalhe', 'Análise da solicitação');
    await clicarEEsperar(admin.page, 'Aprovar e autorizar reserva', '/admin/solicitacoes');
    exigir((await texto(admin.page)).includes('Solicitação aprovada e reserva autorizada.'), 'aprovação');
    await capturar(admin.page, 'admin-aprovacao-registrada', 'Aprovação registrada');

    await admin.page.goto(`${BASE}/admin/solicitacoes/${reservaBruno}`);
    await admin.page.locator('#motivo').fill('Mesmo dia de outra atividade já autorizada; escolha uma data com a quadra livre (exemplo).');
    await capturar(admin.page, 'admin-rejeicao-com-motivo', 'Rejeição com motivo');
    await clicarEEsperar(admin.page, 'Rejeitar solicitação', '/admin/solicitacoes');
    await bruno.page.goto(`${BASE}/solicitacoes/${reservaBruno}`);
    exigir((await texto(bruno.page)).includes('Mesmo dia de outra atividade'), 'aluno vê motivo da rejeição');
    await capturar(bruno.page, 'aluno-ve-rejeicao', 'O aluno vê o motivo da rejeição');

    await ana.page.goto(`${BASE}/solicitacoes/${reservaId}`);
    await capturar(ana.page, 'aluna-solicitacao-aprovada', 'Solicitação aprovada');

    // ================= PORTARIA =================
    const portaria = await novoContexto(browser);
    await entrar(portaria.page, CONTAS.seguranca);
    await portaria.page.goto(`${BASE}/seguranca/reservas`);
    await capturar(portaria.page, 'portaria-reservas-autorizadas', 'Reservas autorizadas');
    await portaria.page.goto(`${BASE}/seguranca/reservas/${reservaId}`);
    exigir((await texto(portaria.page)).includes('Outro (Pai ou mãe de aluno)'), 'vínculo na portaria');
    await capturar(portaria.page, 'portaria-conferencia', 'Conferência na entrada, com o vínculo do Outro; a presença só é marcada no dia (R08)');

    // ================= CANCELAMENTO INSTITUCIONAL =================
    await admin.page.goto(`${BASE}/admin/solicitacoes/${reservaId}`);
    await admin.page.locator('#motivoCancelamento').fill('Quadra interditada para manutenção do piso (exemplo).');
    exigir((await texto(admin.page)).includes('avise o aluno pelo WhatsApp'), 'lembrete do WhatsApp');
    await capturar(admin.page, 'admin-cancelamento-com-motivo', 'Cancelamento com motivo e lembrete do WhatsApp (R05)');
    await clicarEEsperar(admin.page, 'Cancelar autorização', '/admin/solicitacoes');
    exigir((await texto(admin.page)).includes('Reserva cancelada por motivo institucional.'), 'cancelamento');
    await capturar(admin.page, 'admin-cancelamento-registrado', 'Cancelamento registrado');
    await portaria.page.goto(`${BASE}/seguranca/reservas`);
    await capturar(portaria.page, 'portaria-apos-cancelamento', 'Portaria depois do cancelamento');

    // ================= REMARCACAO =================
    await ana.page.goto(`${BASE}/solicitacoes/${reservaId}`);
    await capturar(ana.page, 'aluna-reserva-cancelada', 'Reserva cancelada');
    await Promise.all([
      ana.page.waitForURL(url => url.pathname === '/solicitacoes/nova'),
      ana.page.getByRole('link', { name: 'Solicitar outro horário' }).click(),
    ]);
    await ana.page.goto(`${BASE}/solicitacoes/nova?origem=${reservaId}&data=${iso(DATA_REMARCACAO)}`);
    await selecionarHorario(ana.page, '09:00');
    await capturar(ana.page, 'aluna-remarcacao-formulario', 'Remarcação');
    await clicarEEsperar(ana.page, 'Registrar solicitação', /^\/solicitacoes\/\d+$/);
    const remarcadaId = Number(ana.page.url().match(/(\d+)$/)[1]);
    exigir((await texto(ana.page)).includes('Esta solicitação remarca a reserva de'), 'remarcação vinculada');
    await capturar(ana.page, 'aluna-remarcacao-registrada', 'Nova solicitação vinculada');
    await ana.page.goto(`${BASE}/solicitacoes/${remarcadaId}/participantes`);
    await capturar(ana.page, 'aluna-remarcacao-participantes', 'Participantes copiados');

    // ================= REMARCACAO: CREDITO (R04) =================
    await enviar(ana.page, remarcadaId, 'termo-remarcacao-exemplo.pdf', null);
    const docRemarcada = Number((await ana.page.getByRole('link', { name: 'Baixar original' }).first().getAttribute('href')).match(/documentos\/(\d+)\/baixar/)[1]);
    await secretaria.page.goto(`${BASE}/secretaria/documentos/${docRemarcada}`);
    await secretaria.page.getByLabel('Conferi a assinatura gov.br no verificador oficial do ITI').check();
    await clicarEEsperar(secretaria.page, 'Validar documento', '/secretaria/documentos');
    await secretaria.page.goto(`${BASE}/secretaria/contribuicoes`);
    exigir((await texto(secretaria.page)).includes('Aplicar crédito'), 'crédito cobre a remarcação');
    await registrarContribuicao(remarcadaId, 'secretaria-credito-remarcacao', 'Remarcação: crédito da reserva cancelada cobre o valor devido (R04)');
    await capturar(secretaria.page, 'secretaria-credito-aplicado', 'Crédito da reserva cancelada aplicado (R04)');
    await ana.page.goto(`${BASE}/solicitacoes`);
    await capturar(ana.page, 'aluna-minhas-solicitacoes', 'Minhas solicitações');

    // ================= ADMINISTRACAO: VISAO GERAL =================
    await admin.page.goto(`${BASE}/admin`);
    await capturar(admin.page, 'admin-painel', 'Painel administrativo');
    await admin.page.goto(`${BASE}/admin/horarios`);
    await capturar(admin.page, 'admin-horarios', 'Horários semanais');
    await admin.page.goto(`${BASE}/admin/bloqueios`);
    await capturar(admin.page, 'admin-bloqueios', 'Bloqueios');
    await admin.page.goto(`${BASE}/admin/usuarios`);
    await capturar(admin.page, 'admin-usuarios', 'Usuários');

    // ================= CELULAR (somente a area visivel) =================
    const celular = await novoContexto(browser, 390, 844);
    await celular.page.goto(`${BASE}/`);
    await capturar(celular.page, 'celular-pagina-inicial', 'Celular: página inicial', { inteira: false });
    await entrar(celular.page, CONTAS.aluna);
    await capturar(celular.page, 'celular-aluna-inicio', 'Celular: início da aluna', { inteira: false });
    const menu = celular.page.getByRole('button', { name: /menu/i });
    if (await menu.count()) {
      await menu.first().click();
      await celular.page.waitForTimeout(300);
      sequencia += 1;
      const arquivo = `${String(sequencia).padStart(2, '0')}-celular-menu-aberto.png`;
      await celular.page.screenshot({ path: path.join(SAIDA, arquivo) });
      registro.push({ arquivo, legenda: 'Celular: menu aberto', url: '/inicio', largura: 390 });
    }
    await celular.page.goto(`${BASE}/solicitacoes/${remarcadaId}`);
    await capturar(celular.page, 'celular-detalhe-remarcacao', 'Celular: detalhe da remarcação', { inteira: false });
    const portariaCelular = await novoContexto(browser, 390, 844);
    await entrar(portariaCelular.page, CONTAS.seguranca);
    await portariaCelular.page.goto(`${BASE}/seguranca/reservas`);
    await capturar(portariaCelular.page, 'celular-portaria', 'Celular: portaria', { inteira: false });

    fs.writeFileSync(path.join(SAIDA, 'capturas.json'), JSON.stringify({
      sha: process.env.COURTTIME_E2E_TARGET_SHA,
      geradoEm: new Date().toISOString(),
      datas: { reserva: iso(DATA_RESERVA), bloqueio: iso(DATA_BLOQUEIO), remarcacao: iso(DATA_REMARCACAO) },
      ids: { reservaId, reservaBruno, remarcadaId },
      capturas: registro,
    }, null, 2));
    process.stdout.write(`RESUMO | ${registro.length} capturas\n`);
  } catch (erro) {
    fs.writeFileSync(path.join(SAIDA, 'erro.txt'), String(erro.stack));
    process.stderr.write(`${erro.stack}\n`);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();
