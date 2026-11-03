# CourtTime na nuvem: testes e demonstração local

Executar a partir do checkout, em Bash/Linux, com JDK 17, Node 22, Python 3,
OpenSSL, PostgreSQL 16 (servidor e clientes) e Chromium disponíveis no `PATH`.
Usar o Maven Wrapper; não é necessário Maven global, Docker ou serviço público.
Conferir `java -version`, `node --version`, `python3 --version`,
`pg_ctl --version`, `psql --version` e `./mvnw --version` antes de instalar algo.
Se o JDK estiver fora do `PATH`, definir `JAVA_HOME` e acrescentar seu `bin`.

Na sessão cloud de 01/11/2026, Debian 13 sem sudo: Temurin 17.0.21,
Node 22.23.4 e PostgreSQL 16.15 foram instalados em diretórios do usuário,
com SHA-256 dos arquivos oficiais conferidos. PostgreSQL foi compilado com
`./configure --prefix=<diretorio-do-usuario> --without-readline --without-icu`,
`make -j4` e `make install`. Não foi necessária instalação de outro Chromium:
o disponível no sistema funcionou com Playwright 1.56.1 do lockfile.

## Rede do Maven

Se `curl` baixa o POM, mas Maven informa `Network is unreachable`, configurar
o proxy da sessão em um `settings.xml` **fora do Git**. Maven não utiliza
automaticamente `HTTPS_PROXY`. Preservar proxy e verificação TLS; não mudar
dependências para corrigir conectividade. Nesta nuvem funcionaram um proxy
HTTP com host/porta extraídos dessa variável, `nonProxyHosts` limitado a
`localhost|127.0.0.1` e o truststore `/etc/ssl/certs/java/cacerts`.
Acrescentar `-s <settings-privado.xml>` às chamadas do wrapper e, se necessário,
`MAVEN_OPTS=-Djavax.net.ssl.trustStore=<truststore-da-nuvem>`.
Esses caminhos são configuração do operador, não requisitos da aplicação.
Não imprimir variáveis de credenciais nem desabilitar certificados.

## Cluster descartável exclusivo desta execução

Em uma sessão reservada, sem `set -x`, executar como usuário comum. O bloco
cria um cluster novo e privado; não utiliza um servidor PostgreSQL existente.
A porta deve estar livre; se ocupada, escolher outra e manter `PGPORT` e JDBC
iguais. Não publicar o diretório criado nem projetar esse terminal.

```bash
set -euo pipefail
export CT_RUN_DIR="$(mktemp -d /tmp/courttime-cloud.XXXXXX)"
chmod 700 "$CT_RUN_DIR"
export PGHOST=127.0.0.1 PGPORT=55432 PGUSER=courttime_cloud
export PGPASSWORD="$(openssl rand -hex 24)"
initdb -D "$CT_RUN_DIR/pgdata" -U "$PGUSER" \
  --pwfile=<(printf '%s\n' "$PGPASSWORD") --auth=scram-sha-256 \
  --encoding=UTF8 --locale=C.UTF-8
pg_ctl -D "$CT_RUN_DIR/pgdata" -l "$CT_RUN_DIR/postgres.log" \
  -o "-h 127.0.0.1 -p $PGPORT -k $CT_RUN_DIR" start
createdb courttime_cloud_test
createdb courttime_e2e
export COURTTIME_DB_USERNAME="$PGUSER" COURTTIME_DB_PASSWORD="$PGPASSWORD"
export COURTTIME_DOCUMENT_STORAGE_PATH="$CT_RUN_DIR/documentos"
psql -d courttime_cloud_test -c 'SELECT current_database(), current_user;'
test "$(psql -d courttime_cloud_test -Atc 'SELECT current_database()')" = courttime_cloud_test
COURTTIME_POSTGRES_TEST=true SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop \
  COURTTIME_DB_URL="jdbc:postgresql://127.0.0.1:$PGPORT/courttime_cloud_test" \
  ./mvnw clean verify
```

A guarda Java exige que o nome do banco realmente conectado contenha `test`.
Não habilitar a suíte Java no banco de navegador ou demonstração. Um resultado
com testes ignorados não substitui a execução das integrações PostgreSQL.

## Navegador real com os quatro perfis

Continuar na mesma sessão, após o build aprovado. `PGPORT` também é utilizado
pelo `psql` chamado pela fixture de portaria. A aplicação deve escutar somente
em loopback. O banco E2E precisa estar novo e vazio.

```bash
export COURTTIME_DB_URL="jdbc:postgresql://127.0.0.1:$PGPORT/courttime_e2e"
export COURTTIME_POSTGRES_TEST=false
export COURTTIME_E2E_DATABASE_NAME=courttime_e2e
export COURTTIME_E2E_ACK_ISOLATED_DB=YES
export COURTTIME_E2E_INSTANCE_TOKEN="qa$(openssl rand -hex 6)"
export COURTTIME_E2E_ADMIN_EMAIL="admin.$COURTTIME_E2E_INSTANCE_TOKEN@e2e.invalid"
export COURTTIME_E2E_PASSWORD="$(openssl rand -hex 20)"
export COURTTIME_E2E_BASE_URL=http://127.0.0.1:18081
export COURTTIME_E2E_TARGET_SHA="$(git rev-parse HEAD)"
export COURTTIME_E2E_EVIDENCE_DIR="$CT_RUN_DIR/evidencias"
export COURTTIME_E2E_CHROMIUM_PATH="$(command -v chromium)"
export COURTTIME_DEV_USER_NAME="Administrador E2E $COURTTIME_E2E_INSTANCE_TOKEN"
export COURTTIME_DEV_USER_EMAIL="$COURTTIME_E2E_ADMIN_EMAIL"
export COURTTIME_DEV_USER_PASSWORD="$COURTTIME_E2E_PASSWORD"
export COURTTIME_DEV_USER_PROFILE=ADMINISTRADOR
export COURTTIME_BOOTSTRAP_ADMIN_ENABLED=true
SERVER_ADDRESS=127.0.0.1 SERVER_PORT=18081 \
  java -jar target/courttime-0.0.1-SNAPSHOT.jar > "$CT_RUN_DIR/app.log" 2>&1 &
CT_APP_PID=$!
```

Esperar o login responder em `http://127.0.0.1:18081/login`. Conferir antes
das mutações, na mesma sessão:

```bash
psql -d courttime_e2e -c 'SELECT current_database(), current_user;'
test "$(psql -d courttime_e2e -Atc 'SELECT current_database()')" = courttime_e2e
test "$(psql -d courttime_e2e -Atc 'SELECT count(*) FROM usuarios')" = 1
unset COURTTIME_BOOTSTRAP_ADMIN_ENABLED COURTTIME_DEV_USER_PASSWORD
(cd qa/playwright && npm ci && npm run test:qa)
```

Se não houver Chromium utilizável, seguir `npx playwright install chromium` e
os requisitos do sistema da versão instalada. As travas existentes continuam
obrigatórias: loopback/porta 18081, banco permitido e administrador com marcador
comprovado na página. A jornada cria Secretaria, Segurança e dois alunos pela
interface. Os identificadores estão em
[`qa/playwright/suporte/configuracao.cjs`](../qa/playwright/suporte/configuracao.cjs);
a senha é a variável privada da execução. Não versionar senhas ou relatórios
Surefire XML brutos, que podem conter propriedades do ambiente.

A fixture `definirDataDeDemonstracaoHoje` ajusta **somente** uma reserva aprovada
no banco E2E para exercitar a portaria hoje. É preparação artificial identificada,
não uma funcionalidade de remarcação. A conferência dos PDFs sintéticos simula
a decisão humana; não comprova assinatura gov.br. Os testes consomem estados,
cancelam reservas e desativam contas: essa base não é um cenário pronto de banca.

## Demonstração à banca

Usar outra sessão e outro cluster novo, com banco `courttime_demo`, sem habilitar
testes Java ou E2E. Apontar JDBC e PDFs para esse ambiente, conferir a conexão e
iniciar o mesmo JAR em loopback com bootstrap administrativo temporário. Criar
os outros três perfis em **Administração → Usuários**; guardar senhas distintas
somente na ficha privada. Cadastrar grade e quadra sintéticas pela interface.
O [roteiro existente](tcc/preparacao-banca/DEMONSTRACAO.md) define as contas,
os participantes e os cenários A/B/C; preparar B pelo fluxo normal no dia anterior.
O seed opcional `scripts/popular-demo.sql` exige `courttime_demo` e `pgcrypto`;
ele não prepara reservas aprovadas nem substitui o fluxo documental.

Variante de aproximadamente **quatro minutos**, com sessões independentes já
abertas e dados preparados; duração proposta, ainda depende de ensaio do grupo:

| Tempo | Ação |
| --- | --- |
| 0:00–0:35 | Mostrar login do aluno e calendário; abrir A, futura, com nove participantes. |
| 0:35–1:10 | Incluir o décimo; mostrar formalização e formulário já preparado. |
| 1:10–2:00 | Enviar PDF claramente sintético; Secretaria registra conferência e R$ 20,00 presenciais. |
| 2:00–2:35 | Administração confere e aprova A; explicar a revalidação de conflitos. |
| 2:35–3:40 | Anunciar troca para B, preparada para hoje; Segurança confere dez presentes e libera. |
| 3:40–4:00 | Mostrar encerramento da conferência e explicar que crédito/remarcação ficam no roteiro ampliado. |

Não representar o PDF sintético como assinatura válida, pagamento como operação
bancária ou a reserva criada hoje para o futuro como a reserva da portaria de hoje.
Substituição, complemento, repasse e remarcação completa foram exercitados pela
automação; exigem mais tempo na apresentação humana.

## Recuperar e encerrar

Perda de sessão: entrar novamente no perfil correto. Estado consumido: usar
outra reserva preparada ou reiniciar o procedimento em **outro cluster novo**;
não apagar dados para repetir a cena. Antes do ensaio, guardar `pg_dump -Fc`
somente da base sintética junto com cópia privada dos PDFs e o SHA. Restaurar
banco e PDFs em destinos novos, conferir conexão e integridade antes de usar.

```bash
kill "$CT_APP_PID"
wait "$CT_APP_PID" || true
pg_ctl -D "$CT_RUN_DIR/pgdata" -m fast stop
unset PGPASSWORD COURTTIME_DB_PASSWORD COURTTIME_E2E_PASSWORD
```

Conferir o encerramento dos processos e fechar a sessão reservada. Preservar
somente evidências revisadas, sem credenciais; não publicar o cluster/PDFs.
O ambiente temporário do Codex não é hospedagem permanente. Nenhum deploy
ou acesso público é necessário para este procedimento.

## Verificar o novo storage S3 sem atingir a demonstração

A hospedagem HTTPS está no [guia próprio](deploy-gratuito.md). Para a regressão
do protocolo, foi usado Moto `5.1.16` com servidor S3 HTTPS local e bucket
privado `courttime-s3-test`; ele é somente um emulador de QA, não infraestrutura
da aplicação. Criar esse bucket fora da suíte, bloquear acesso público e usar
credenciais exclusivamente sintéticas. O certificado deve conter localhost e
ser confiado em um truststore de laboratório separado, sem desabilitar TLS.

Configurar `AWS_ENDPOINT_URL_S3=https://localhost:<porta>`, `AWS_REGION`,
`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `COURTTIME_S3_TEST=true` e
`COURTTIME_S3_TEST_BUCKET=courttime-s3-test` somente nessa sessão. Passar o
truststore ao processo Java (`JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStore=<arquivo>`).
Então executar o Wrapper com `-Dtest=DocumentoStorageS3IntegrationTest test`.
São três testes de protocolo: recuperar os mesmos bytes após reabrir o cliente
e recusar GET anônimo; impedir sobrescrita concorrente; remover somente o objeto
descartável criado pelo teste. Cada teste usa UUID novo. A guarda recusa endpoint
fora de localhost/127.0.0.1 ou bucket diferente antes de escrever.

Sem essa configuração, somente esses três testes opcionais são ignorados;
isso não comprova aprovação do S3 real. A CI PostgreSQL continua independente.
Para exercitar toda a jornada no novo backend, usar a base E2E descartável acima
com `COURTTIME_DOCUMENT_STORAGE=s3` e `COURTTIME_S3_BUCKET=courttime-s3-test`.
Manter todas as travas E2E e o truststore local. Nunca apontar essas suítes para
o Neon/bucket publicados. O provedor real exige conferência separada de TLS,
política privada, escrita condicional e persistência, com PDF sintético avulso.
