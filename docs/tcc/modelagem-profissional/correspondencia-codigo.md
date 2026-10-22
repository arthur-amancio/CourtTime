# Correspondência entre diagramas e código

## Escopo analisado

A correspondência usa o commit `6fc1aee626c7478a35a93b5b70d7f70b4de1c042`, cabeça do PR #87 no início do trabalho. O histórico confirma que os PRs #85 e #86 são ancestrais dessa revisão. Os caminhos abaixo são relativos à raiz do repositório.

## Casos de uso e atividades

| Elemento modelado | Implementação principal | Evidência |
|---|---|---|
| Autorização por Aluno, Secretaria, Segurança e Administrador | `SecurityConfig` | `src/main/java/br/com/courttime/config/SecurityConfig.java` associa `/solicitacoes/**`, `/secretaria/**`, `/seguranca/**` e `/admin/**` aos perfis |
| Consultar agenda e criar solicitação | `SolicitacaoReservaController`, `ReservaService`, `CalendarioDisponibilidadeService` | criação valida quadra, faixa de horário, antecedência, disponibilidade e concorrência |
| Manter ao menos dez pessoas, sem cota por categoria | `ParticipanteReservaService` | constante `MINIMO_DE_PARTICIPANTES = 10` e validação do total da lista |
| Gerar e enviar formulário oficial | `FormularioOficialService`, `FormularioReservaPdf`, `DocumentoReservaService`, `DocumentoReservaController` | formulário é preenchido, versionado, armazenado e relacionado à revisão dos participantes |
| Conferir documento e assinatura | `ConferenciaDocumentoService` | resultado, justificativa, conferência da assinatura e usuário responsável |
| Registrar R$ 2,00 por participante | `ContribuicaoApmService` | constante `VALOR_POR_PARTICIPANTE = 2.00` e multiplicação pela quantidade |
| Aprovar, devolver ou cancelar | `ReservaService`, `AnaliseAdministrativaService`, `AdminSolicitacaoController` | transições de estado e justificativas administrativas |
| Remarcar cancelamento uma única vez | `ReservaService`, `ReservaRepository`, `Reserva.remarcacaoDe` | bloqueio pessimista da origem e FK autorreferente única |
| Copiar pessoas na remarcação | `ParticipanteReservaService.copiarParticipantes` | nova lista é criada sem alterar a reserva cancelada |
| Reaproveitar crédito da contribuição | `ContribuicaoApmService`, `ContribuicaoApm.contribuicaoOrigem` | crédito aplicado à nova contribuição com rastreio da origem |
| Consultar reservas aprovadas do dia | `ConsultaSegurancaService`, `SegurancaReservaController` | restringe a operação ao dia e à reserva aprovada |
| Marcar presente ou ausente | `ConsultaSegurancaService.registrarPresencas` | presença, responsável e instante são persistidos |
| Cadastrar pessoa adicional | `PortariaService.adicionarPessoa`, `ParticipanteReserva.marcarAdicionadoNaPortaria` | identificação, categoria e vínculo são preservados e a pessoa nasce presente |
| Cobrar somente vagas ainda não pagas | `PortariaService.calcularPessoasAPagar`, `ResumoPortaria` | `max(0, presentes - pagos)` considera lista original e pagamentos adicionais |
| Registrar pagamento na portaria e confirmar repasse | `PortariaService.registrarPagamento`, `PortariaService.confirmarRepasse`, `SecretariaContribuicaoController` | `LocalPagamento.PORTARIA` permanece aguardando confirmação da Secretaria |
| Liberar com dez presentes e nada pendente | `PortariaService.liberar`, `ResumoPortaria` | `MINIMO_DE_PRESENTES = 10` e valor pendente igual a zero |
| Impedir mudanças depois da liberação | `PortariaService`, `ConsultaSegurancaService`, `Reserva.liberadaEm` | as operações de presença e inclusão verificam o encerramento da conferência |

## Classes e tabelas

| Classe / tabela | Papel no modelo | Arquivo |
|---|---|---|
| `Usuario` / `usuarios` | identidade, perfil, ativação e versão de acesso | `src/main/java/br/com/courttime/entity/Usuario.java` |
| `Quadra` / `quadras` | recurso físico reservável | `src/main/java/br/com/courttime/entity/Quadra.java` |
| `DisponibilidadeQuadra` / `disponibilidades_quadra` | janela semanal configurada | `src/main/java/br/com/courttime/entity/DisponibilidadeQuadra.java` |
| `BloqueioQuadra` / `bloqueios_quadra` | indisponibilidade excepcional por data e faixa | `src/main/java/br/com/courttime/entity/BloqueioQuadra.java` |
| `Reserva` / `reservas` | agregado lógico do pedido, estado, análise, cancelamento, remarcação e liberação | `src/main/java/br/com/courttime/entity/Reserva.java` |
| `ParticipanteReserva` / `participantes_reserva` | pessoa prevista ou acrescentada, identificação e presença | `src/main/java/br/com/courttime/entity/ParticipanteReserva.java` |
| `DocumentoReserva` / `documentos_reserva` | versão e metadados do PDF enviado | `src/main/java/br/com/courttime/entity/DocumentoReserva.java` |
| `ConferenciaDocumento` / `conferencias_documento` | parecer da Secretaria sobre uma versão | `src/main/java/br/com/courttime/entity/ConferenciaDocumento.java` |
| `ContribuicaoApm` / `contribuicoes_apm` | valor devido, crédito e confirmação presencial | `src/main/java/br/com/courttime/entity/ContribuicaoApm.java` |
| `AjusteContribuicaoApm` / `ajustes_contribuicao_apm` | histórico de correções da contribuição | `src/main/java/br/com/courttime/entity/AjusteContribuicaoApm.java` |
| `PagamentoAdicional` / `pagamentos_adicionais` | cobrança de pessoas excedentes, local e repasse | `src/main/java/br/com/courttime/entity/PagamentoAdicional.java` |
| `AuditoriaUsuario` / `auditorias_usuario` | alteração administrativa de usuário | `src/main/java/br/com/courttime/entity/AuditoriaUsuario.java` |

## Matriz de relacionamentos e cardinalidades

| Origem | Destino | Cardinalidade modelada | Fundamento técnico |
|---|---|---|---|
| `Quadra` | `Reserva` | 1 para 0..* | `Reserva.quadra` é `ManyToOne`, obrigatória |
| `Quadra` | `DisponibilidadeQuadra` | 1 para 0..* | FK obrigatória `quadra_id` |
| `Quadra` | `BloqueioQuadra` | 1 para 0..* | FK obrigatória `quadra_id` |
| `Usuario` | `Reserva` como solicitante | 1 para 0..* | `solicitante_id` é obrigatório |
| `Usuario` | `Reserva` como analista, cancelador ou liberador | 0..1 por reserva; 0..* por usuário | FKs opcionais correspondentes ao estágio do processo |
| `Reserva` | `Reserva` de origem | 0..1 para 0..1 | `OneToOne` e `remarcacao_de_id` única |
| `Reserva` | `ParticipanteReserva` | 1 para 0..* | cada participante tem uma reserva obrigatória; o mínimo dez é regra de serviço, não cardinalidade estrutural |
| `Reserva` | `DocumentoReserva` | 1 para 0..* | versões identificadas por unicidade `(reserva_id, versao)` |
| `DocumentoReserva` | `ConferenciaDocumento` | 1 para 0..1 | FK `documento_id` obrigatória e única no lado da conferência |
| `Reserva` | `ContribuicaoApm` | 1 para 0..1 | `reserva_id` obrigatória e única |
| `ConferenciaDocumento` | `ContribuicaoApm` | 1 para 0..1 | `conferencia_documento_id` obrigatória e única |
| `ContribuicaoApm` de origem | `ContribuicaoApm` que usa crédito | 0..1 para 0..* | o JPA declara `ManyToOne`; a regra da remarcação reduz o uso prático da origem, sem restrição única no banco |
| `ContribuicaoApm` | `AjusteContribuicaoApm` | 1 para 0..* | FK obrigatória e histórico ordenável por instante |
| `Reserva` | `PagamentoAdicional` | 1 para 0..* | cada registro cobre uma quantidade positiva de pessoas adicionais |
| `Usuario` | registros operacionais | 1 para 0..* | FKs de envio, conferência, ajuste, presença, inclusão, pagamento, repasse e auditoria |

## Arquitetura

| Componente | Correspondência no projeto |
|---|---|
| Navegador e páginas renderizadas | `src/main/resources/templates`, `src/main/resources/static` e controladores MVC |
| Spring Security | `SecurityConfig`, `UsuarioDetailsService` e sessão HTTP |
| Serviços de aplicação | pacote `src/main/java/br/com/courttime/service` com métodos transacionais e regras |
| Spring Data JPA/Hibernate | pacote `repository`, entidades JPA e `spring.jpa.hibernate.ddl-auto=update` |
| PostgreSQL | driver declarado no `pom.xml` e URL padrão em `application.properties` |
| Apache PDFBox | dependência no `pom.xml` e `FormularioReservaPdf` |
| Diretório privado | serviço de armazenamento de documentos; os PDFs não são expostos como arquivo estático |
| Gov.br, ITI e WhatsApp | etapas humanas descritas pela interface e pelo fluxo; não há cliente de integração automática no código analisado |

Os diagramas de classes priorizam atributos e operações que explicam as regras do domínio. Getters, setters, construtores e objetos de apresentação foram omitidos para caber em A4 e evitar duplicação mecânica do código.
