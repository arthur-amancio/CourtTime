# Demonstração HTTPS: Render Free + Neon Free

Uso acadêmico com dados inteiramente fictícios. Não é implantação institucional.
O [checkpoint](tcc/CONTINUIDADE-MISSAO.md) registra o que foi efetivamente
provisionado/testado; este guia não significa que exista uma aplicação publicada.

## Limite obrigatório: R$ 0

Antes de criar qualquer recurso, confirmar o workspace Render e conferir **Billing**:
sem forma de pagamento, sem plano pago, sem auto-upgrade. A documentação do
[Render Free](https://render.com/docs/free) informa que excedentes de tráfego e
build **podem ser cobrados quando há pagamento cadastrado**, mesmo com servidor
Free. Sem pagamento, o comportamento documentado é suspensão/bloqueio, não
contratação. Não cadastrar cartão para contornar uma recusa. Conferir uso dos
outros serviços do mesmo workspace e manter deploys manuais.

Em 03/11/2026, a documentação do [Neon](https://neon.com/docs/introduction/plans)
informa Free sem cartão, com limites rígidos: 100 CU-h/projeto/mês, 1 GB de
PostgreSQL/projeto, 5 GB de Object Storage/projeto e 5 GB de transferência/mês
compartilhada pelos serviços. Confirmar novamente plano e franquias na conta
antes de provisionar. Não ativar snapshots pagos, upgrades ou serviços extras.
Se a gratuidade/persistência não puder ser confirmada, parar o provisionamento.

Render Free: 0,1 CPU, 512 MB RAM, 750 horas/mês por workspace; dorme após
15 minutos sem tráfego, pode demorar a acordar e pode ser reiniciado/suspenso.
Não usar pings para mantê-lo acordado. Não tem disco persistente, SSH ou jobs
avulsos. Não usar Render Postgres Free, cujo banco expira após 30 dias.
Franquias e políticas podem mudar; não se promete disponibilidade permanente.

## 1. Preparar banco e PDFs antes de expor o serviço

1. Criar projeto **Neon Free**, PostgreSQL 16, região AWS `us-east-2` (Ohio),
   banco exclusivo `courttime_demo`. A aplicação Render também ficará em Ohio.
   Sem dados/importações da escola; sem habilitar testes nesse projeto.
2. Conferir `SELECT current_database(), current_user, version();` e confirmar
   `courttime_demo`. Em banco novo com schema `public` vazio, revisar/aplicar
   [schema-inicial-demo.sql](../scripts/deploy/schema-inicial-demo.sql) uma vez,
   pelo SQL Editor ou cliente privado. Ele cria as 12 tabelas/identidades,
   índices e relações das entidades atuais, em transação, e recusa banco
   incorreto ou esquema já ocupado. Não apaga nem atualiza um esquema existente.
   Alterações futuras exigem SQL próprio revisado; o perfil Render usa
   **`ddl-auto=validate`**, não uma suposta migração automática de produção.
3. Criar bucket **privado** no Neon Object Storage e credencial com somente
   `storage:read`/`storage:write` na branch de demonstração. Não usar chave
   administrativa da organização. Conferir plano Free e política privada.
4. Testar, com um PDF sintético novo: PUT, GET autenticado com hash idêntico,
   GET anônimo recusado, PUT repetido com `If-None-Match: *` recusado e DELETE
   somente dessa chave descartável. Reabrir o cliente e recuperar novamente.
   Não publicar URLs assinadas, tokens, PDFs individuais ou dumps.
5. Provisionar **primeiro administrador** antes do servidor público. Em acesso
   privado ao banco, inserir em `usuarios` nome/email `.invalid`, hash BCrypt
   gerado pelo `BCryptPasswordEncoder` existente, perfil `ADMINISTRADOR`,
   `ativo=true`, `versao_acesso=0`. Usar senha aleatória exclusiva (8–72
   caracteres); guardar a ficha de acesso somente em local privado da equipe.
   Não gravar SQL com senha/hash no Git. Como alternativa privada, o bootstrap
   existente pode ser executado uma vez em loopback com perfil local e
   `ddl-auto=validate`, antes da exposição. Desligá-lo imediatamente.
   O perfil `render` **recusa bootstrap habilitado** e exige administrador ativo.

Não executar `popular-demo.sql` na instalação pública: as contas compartilhadas
desse seed são explicitamente locais. Criar os demais três perfis na interface,
com senhas temporárias distintas, sem construir outro sistema de seed.

## 2. Variáveis privadas do serviço

| Variável | Valor/configuração |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `render` |
| `COURTTIME_DEMO_SYNTHETIC_DATA` | `true`, após conferir os dados fictícios |
| `COURTTIME_DB_URL` | `jdbc:postgresql://<host-neon>:5432/courttime_demo?sslmode=verify-full` |
| `COURTTIME_DB_USERNAME`, `COURTTIME_DB_PASSWORD` | Campos separados, privados |
| `COURTTIME_S3_BUCKET` | Nome do bucket privado confirmado |
| `AWS_ENDPOINT_URL_S3` | Endpoint HTTPS informado pelo Neon, sem credenciais/query |
| `AWS_REGION` | Região indicada pelo armazenamento (`us-east-2` quando aplicável) |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | Token ID e segredo S3 da credencial restrita |
| `COURTTIME_POSTGRES_TEST`, `COURTTIME_BOOTSTRAP_ADMIN_ENABLED` | `false` |

Usar o host de conexão direta do banco com pool Hikari máximo de três conexões.
Não pôr usuário/senha nem parâmetros adicionais na URL JDBC. A configuração
usa o truststore Java com validação de certificado **e hostname**; não usar
`sslmode=require`, `NonValidatingFactory` ou certificados ignorados.
Não substituir o truststore público por certificados privados da sessão Codex.
`COURTTIME_DOCUMENT_STORAGE_PATH` continua disponível apenas para storage local;
no perfil Render os PDFs vão ao S3. Nenhum segredo entra no Dockerfile/Blueprint.

## 3. Construir e implantar manualmente

Primeiro publicar o código/testes em PR draft dependente do #91 e verificar a
CI do novo HEAD. Não fazer merge para implantar esta branch autorizada.

```bash
./mvnw clean verify  # integrações somente com banco descartável configurado
docker build -t courttime-demo .
```

O Dockerfile usa JDK/JRE 17 com digest fixado, Maven Wrapper e usuário 10001.
O `-DskipTests` do empacotamento interno **não comprova testes**: a suíte é
executada separadamente. `.dockerignore` permite somente fontes/build,
excluindo configurações privadas. Em rede cloud com proxy/CA, utilizar os
secrets opcionais BuildKit `maven_settings`, `java_truststore`, `ca_bundle` e
`wget_config` (este pode apontar `ca_certificate=/tmp/build-ca.pem`). Não copiar
CA ou credenciais da sessão para a imagem. Manter verificação TLS habilitada.

Após a conferência de custo/workspace, usar o [render.yaml](../render.yaml)
com **um** Web Service, Free, Ohio, Docker, branch
`chore/render-neon-demo-gratuita`, deploy automático desligado, previews
desligados e `/healthz`. O Blueprint é uma configuração pronta para revisão;
validá-lo não cria serviço. Não criar outro serviço se já houver um correspondente.
Se usar criação direta via Dashboard/MCP, reproduzir esses campos; o MCP de
criação não expõe `healthCheckPath`, portanto conferir essa opção no Dashboard
antes de considerar a implantação concluída. Não modificar permissões da conta.

A porta vem de `PORT`; o processo escuta em `0.0.0.0`. O proxy do Render termina
HTTPS e informa os cabeçalhos encaminhados. Cookies de sessão são Secure,
HttpOnly e SameSite=Lax; CSRF e autorização existentes permanecem ativos.
Somente `/healthz` aceita HTTP interno: retorna `ok` após os inicializadores,
sem banco, segredos ou metadados. Outras páginas exigem HTTPS nesse perfil.
Não expor diretamente a porta interna nem adicionar outro proxy público.

Memória: heap até 45% do limite, Serial GC, metaspace 128 MB, code cache 32 MB,
direct memory 16 MB, threads HTTP máximas 16. Esses limites não garantem capacidade
para tráfego público intenso; medir inicialização, PDF e navegação no plano real.
A JVM limita a compilação JIT ao nível 1 para reduzir trabalho de inicialização
em 0,1 CPU. A sonda Docker tolera até dez minutos de início; a sonda HTTP do
Render deve continuar apontando para `/healthz`, que só aceita após readiness.
Abrir a demonstração com dez minutos de antecedência e conferir a sonda/login;
o tempo de despertar do plano gratuito não é garantia de disponibilidade.

## 4. Validar a demonstração publicada

1. Aguardar build/deploy concluído; confirmar SHA, Free, região e `/healthz=200`.
2. Confirmar HTTPS, redirecionamento de HTTP, cookie Secure/HttpOnly/SameSite e
   POST sem CSRF recusado. Não executar a suíte E2E destrutiva no Neon.
3. Entrar como administrador; cadastrar Secretaria, Segurança e Aluno fictícios
   pela interface. Guardar credenciais somente na ficha privada. Cadastrar a
   grade sintética na quadra criada pelo inicializador; nenhum horário é oficial.
4. Preparar A/B/C pelo [roteiro da banca](tcc/preparacao-banca/DEMONSTRACAO.md):
   rascunho com nove/dez participantes, formalização/PDF sintético, conferência
   e contribuição presenciais simuladas, aprovação, portaria e remarcação.
   A reserva da portaria precisa estar aprovada para o dia do uso; prepará-la
   antecipadamente, sem alterar datas no banco público para contornar o fluxo.
5. Conferir acesso de proprietário/perfis aos PDFs e recusa para anônimo/aluno
   alheio, inclusive tentando a URL do objeto sem credencial.
6. Registrar contagens de reservas/documentos e hashes de PDFs sintéticos.
   Reiniciar/reimplantar **o mesmo SHA** manualmente; entrar novamente (sessões
   são de memória) e conferir os mesmos registros/bytes. Banco e bucket são
   externos; o boot valida esquema, não limpa tabelas nem arquivos.

Não declarar persistência no Neon a partir de um teste só no emulador local.
Não afirmar que o endereço está online sem abrir/verificar a URL real.

## 5. Recuperar, encerrar e usar na escola

Aplicação dormindo: abrir o link HTTPS com antecedência e aguardar; não alterar
o plano. Erro de startup: consultar Events/logs privados, confirmar variáveis,
schema e administrador; não ativar `update/create-drop` para tentar corrigir.
Banco indisponível: conferir status/limites Neon e aguardar retomada; sem upgrade.
PDF ausente: conferir bucket/credencial e chave em `documentos_reserva`; restaurar
somente cópia sintética conferida, nunca tornar o bucket público. Senha inválida:
usar Administração → Usuários; se perder o único administrador, recuperar por
acesso privado, sem deixar bootstrap público. Dados consumidos: preparar outra
reserva pelo fluxo normal, sem limpar a base para repetir a cena.

Para backup manual, guardar `pg_dump -Fc` e os objetos referenciados em destino
privado, com SHA/hash. Restaurar em destinos novos e conferir ambos antes de
usar. Não configurar snapshots faturáveis nem confiar no disco do Render.
Encerrar sessões após a banca; suspensão manual do serviço é opção operacional,
sem excluir projeto/bucket automaticamente ou trocar planos/permissões.

Na escola, basta navegador e internet para o endereço HTTPS validado: não é
preciso instalar PostgreSQL/Docker. Se a rede/projeção falhar, usar capturas
fictícias identificadas como contingência, sem afirmar execução ao vivo.
O [guia local](desenvolvimento-local-rapido.md) permanece disponível. O erro no
PostgreSQL da ETEC só pode ser diagnosticado com log real; não se presume a causa.
