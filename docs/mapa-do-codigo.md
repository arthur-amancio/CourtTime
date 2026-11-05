# Mapa do código do CourtTime

Guia para explicar o código e localizar alterações. A candidata de bloqueios semanais parte do PR #94; não foi publicada no Render. Nesta candidata há 13 entidades; o runtime publicado e a monografia preservada continuam com 12.

## As camadas (de fora para dentro)

| Camada | Pasta | O que faz | Exemplo |
|---|---|---|---|
| Telas | `templates/` | HTML com Thymeleaf. Mostra dados e envia formulários. Não decide regras. | `solicitacoes/formulario.html` |
| Controllers | `controller/` | Recebem a requisição, chamam um serviço e escolhem a tela ou o redirecionamento. | `SolicitacaoReservaController` |
| Serviços | `service/` | Coordenam as regras, permissões e transações; usam cálculos derivados para preparar os resultados. | `ReservaService`, `PortariaService` |
| Entidades | `entity/` | As tabelas do banco como classes Java (JPA). | `Reserva`, `ParticipanteReserva` |
| Repositórios | `repository/` | Consultas ao PostgreSQL (Spring Data JPA). | `ReservaRepository` |
| Segurança | `security/` + `config/SecurityConfig` | Login por sessão, perfil de cada rota e verificação de perfil nos serviços. | `Permissao.exigir(usuario, Perfil.SECRETARIA)` |

Os `record` da pasta `service` agrupam dados, mas alguns também calculam valores derivados:
`ResumoPortaria.minimoAtingido` compara presenças com o mínimo; `ContribuicaoPendente`
calcula crédito aplicado, diferença e excedente. Esses cálculos não gravam no banco.
Permissões, locks e mudanças persistentes continuam nos serviços. Ao alterar uma conta
financeira ou um indicador, conferir também o `record` que produz o valor exibido.

## Quem pode o quê (três verificações)

1. **`SecurityConfig`:** cada perfil só abre as próprias rotas (`/admin/**`, `/secretaria/**`, `/seguranca/**`, `/solicitacoes/**`).
2. **`Permissao.exigir(...)`:** cada serviço confere de novo o perfil, para a regra valer mesmo se for chamada de outro lugar.
3. **Telas do aluno:** pedir a solicitação de outro aluno responde "página não encontrada" (`AreaDoAlunoExceptionHandler`), para ninguém descobrir números de outras solicitações.

## O caminho de uma solicitação

```
Aluno          → salva rascunho; dez participantes formalizam o pedido ReservaService, ParticipanteReservaService
Aluno          → gera o formulário oficial e envia o PDF assinado       FormularioOficialService, DocumentoReservaService
Secretaria     → confere a assinatura gov.br e valida o documento       ConferenciaDocumentoService
Secretaria     → registra a contribuição de R$ 2,00 por participante    ContribuicaoApmService
Administração  → aprova, rejeita ou cancela com motivo                  ReservaService
Segurança      → no dia: presença, pessoas a mais e liberação           ConsultaSegurancaService, PortariaService
```

A situação mostrada ao aluno em cada etapa vem do `SituacaoSolicitacaoService`.

## Onde está cada regra

| Regra | Classe | O que procurar |
|---|---|---|
| Mínimo de 10 participantes no total, contando Aluno e Outro | `ParticipanteReservaService` | `MINIMO_DE_PARTICIPANTES` |
| 1 dia de antecedência | `CalendarioDisponibilidadeService` | `DIAS_MINIMOS_DE_ANTECEDENCIA` |
| Bloqueio semanal dentro de vigência | `BloqueioSemanalQuadra`, `AdministracaoQuadraService` e `CalendarioDisponibilidadeService` | `ocorreEm`, `cadastrarBloqueioSemanal`, `obterBloqueios`; sem materializar ocorrências |
| Duas reservas no mesmo horário não são aprovadas | `ReservaService.aprovar` | trava da quadra e `periodoEstaDisponivel` |
| R01 — documento até o dia anterior | `DocumentoReservaService` | `prazoFinalDeEnvio` |
| R02 — mais de 30 participantes | `FormularioReservaPdf` | páginas extras do PDF |
| R03 — R$ 2,00 por participante | `ContribuicaoApmService` | `calcularValorDevido` |
| R04 — crédito na remarcação | `ContribuicaoApmService` e `ReservaService.criarRemarcacao` | `creditoAplicado` |
| R06 — documentos não são apagados | `DocumentoReservaService` e `DocumentoStorage` (local ou S3 privado no Render) | `remover` só apaga o arquivo de um envio que falhou |
| R07 — assinatura gov.br conferida pela secretaria | `ConferenciaDocumentoService` | confirmação obrigatória |
| R08 — presença na portaria | `ConsultaSegurancaService` | `registrarPresencas` |
| R09 — vínculo da categoria Outro | `ParticipanteReservaService` e `VinculoParticipante` | `preencher` |
| R10 — pessoas a mais na portaria | `PortariaService` | `acrescentarPessoa`, `calcularPessoasAPagar` |
| R11 — liberação com 10 presentes e sem valor pendente | `PortariaService` | `liberarQuadra` |

O CourtTime aplica o mínimo total por decisão atual do grupo. O formulário oficial versionado ainda menciona dez alunos; a ETEC precisa confirmar ou atualizar o termo antes da homologação documental. As regras e a divergência estão descritas em [regras-etec.md](regras-etec.md).

## Como explicar em 2 minutos uma regra (exemplo: R10)

1. **Tela.** A segurança preenche "Acrescentar pessoa" em `seguranca/reservas/detalhe.html`.
2. **Controller.** `SegurancaReservaController.acrescentarPessoa` recebe o formulário e chama o serviço.
3. **Serviço.** `PortariaService.acrescentarPessoa` confere o perfil, se a reserva está aprovada e se é o dia dela, e usa as mesmas validações do cadastro do aluno.
4. **Conta.** `PortariaService.resumir` usa `calcularPessoasAPagar`: `max(0, presentes − (pessoas da lista documental + adicionais já pagos))`. Multiplica o resultado por R$ 2,00. Uma substituição dentro dessa capacidade paga, antes da liberação, não gera novo complemento; o recebimento anterior permanece no histórico.
5. **Teste.** `PortariaServiceTest` tem um teste para cada caso: troca, acréscimo, falta, liberação e pagamento.

## Testes

- **Testes de unidade** (`src/test/java/.../service`): regras com Mockito, sem banco.
- **Testes de controller** (`.../controller`): rotas, perfis, CSRF e mensagens, com `MockMvc`.
- **Testes no PostgreSQL** (`*PostgreSqlIntegrationTest`): só rodam num banco com "test" no nome. O `courttime_demo` é recusado.
- **Navegador real** (`qa/playwright`): a jornada dos quatro perfis, as capturas e a acessibilidade, num banco descartável no GitHub Actions.

## Manutenção por funcionalidade

| Alterar | Ponto de entrada e cuidado | Verificar |
| --- | --- | --- |
| Rascunho/lista | `SolicitacaoReservaController`, `ParticipanteReservaService`; preservar propriedade e revisão documental | `ReservaServiceTest`, `ParticipanteReservaServiceTest`, integrações equivalentes |
| PDFs/conferência | `DocumentoReservaService`, `FormularioOficialService`, `ConferenciaDocumentoService`; não confundir hash/assinatura nem expor storage | Testes de documentos/conferência e acesso por perfil |
| Contribuição/crédito | `ContribuicaoApmService`; registro presencial e crédito vinculado à origem | `ContribuicaoApmServiceTest`, PostgreSQL financeiro |
| Portaria | `ConsultaSegurancaService`, `PortariaService`; dia, lock e estado liberado | `PortariaServiceTest`, `PresencaPostgreSqlIntegrationTest`, `PortariaPostgreSqlIntegrationTest` |
| Sessão/permissões | `SecurityConfig`, `RevalidacaoSessaoUsuarioFilter`, `Permissao`; rota + serviço + proprietário | Testes de controller, filtro, usuários e navegação dos quatro perfis |
| Grade/bloqueios | `AdminQuadraController`, `AdministracaoQuadraService`, calendário; lock da quadra compartilhado com aprovação | Calendário/Administração/Reserva unitários e PostgreSQL; navegador semanal após jornada **e finalização**, no mesmo candidato/base E2E |
| Ambiente publicado | Perfil Render, `ConfiguracaoRender`, `DocumentoS3Config`; validate e credenciais externas | Testes de configuração/storage + migração isolada antes de autorização de deploy |

A migração, os limites da recorrência, o impacto na modelagem e o procedimento de publicação
estão em [bloqueios-semanais.md](bloqueios-semanais.md). Alterações apenas de JavaDoc não mudam
regras. Não comentar acessores nem substituir testes por explicações.

## Localizar e verificar uma alteração

Os nomes da tabela correspondem aos arquivos em `src/main/java/br/com/courttime/`:
controller em `controller/`, formulário em `controller/form/`, regra em `service/`,
entidade em `entity/` e consulta em `repository/`. Os testes Java repetem o pacote
em `src/test/java/`; procure o nome da classe seguido de `Test` ou `PostgreSqlIntegrationTest`.

Para bloqueios, começar por [AdminQuadraController](../src/main/java/br/com/courttime/controller/AdminQuadraController.java),
seguir para [AdministracaoQuadraService](../src/main/java/br/com/courttime/service/AdministracaoQuadraService.java)
e [CalendarioDisponibilidadeService](../src/main/java/br/com/courttime/service/CalendarioDisponibilidadeService.java).
O primeiro recebe dados; o segundo grava sob lock; o terceiro calcula a disponibilidade.
Formulário e entidade repetem validações de fronteira de propósito: esconder um campo na tela
não protege uma chamada direta ao serviço ou uma gravação JPA.

| Mudança feita | Casos que devem continuar corretos | Testes mínimos afetados |
| --- | --- | --- |
| Dia/vigência/intervalo | Primeiro e último dia inclusivos, dia diferente, meia-noite recusada, minutos e períodos adjacentes | `BloqueioSemanalQuadraTest`, `AdministracaoQuadraServiceTest`, `CalendarioDisponibilidadeServiceTest` |
| Solicitação/remarcação/aprovação | Rascunho não ocupa vaga; remarcação passa pela criação comum; aprovação revalida sob lock | `ReservaServiceTest`, `ReservaPostgreSqlIntegrationTest` |
| Cadastro/desativação | Aprovada impede regra conflitante; desativar uma regra não remove outra nem o pontual | `AdministracaoQuadraPostgreSqlIntegrationTest` |
| Permissões ou formulário | POST direto, CSRF ausente, perfil diferente, campos inválidos e motivo escapado | `AdminQuadraControllerTest`, `test:bloqueios-semanais` |
| SQL/modelo persistente | Schema anterior restaurado, dados preservados e novo JAR com `ddl-auto=validate` | Ensaio isolado descrito no guia; não usar `update` como prova da migração |

Exemplo para a primeira linha e o formulário, sem integração com banco:

```bash
./mvnw -Dtest=BloqueioSemanalQuadraTest,AdministracaoQuadraServiceTest,CalendarioDisponibilidadeServiceTest,AdminQuadraControllerTest test
```

Para as integrações, configurar um banco descartável com `test` no nome pelo
[guia cloud](desenvolvimento-cloud.md), **conferir o banco realmente conectado** e só então
habilitar `COURTTIME_POSTGRES_TEST=true`. Os testes apagam os dados dessa base.
Para navegador, preferir `npm run test:qa` em outro banco E2E novo com as proteções do guia;
o teste semanal depende dos perfis/grade da jornada e da reserva aprovada da finalização.
Executá-lo sozinho em uma base apenas inicializada não substitui essa preparação.

Não remover locks ou unificar validações só porque os trechos parecem semelhantes:
o formulário, o serviço, a entidade e o banco protegem entradas diferentes.
Sem alteração de código e com as mesmas árvores/SHAs validados, revisar a evidência existente
evita repetir a suíte sem motivo; isso não transfere aprovação para código novo.
