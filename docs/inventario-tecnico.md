# Inventário técnico do CourtTime

Levantamento fiel à implementação, extraído do código em `main` (`83ef3e9`).
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

27 templates Thymeleaf. Nenhum framework de frontend; HTML, CSS e Thymeleaf.

| Tela | Arquivo | Perfil | Caso de uso |
|---|---|---|---|
| Página pública | `index.html` | visitante | apresentação do sistema |
| Login | `login.html` | todos | autenticação |
| Início | `inicio.html` | todos | acesso rápido e situação da solicitação mais recente |
| Consulta da quadra | `quadra.html` | todos autenticados | consultar disponibilidade por data |
| Nova solicitação | `solicitacoes/formulario.html` | aluno | criar solicitação |
| Minhas solicitações | `solicitacoes/lista.html` | aluno | acompanhar situação |
| Detalhe da solicitação | `solicitacoes/detalhe.html` | aluno | ver estado, motivo de rejeição ou cancelamento |
| Participantes | `solicitacoes/participantes/lista.html` | aluno | revisar lista e contagem de alunos |
| Cadastro de participante | `solicitacoes/participantes/formulario.html` | aluno | incluir ou editar participante |
| Documentos | `solicitacoes/documentos/lista.html` | aluno | enviar PDF e ver versões |
| Painel administrativo | `admin/painel.html` | administrador | ponto de entrada |
| Fila de análise | `admin/solicitacoes/lista.html` | administrador | solicitações prontas e reservas autorizadas |
| Análise da solicitação | `admin/solicitacoes/detalhe.html` | administrador | aprovar, rejeitar, cancelar |
| Usuários | `admin/usuarios/lista.html` | administrador | gerenciar contas |
| Cadastro de usuário | `admin/usuarios/cadastro.html` | administrador | criar conta |
| Edição de usuário | `admin/usuarios/edicao.html` | administrador | alterar dados |
| Senha de usuário | `admin/usuarios/senha.html` | administrador | redefinir senha |
| Horários | `admin/horarios/lista.html` | administrador | grade semanal |
| Cadastro de horário | `admin/horarios/formulario.html` | administrador | incluir ou editar faixa |
| Bloqueios | `admin/bloqueios/lista.html` | administrador | bloqueios por data |
| Cadastro de bloqueio | `admin/bloqueios/formulario.html` | administrador | incluir bloqueio |
| Fila documental | `secretaria/documentos/lista.html` | secretaria | documentos a conferir e histórico |
| Conferência do documento | `secretaria/documentos/detalhe.html` | secretaria | validar ou devolver |
| Contribuições | `secretaria/contribuicoes/lista.html` | secretaria | registrar recebimento |
| Histórico da contribuição | `secretaria/contribuicoes/detalhe.html` | secretaria | consultar e ajustar |
| Reservas autorizadas | `seguranca/reservas/lista.html` | segurança | consulta na portaria |
| Conferência de entrada | `seguranca/reservas/detalhe.html` | segurança | identificar participantes |

## Status das evidências visuais

A distinção importa para não apresentar à banca algo como o que não é:

| Tipo | Situação |
|---|---|
| **Protótipo** | não existe; o projeto não passou por fase de protótipo formal |
| **Renderização estática** | existe para as 27 telas, com o CSS real, usada nas auditorias de responsividade e acessibilidade. **Não é a aplicação em execução** |
| **Captura da aplicação em execução** | **ainda não existe** |

**Todas as telas acima precisam ser capturadas em navegador real** antes de
entrarem na documentação acadêmica como evidência de funcionamento. O que já
existe é verificação automatizada do fluxo por HTTP, não captura de tela.

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

- não gera o formulário oficial da ETEC preenchido;
- não verifica criptograficamente assinatura de PDF — a conferência é humana;
- não calcula o valor da contribuição à APM;
- não tem reagendamento após cancelamento;
- não registra contato com o aluno;
- não registra ocorrências pela segurança;
- não tem regra de retenção dos documentos enviados;
- não tem migrações de banco versionadas.
