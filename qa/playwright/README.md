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

## Isolamento obrigatório antes de executar a jornada

**Esta automação cria usuários e modifica a grade, documentos, contribuições e reservas.**
Ela não deve ser executada no `courttime_demo`, nem em banco persistente.

1. Crie um banco PostgreSQL **novo, vazio e descartável** chamado `courttime_e2e`.
2. Execute uma **instância separada** do Spring Boot, configurada com
   `COURTTIME_DB_URL=jdbc:postgresql://localhost:5432/courttime_e2e` e
   `SERVER_PORT=18081`. Use credenciais sintéticas de bootstrap neste banco.
3. Verifique pessoalmente, antes de cada execução, que **a aplicação em
   `127.0.0.1:18081` está realmente conectada a `courttime_e2e`** e não ao
   banco de desenvolvimento. A validação do script não consegue provar isso.
4. Habilite o aceite explícito somente para a sessão de teste:

```bash
export COURTTIME_E2E_ACK_ISOLATED_DB=YES
export COURTTIME_E2E_DATABASE_NAME=courttime_e2e
export COURTTIME_E2E_BASE_URL=http://127.0.0.1:18081
```

Sem essas variáveis o script **interrompe antes de iniciar o navegador**.
URLs remotas, HTTPS ou portas diferentes de 18081 são recusadas. A proteção é
preventiva; não dispensa conferir manualmente o banco da instância do servidor.

A suíte de testes da própria proteção pode ser executada sem PostgreSQL nem
Chromium: `npm run test:guard` (usa apenas o Node.js instalado).

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
