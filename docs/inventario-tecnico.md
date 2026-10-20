# Inventário técnico do CourtTime

Levantamento fiel à implementação, extraído do código em `main` (`436b262`).
Serve de base verificável para os capítulos V, VI e VII da documentação
acadêmica.

**Nada aqui foi inventado.** As tabelas de atributos foram geradas a partir das
classes anotadas com JPA; os templates e as versões vêm do repositório. Onde
algo não existe, está dito que não existe.

---

# Capítulo V — Análise orientada a objetos

## Visão geral das camadas

| Camada | Pasta | Arquivos | Papel |
|---|---|---|---|
| Entidades | `entity/` | 11 entidades + 6 enums | estado persistido, mapeado por JPA |
| Repositórios | `repository/` | 11 | acesso a dados via Spring Data JPA |
| Serviços | `service/` | 28 | regras de negócio e objetos de transporte |
| Controladores | `controller/` | 14 | Spring MVC, recebem requisições e escolhem a tela |
| Configuração | `config/` | 3 | segurança, carga inicial da quadra e bootstrap do administrador |
| Segurança | `security/` | 2 | principal autenticado e revalidação de sessão |
| Armazenamento | `storage/` | 2 | gravação dos PDFs fora da área pública |

Os 28 arquivos de `service/` **não são 28 serviços**. Parte são `record`
usados como objeto de transporte entre serviço e tela — por exemplo
`SituacaoSolicitacao`, `ResumoParticipantes`, `DownloadDocumento`,
`DetalheReservaAutorizada`. Essa distinção importa no diagrama de classes: eles
não têm comportamento nem são persistidos.

## As onze entidades e suas responsabilidades

| Entidade | Responsabilidade |
|---|---|
| `Usuario` | conta institucional com perfil e versão de acesso |
| `Quadra` | a quadra esportiva; o sistema opera com uma |
| `DisponibilidadeQuadra` | faixa de funcionamento semanal, por dia da semana |
| `BloqueioQuadra` | indisponibilidade pontual em uma data |
| `Reserva` | a solicitação e todo o seu ciclo de vida |
| `ParticipanteReserva` | pessoa que vai usar a quadra naquela reserva |
| `DocumentoReserva` | versão de PDF enviada pelo aluno |
| `ConferenciaDocumento` | decisão da secretaria sobre uma versão |
| `ContribuicaoApm` | recebimento presencial registrado pela secretaria |
| `AjusteContribuicaoApm` | histórico de alteração de uma contribuição |
| `AuditoriaUsuario` | histórico de operações sobre contas |

As quatro últimas existem para **preservar histórico**: o sistema não sobrescreve
decisões nem apaga registros financeiros.

## Cardinalidades

```
Usuario   1 ──── N  Reserva                    (solicitante)
Usuario   1 ──── N  Reserva                    (analisadaPor, canceladaPor)
Quadra    1 ──── N  DisponibilidadeQuadra
Quadra    1 ──── N  BloqueioQuadra
Quadra    1 ──── N  Reserva
Reserva   1 ──── N  ParticipanteReserva
Reserva   1 ──── N  DocumentoReserva           (versionado)
DocumentoReserva 1 ──── 1 ConferenciaDocumento
Reserva   1 ──── 1  ContribuicaoApm
ContribuicaoApm  1 ──── N AjusteContribuicaoApm
Usuario   1 ──── N  AuditoriaUsuario           (operador e alvo)
```

Três observações que o diagrama precisa refletir:

1. `Reserva` referencia `Usuario` **três vezes** com papéis diferentes:
   solicitante, quem analisou e quem cancelou.
2. `DocumentoReserva` é **versionado**: uma reserva tem várias versões, e só a
   mais recente pode ser conferida.
3. `ContribuicaoApm` aponta para `ConferenciaDocumento`, e não só para a
   reserva. É isso que impede registrar contribuição referente a uma versão
   documental antiga.

## Enums

| Enum | Valores | Onde é usado |
|---|---|---|
| `Perfil` | `ALUNO`, `ADMINISTRADOR`, `SECRETARIA`, `SEGURANCA` | autorização e navegação |
| `StatusReserva` | `PENDENTE_DOCUMENTACAO`, `PENDENTE_ANALISE`, `APROVADA`, `REJEITADA`, `CANCELADA` | ciclo de vida da reserva |
| `CategoriaParticipante` | `ALUNO`, `OUTRO` | contagem do mínimo de dez alunos |
| `ResultadoConferenciaDocumento` | `VALIDADO`, `DEVOLVIDO_CORRECAO` | decisão da secretaria |
| `DiaSemana` | `SEGUNDA` a `DOMINGO` | grade semanal |
| `OperacaoAuditoriaUsuario` | `CADASTRO`, `EDICAO`, `ATIVACAO`, `DESATIVACAO` | auditoria de contas |

## Dicionário de atributos

Gerado das anotações JPA. "Obrigatório" é `nullable = false`; "Único" é
`unique = true`; "Tamanho" é `length`.

### `Usuario` — tabela `usuarios`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `nome` | `String` | — | sim | não | 150 |
| `email` | `String` | — | sim | sim | 254 |
| `senha` | `String` | — | sim | não | 100 |
| `perfil` | `Perfil` | — | sim | não | 20 |
| `versaoAcesso` | `long` | — | sim | não | — |

### `Quadra` — tabela `quadras`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `nome` | `String` | — | sim | não | 100 |
| `descricao` | `String` | — | sim | não | 500 |

### `DisponibilidadeQuadra` — tabela `disponibilidades_quadra`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `diaSemana` | `DiaSemana` | — | sim | não | 10 |
| `horarioInicio` | `LocalTime` | — | sim | não | — |
| `horarioFim` | `LocalTime` | — | sim | não | — |
| `quadra` | `Quadra` | N:1 | sim | não | — |

### `BloqueioQuadra` — tabela `bloqueios_quadra`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `data` | `LocalDate` | — | sim | não | — |
| `horarioInicio` | `LocalTime` | — | sim | não | — |
| `horarioFim` | `LocalTime` | — | sim | não | — |
| `motivo` | `String` | — | sim | não | 200 |
| `quadra` | `Quadra` | N:1 | sim | não | — |

### `Reserva` — tabela `reservas`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `quadra` | `Quadra` | N:1 | sim | não | — |
| `solicitante` | `Usuario` | N:1 | sim | não | — |
| `dataUtilizacao` | `LocalDate` | — | sim | não | — |
| `horarioInicio` | `LocalTime` | — | sim | não | — |
| `horarioFim` | `LocalTime` | — | sim | não | — |
| `finalidade` | `String` | — | sim | não | 300 |
| `revisaoParticipantes` | `int` | — | sim | não | — |
| `analisadaPor` | `Usuario` | N:1 | não | não | — |
| `motivoRejeicao` | `String` | — | não | não | 500 |
| `motivoCancelamentoInstitucional` | `String` | — | não | não | 500 |
| `canceladaPor` | `Usuario` | N:1 | não | não | — |
| `criadaEm` | `Instant` | — | sim | não | — |
| `atualizadaEm` | `Instant` | — | sim | não | — |
| `analisadaEm` | `Instant` | — | não | não | — |
| `canceladaEm` | `Instant` | — | não | não | — |

### `ParticipanteReserva` — tabela `participantes_reserva`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `reserva` | `Reserva` | N:1 | sim | não | — |
| `nomeCompleto` | `String` | — | sim | não | 150 |
| `rmOuRg` | `String` | — | sim | não | 30 |
| `categoria` | `CategoriaParticipante` | — | sim | não | 10 |

### `DocumentoReserva` — tabela `documentos_reserva`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `reserva` | `Reserva` | N:1 | sim | não | — |
| `versao` | `int` | — | sim | não | — |
| `revisaoParticipantes` | `int` | — | sim | não | — |
| `nomeOriginal` | `String` | — | sim | não | 255 |
| `chaveArmazenamento` | `String` | — | sim | sim | 80 |
| `sha256` | `String` | — | sim | não | 64 |
| `tamanhoBytes` | `long` | — | sim | não | — |
| `enviadoPor` | `Usuario` | N:1 | sim | não | — |
| `enviadoEm` | `Instant` | — | sim | não | — |
| `conferencia` | `ConferenciaDocumento` | 1:1 | não | não | — |

### `ConferenciaDocumento` — tabela `conferencias_documento`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `documento` | `DocumentoReserva` | 1:1 | sim | sim | — |
| `resultado` | `ResultadoConferenciaDocumento` | — | sim | não | 30 |
| `justificativa` | `String` | — | não | não | 500 |
| `conferidoPor` | `Usuario` | N:1 | sim | não | — |
| `conferidoEm` | `Instant` | — | sim | não | — |

### `ContribuicaoApm` — tabela `contribuicoes_apm`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `reserva` | `Reserva` | 1:1 | sim | não | — |
| `conferenciaDocumento` | `ConferenciaDocumento` | 1:1 | sim | não | — |
| `valor` | `BigDecimal` | — | sim | não | — |
| `observacoes` | `String` | — | não | não | 500 |
| `registradaPor` | `Usuario` | N:1 | sim | não | — |
| `registradaEm` | `Instant` | — | sim | não | — |

### `AjusteContribuicaoApm` — tabela `ajustes_contribuicao_apm`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `contribuicao` | `ContribuicaoApm` | N:1 | sim | não | — |
| `valorAnterior` | `BigDecimal` | — | sim | não | — |
| `valorNovo` | `BigDecimal` | — | sim | não | — |
| `observacoesAnteriores` | `String` | — | não | não | 500 |
| `observacoesNovas` | `String` | — | não | não | 500 |
| `motivo` | `String` | — | sim | não | 500 |
| `ajustadaPor` | `Usuario` | N:1 | sim | não | — |
| `ajustadaEm` | `Instant` | — | sim | não | — |

### `AuditoriaUsuario` — tabela `auditorias_usuario`

| Atributo | Tipo Java | Relacionamento | Obrigatório | Único | Tamanho |
|---|---|---|---|---|---|
| `id` | `Long` | — | não | não | — |
| `operador` | `Usuario` | N:1 | sim | não | — |
| `usuarioAfetado` | `Usuario` | N:1 | sim | não | — |
| `operacao` | `OperacaoAuditoriaUsuario` | — | sim | não | 30 |
| `perfilAnterior` | `Perfil` | — | não | não | 20 |
| `perfilNovo` | `Perfil` | — | não | não | 20 |
| `ativoAnterior` | `Boolean` | — | não | não | — |
| `ativoNovo` | `Boolean` | — | não | não | — |
| `realizadaEm` | `Instant` | — | sim | não | — |

## Restrições e relacionamentos no PostgreSQL

O esquema é gerado pelo Hibernate a partir das entidades
(`spring.jpa.hibernate.ddl-auto=update`). **Não há migrações versionadas** —
é uma limitação real do projeto e vale registrar na documentação.

Restrições efetivamente declaradas no modelo:

- chave primária `id` com geração automática em todas as entidades;
- `usuarios.email` único;
- `nullable = false` nos campos obrigatórios listados acima;
- `length` limitando colunas de texto;
- índices em `reservas` para consulta por quadra, data e status, e por solicitante;
- bloqueio pessimista (`SELECT ... FOR UPDATE`) em quadra, reserva e usuário
  durante a aprovação, para impedir autorizações simultâneas do mesmo período.

---

# Capítulo VI — Telas

28 templates Thymeleaf. Nenhum framework de frontend; HTML, CSS e Thymeleaf.
A matriz abaixo registra o contrato que um redesign precisa preservar. “Teste”
indica a cobertura automatizada existente; a jornada Playwright complementa as
verificações de navegador, responsividade e integração entre perfis.

| Tela (template) | Rota | Perfil autorizado | Ações reais | Dados apresentados ou enviados | Restrições que a interface deve preservar | Testes atuais |
|---|---|---|---|---|---|---|
| Página pública (`index.html`) | `GET /` | Público | Ir ao login | Identidade e finalidade do sistema | Não prometer aprovação, assinatura ou pagamento eletrônico | `HomeControllerTest` |
| Login (`login.html`) | `GET /login`, `POST /login` | Público | Autenticar | Email, senha e mensagens da sessão | Senha nunca volta no HTML; falha não revela existência da conta | `HomeControllerTest`; jornada Playwright |
| Início (`inicio.html`) | `GET /inicio` | Autenticado | Navegar para ações do perfil | Usuário, perfil e, para aluno, situação mais recente | Atalhos variam por perfil; situação não pode alegar autorização indevida | `InicioControllerTest`, `HomeControllerTest` |
| Erro (`error.html`) | despacho de erro | Conforme requisição | Voltar ao início | Código e mensagem pública | Sem stack trace, SQL, chave de arquivo ou detalhe interno | `PaginaDeErroTest`, `PaginaDeErroServidorTest` |
| Consulta da quadra (`quadra.html`) | `GET /quadra?data=` | Autenticado | Mudar data; iniciar solicitação quando disponível | Quadra, estado operacional, data e períodos | Períodos vêm do backend; indisponível não oferece ação de solicitar | `QuadraControllerTest`; jornada Playwright |
| Nova solicitação (`solicitacoes/formulario.html`) | `GET /solicitacoes/nova`, `POST /solicitacoes` | Aluno | Consultar data; registrar | `data`, `horarioInicio`, `finalidade` | Um dia de antecedência, uma hora, grade/bloqueio/conflito; servidor revalida | `SolicitacaoReservaControllerTest`, `ReservaServiceTest`; jornada Playwright |
| Minhas solicitações (`solicitacoes/lista.html`) | `GET /solicitacoes` | Aluno | Abrir detalhe; iniciar nova | Somente solicitações do usuário e situação derivada | Ownership obrigatório; uma situação principal por solicitação | `SolicitacaoReservaControllerTest`, `SituacaoSolicitacaoServiceTest` |
| Detalhe do aluno (`solicitacoes/detalhe.html`) | `GET /solicitacoes/{id}` | Aluno proprietário | Abrir participantes/documentos conforme etapa | Período, finalidade, situação e motivos aplicáveis | ID alheio retorna 404; pendente não aparece como aprovado | `SolicitacaoReservaControllerTest`; jornada Playwright |
| Participantes (`solicitacoes/participantes/lista.html`) | `GET /solicitacoes/{id}/participantes`; `POST .../{participanteId}/remover` | Aluno proprietário | Adicionar, editar, remover | Contagens separadas, nome, RM/RG e categoria | Mínimo é dez alunos; lista fica somente leitura após envio; CSRF e ownership | `ParticipanteReservaControllerTest`, `ParticipanteReservaServiceTest`; jornada Playwright |
| Formulário de participante (`solicitacoes/participantes/formulario.html`) | `GET/POST /solicitacoes/{id}/participantes/...` | Aluno proprietário | Incluir ou atualizar | `nomeCompleto`, `rmOuRg`, `categoria` | Campos obrigatórios; 150/30 caracteres; participante precisa pertencer à reserva | `ParticipanteReservaControllerTest`; jornada Playwright |
| Documentos do aluno (`solicitacoes/documentos/lista.html`) | `GET/POST /solicitacoes/{id}/documentos`; `GET .../{documentoId}/baixar` | Aluno proprietário | Enviar nova versão; baixar original | PDF, versões, tamanho, envio e conferência | PDF válido até 10 MB; bytes imutáveis; armazenamento privado; upload só na etapa permitida | `DocumentoReservaControllerTest`, `DocumentoReservaServiceTest`, `DocumentoStorageLocalTest`; jornada Playwright |
| Painel administrativo (`admin/painel.html`) | `GET /admin` | Administrador | Navegar para solicitações, usuários, grade e bloqueios | Métricas reais da quadra e solicitações | Nenhuma ação fictícia; não expor funções a outros perfis | `AdminQuadraControllerTest` |
| Fila administrativa (`admin/solicitacoes/lista.html`) | `GET /admin/solicitacoes` | Administrador | Abrir análise ou reserva autorizada | Pendências e aprovadas em listas separadas | Pendentes nunca aparecem como autorização | `AdminSolicitacaoControllerTest`; jornada Playwright |
| Análise administrativa (`admin/solicitacoes/detalhe.html`) | `GET /admin/solicitacoes/{id}`; `POST .../aprovar`, `/rejeitar`, `/cancelar`; `GET .../documentos/{documentoId}/baixar` | Administrador | Baixar PDF; aprovar; rejeitar; cancelar | Pedido, dez alunos, documento, contribuição e histórico de decisão | Aprovação sem motivo; rejeição/cancelamento com motivo; transação revalida horário e elegibilidade | `AdminSolicitacaoControllerTest`, `AnaliseAdministrativaServiceTest`; jornada Playwright |
| Usuários (`admin/usuarios/lista.html`) | `GET /admin/usuarios`; `POST .../{id}/alternar` | Administrador | Filtrar; cadastrar; editar; ativar/desativar; redefinir senha | Nome, email, perfil, situação e paginação | Não expor hash; não desativar último admin; CSRF; sessão revogada após bloqueio | `AdminUsuarioControllerTest`, `AdministracaoUsuarioServiceTest`; jornada Playwright |
| Cadastro de usuário (`admin/usuarios/cadastro.html`) | `GET/POST /admin/usuarios/novo` | Administrador | Criar conta | Nome, email, perfil, senha e confirmação | 8–72 caracteres; confirmação; não permite criar outro administrador por esse fluxo | `AdminUsuarioControllerTest` |
| Edição de usuário (`admin/usuarios/edicao.html`) | `GET/POST /admin/usuarios/{id}/editar` | Administrador | Atualizar identidade e perfil permitido | Nome, email e perfil | Administrador não tem seu perfil rebaixado pelo formulário comum | `AdminUsuarioControllerTest` |
| Redefinição de senha (`admin/usuarios/senha.html`) | `GET/POST /admin/usuarios/{id}/senha` | Administrador | Definir nova senha | Nova senha e confirmação | Nunca renderizar credencial ou hash anterior; 8–72 caracteres | `AdminUsuarioControllerTest` |
| Horários semanais (`admin/horarios/lista.html`) | `GET /admin/horarios`; `POST .../{id}/alternar` | Administrador | Incluir, editar, ativar/desativar | Dia, início, fim e estado | Alteração não pode comprometer reserva aprovada; CSRF | `AdminQuadraControllerTest`, `AdministracaoQuadraServiceTest`; jornada Playwright |
| Formulário de horário (`admin/horarios/formulario.html`) | `GET/POST /admin/horarios/novo`; `GET/POST .../{id}/editar` | Administrador | Cadastrar ou editar faixa | `diaSemana`, `horarioInicio`, `horarioFim` | Fim posterior ao início; sem sobreposição; backend revalida | `AdminQuadraControllerTest` |
| Bloqueios (`admin/bloqueios/lista.html`) | `GET /admin/bloqueios`; `POST .../{id}/desativar` | Administrador | Incluir; desativar | Data, período, motivo e estado | Desativação explícita; sem exclusão de histórico; CSRF | `AdminQuadraControllerTest` |
| Formulário de bloqueio (`admin/bloqueios/formulario.html`) | `GET/POST /admin/bloqueios/novo` | Administrador | Cadastrar bloqueio | `data`, início, fim e motivo | Não pode sobrepor reserva aprovada; motivo obrigatório; fim posterior | `AdminQuadraControllerTest`, `AdministracaoQuadraServiceTest`; jornada Playwright |
| Fila documental (`secretaria/documentos/lista.html`) | `GET /secretaria/documentos` | Secretaria | Abrir conferência ou histórico | Pendentes e conferidos | Não oferece aprovação de reserva; versões substituídas não voltam à fila | `SecretariaDocumentoControllerTest` |
| Conferência documental (`secretaria/documentos/detalhe.html`) | `GET /secretaria/documentos/{documentoId}`; `GET .../baixar`; `POST .../validar`, `/devolver` | Secretaria | Baixar original; validar; devolver | Metadados do PDF e histórico; justificativa de devolução | Devolução exige justificativa; só a versão mais recente; PDF privado e imutável | `SecretariaDocumentoControllerTest`, `ConferenciaDocumentoServiceTest`; jornada Playwright |
| Contribuições (`secretaria/contribuicoes/lista.html`) | `GET /secretaria/contribuicoes`; `POST .../solicitacoes/{reservaId}` | Secretaria | Registrar recebimento presencial | Pedido, situação, valor e observações | Somente após documento validado; valor informado, não calculado; sem duplicidade | `SecretariaContribuicaoControllerTest`, `ContribuicaoApmServiceTest`; jornada Playwright |
| Histórico da contribuição (`secretaria/contribuicoes/detalhe.html`) | `GET /secretaria/contribuicoes/solicitacoes/{id}`; `POST /secretaria/contribuicoes/{id}/ajustes` | Secretaria | Consultar; adicionar ajuste | Registro original e ajustes append-only | Ajuste exige motivo; histórico original não é sobrescrito | `SecretariaContribuicaoControllerTest`, `ContribuicaoApmServiceTest` |
| Reservas autorizadas (`seguranca/reservas/lista.html`) | `GET /seguranca/reservas` | Segurança | Abrir conferência | Somente reservas atualmente aprovadas | Consulta somente leitura; canceladas, rejeitadas e pendentes ausentes | `SegurancaReservaControllerTest`, `ConsultaSegurancaServiceTest`; jornada Playwright |
| Conferência de entrada (`seguranca/reservas/detalhe.html`) | `GET /seguranca/reservas/{id}` | Segurança | Voltar à lista | Período, finalidade, nomes, RM/RG e categoria | Só reserva aprovada; sem documento, contribuição, email ou controles de escrita | `SegurancaReservaControllerTest`; jornada Playwright |

## Status das evidências visuais

A distinção importa para não apresentar à banca algo como o que não é:

| Tipo | Situação |
|---|---|
| **Protótipo** | não existe; o projeto não passou por fase de protótipo formal |
| **Renderização estática** | existia para as telas anteriores à integração. Continua sendo diferente de uma execução real. |
| **Captura da aplicação em execução** | existe uma baseline reproduzível do SHA `436b262`, feita em Chromium com Spring Boot e PostgreSQL reais, nos viewports 390, 430, 768 e 1440 px. |

Uma seleção sem dados reais está em
[`docs/evidencias/baseline-436b262`](evidencias/baseline-436b262/README.md).
Ela não substitui os testes funcionais: o JSON e o roteiro Playwright preservam
a diferença entre evidência visual e comprovação de comportamento.

---

# Capítulo VII — Tecnologias

Versões efetivamente usadas, conferidas em `pom.xml` e no workflow do CI.

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | **17** | linguagem; `java.version` no `pom.xml` e `setup-java` no CI |
| Spring Boot | **3.5.16** | framework base, via `spring-boot-starter-parent` |
| Spring MVC | do Spring Boot 3.5.16 | controladores e roteamento (`spring-boot-starter-web`) |
| Thymeleaf | do Spring Boot 3.5.16 | renderização das telas no servidor |
| Spring Security | do Spring Boot 3.5.16 | autenticação por sessão, autorização por perfil, CSRF |
| Spring Data JPA | do Spring Boot 3.5.16 | repositórios |
| Hibernate | do Spring Boot 3.5.16 | implementação JPA; gera o esquema |
| PostgreSQL | **16** no CI | banco de dados; driver `org.postgresql:postgresql` |
| Maven Wrapper | **3.9.16** | build sem instalar Maven |
| HTML e CSS | — | 27 templates e uma folha de estilo |
| JavaScript | **nenhum** | não há arquivo `.js` no projeto |
| JUnit 5, Mockito, Spring Test | do `spring-boot-starter-test` | testes |
| Spring Security Test | do Spring Boot 3.5.16 | testes de autorização e CSRF |
| GitHub Actions | — | CI com PostgreSQL 16 temporário |

Duas observações que valem no texto acadêmico:

1. **Não há JavaScript no projeto.** A responsividade, inclusive a conversão de
   tabelas em cartões no celular, é feita só com CSS. É uma decisão defensável
   e simples de explicar.
2. **Nenhuma biblioteca de frontend** foi adicionada: sem Bootstrap, sem
   Tailwind, sem jQuery. O CSS é próprio.

---

# O que o sistema NÃO faz

Para a documentação acadêmica não prometer o que o código não entrega:

- gera o formulário oficial preenchido, mas não o assina nem registra aprovação;
- não verifica criptograficamente assinatura de PDF — a conferência é humana;
- não calcula o valor da contribuição à APM;
- remarca após cancelamento só como nova solicitação, sem reaproveitar aprovação, PDF ou contribuição;
- não registra contato com o aluno;
- não registra ocorrências pela segurança;
- não tem regra de retenção dos documentos enviados;
- não tem migrações de banco versionadas.
