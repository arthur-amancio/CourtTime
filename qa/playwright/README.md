# QA de navegador do CourtTime

Esta pasta contém automação Playwright contra a aplicação Spring Boot real. Os
seletores priorizam papel, nome, rótulo e contratos HTTP; classes CSS e
coordenadas não fazem parte do contrato.

| Comando | Finalidade |
| --- | --- |
| `npm run test:guard` | Testa localmente as travas de ambiente, sem abrir navegador. |
| `npm run test:jornada` | Cria dados sintéticos e percorre o fluxo dos quatro perfis. |
| `npm run test:navegacao` | Usa sessões independentes e testa links, rotas proibidas, ownership, logout e CSRF. |
| `npm run test:redesign` | Captura 390, 430, 768 e 1440 px, avalia reflow e testa os contratos interativos da V2 quando presentes. |
| `npm run test:a11y` | Executa axe-core WCAG 2.0/2.1 A e AA nas páginas reais. |
| `npm run test:qa` | Executa a sequência completa acima. |

`test:navegacao`, `test:redesign` e `test:a11y` devem ser executados depois de
`test:jornada`, pois reutilizam somente os identificadores e usuários
sintéticos registrados em `resultados-e2e.json`.

## Pré-requisitos

- Java 17 e aplicação CourtTime em uma instância separada;
- PostgreSQL 16 com banco novo, vazio e descartável chamado
  **exatamente** `courttime_e2e`;
- Node.js 20 ou superior;
- Chromium instalado pelo Playwright ou disponível no sistema.

```bash
cd qa/playwright
npm ci
# Somente se não houver um Chromium utilizável:
npx playwright install chromium
```

## Travas obrigatórias

Esta automação altera grade, usuários, documentos, contribuições e reservas.
Ela recusa bancos `courttime_demo` e `courttime_test`, alvos que não sejam
loopback HTTP na porta 18081 e administradores que não correspondam ao
marcador efêmero da execução.

Escolha um marcador novo, com 8 a 32 letras minúsculas ou números. A aplicação
de teste deve ser iniciada com o mesmo email e com o marcador no nome exibido:

```bash
export COURTTIME_E2E_INSTANCE_TOKEN=qa8c00b35
export COURTTIME_E2E_ADMIN_EMAIL=admin.qa8c00b35@e2e.invalid
export COURTTIME_E2E_ACK_ISOLATED_DB=YES
export COURTTIME_E2E_DATABASE_NAME=courttime_e2e
export COURTTIME_E2E_BASE_URL=http://127.0.0.1:18081

export COURTTIME_DB_URL=jdbc:postgresql://localhost:5432/courttime_e2e
export COURTTIME_DEV_USER_NAME="Administrador E2E qa8c00b35"
export COURTTIME_DEV_USER_EMAIL="$COURTTIME_E2E_ADMIN_EMAIL"
export COURTTIME_DEV_USER_PROFILE=ADMINISTRADOR
export COURTTIME_BOOTSTRAP_ADMIN_ENABLED=true
```

Antes do primeiro dado ser criado, o roteiro autentica esse administrador e
exige encontrar `COURTTIME_E2E_INSTANCE_TOKEN` na página. Isso vincula o alvo
HTTP ao bootstrap da instância descartável. Ainda é responsabilidade de quem
executa conferir no PostgreSQL que `current_database()` retorna
`courttime_e2e`; nenhuma variável declarativa substitui essa conferência.

Defina as senhas sintéticas somente no ambiente da sessão. Não as grave em
arquivo, linha de comando versionada, log ou captura:

```bash
export COURTTIME_E2E_PASSWORD='senha-sintetica-da-execucao'
export COURTTIME_DEV_USER_PASSWORD="$COURTTIME_E2E_PASSWORD"
export COURTTIME_DB_PASSWORD='senha-sintetica-do-postgres'
export COURTTIME_E2E_TARGET_SHA="$(git rev-parse HEAD)"
export COURTTIME_E2E_EVIDENCE_DIR="$PWD/evidencias"
# Opcional para usar Chromium do sistema:
export COURTTIME_E2E_CHROMIUM_PATH=/usr/bin/chromium
```

O banco deve estar vazio. A jornada cria as contas restantes e uma grade
controlada de segunda a quinta apenas como dado de teste; isso não declara o
horário oficial da ETEC.

## Evidências e interpretação

O diretório configurado recebe PNGs reais do navegador e JSONs com SHA,
cenários e resultados. Ele é ignorado pelo Git. Os estados significam:

- `PASS`: contrato realmente executado na aplicação;
- `FAIL`: regressão reproduzida;
- `PENDING`: componente de uma fase visual ainda não existe no SHA testado.

O teste de zoom usa o equivalente de reflow WCAG sobre uma referência de
1440 px: viewport CSS de 720 px para 200% e 360 px para 400%. Axe complementa,
mas não substitui, a verificação manual de teclado, foco e contraste.

A automação gera um PDF sintético identificado como teste. Ele não reproduz o
termo da ETEC, não contém dados pessoais e não representa assinatura.
