# QA de navegador do CourtTime

Esta pasta contém dois executáveis Node independentes da suíte Maven:

- `jornada-quatro-perfis.cjs`: cria dados integralmente sintéticos pela própria
  interface e percorre Aluno, Secretaria, Administrador e Segurança. Também
  cobre CSRF, IDOR, upload inválido/excessivo, concorrência de aprovação,
  repetição de operações, cancelamento, sessão revogada e responsividade.
- `auditoria-acessibilidade.cjs`: executa axe-core nas páginas principais para
  os viewports 390, 430, 768 e 1440 px. Deve ser rodada depois da jornada,
  porque reutiliza os usuários sintéticos criados por ela.

Os seletores usam papel, rótulo e texto visível. Classes CSS não fazem parte do
contrato de teste, permitindo que o redesign mude a aparência sem mascarar uma
regressão funcional.

## Pré-requisitos

- aplicação CourtTime em execução com PostgreSQL descartável;
- uma conta administrativa de bootstrap ainda não usada pela automação;
- Node.js 20 ou superior;
- Chromium disponível ou navegador do Playwright instalado.

Instale somente as dependências de QA:

```bash
cd qa/playwright
npm ci
# Apenas quando não houver Chromium do sistema:
npx playwright install chromium
```

## Execução

Defina a senha sintética apenas no ambiente da sessão. Não a grave em arquivo,
comando versionado, log ou captura.

```bash
export COURTTIME_E2E_BASE_URL=http://127.0.0.1:18081
export COURTTIME_E2E_PASSWORD='senha-sintetica-da-execucao'
export COURTTIME_E2E_TARGET_SHA="$(git rev-parse HEAD)"
export COURTTIME_E2E_EVIDENCE_DIR="$PWD/evidencias"
# Opcional quando se usa um Chromium do sistema:
export COURTTIME_E2E_CHROMIUM_PATH=/usr/bin/chromium

npm run test:jornada
npm run test:a11y
```

A aplicação deve ser iniciada com o mesmo email administrativo usado pelo
roteiro (`admin.banca@exemplo.local`) e com a senha fornecida em
`COURTTIME_E2E_PASSWORD`. O banco precisa estar vazio, pois a jornada cria as
demais contas e a grade semanal.

## Evidências

O diretório configurado recebe PNGs reais do navegador e JSONs com SHA,
cenários e resultados. Ele é ignorado pelo Git porque pode conter IDs gerados
na execução. Antes de compartilhar uma captura, confirme que o banco contém
somente pessoas fictícias.

A automação usa PDF sintético gerado pelo próprio Chromium. Ele é identificado
como teste, não reproduz o termo da ETEC e não representa assinatura.
