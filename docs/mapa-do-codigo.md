# Mapa do código do CourtTime

Uma página para explicar o código na banca. Cada parte cabe em uma frase.

## As camadas (de fora para dentro)

| Camada | Pasta | O que faz | Exemplo |
|---|---|---|---|
| Telas | `templates/` | HTML com Thymeleaf. Mostra dados e envia formulários. Não decide regras. | `solicitacoes/formulario.html` |
| Controllers | `controller/` | Recebem a requisição, chamam um serviço e escolhem a tela ou o redirecionamento. | `SolicitacaoReservaController` |
| Serviços | `service/` | **Onde ficam as regras.** Validam, calculam e gravam. | `ReservaService`, `PortariaService` |
| Entidades | `entity/` | As tabelas do banco como classes Java (JPA). | `Reserva`, `ParticipanteReserva` |
| Repositórios | `repository/` | Consultas ao PostgreSQL (Spring Data JPA). | `ReservaRepository` |
| Segurança | `security/` + `config/SecurityConfig` | Login por sessão, perfil de cada rota e verificação de perfil nos serviços. | `Permissao.exigir(usuario, Perfil.SECRETARIA)` |

Os `record` da pasta `service` (como `ResumoPortaria` e `DetalheReservaAutorizada`) não têm regra: só levam os dados prontos para a tela.

## Quem pode o quê (três verificações)

1. **`SecurityConfig`:** cada perfil só abre as próprias rotas (`/admin/**`, `/secretaria/**`, `/seguranca/**`, `/solicitacoes/**`).
2. **`Permissao.exigir(...)`:** cada serviço confere de novo o perfil, para a regra valer mesmo se for chamada de outro lugar.
3. **Telas do aluno:** pedir a solicitação de outro aluno responde "página não encontrada" (`AreaDoAlunoExceptionHandler`), para ninguém descobrir números de outras solicitações.

## O caminho de uma solicitação

```
Aluno          → cria a solicitação e cadastra os participantes        ReservaService, ParticipanteReservaService
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
| Duas reservas no mesmo horário não são aprovadas | `ReservaService.aprovar` | trava da quadra e `periodoEstaDisponivel` |
| R01 — documento até o dia anterior | `DocumentoReservaService` | `prazoFinalDeEnvio` |
| R02 — mais de 30 participantes | `FormularioReservaPdf` | páginas extras do PDF |
| R03 — R$ 2,00 por participante | `ContribuicaoApmService` | `calcularValorDevido` |
| R04 — crédito na remarcação | `ContribuicaoApmService` e `ReservaService.criarRemarcacao` | `creditoAplicado` |
| R06 — documentos não são apagados | `DocumentoReservaService` e `DocumentoStorageLocal` | `remover` só apaga o arquivo de um envio que falhou |
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
4. **Conta.** `PortariaService.resumir` calcula: valor = R$ 2,00 × (presentes − pagos), nunca negativo. Por isso trocar pessoas não cobra nada.
5. **Teste.** `PortariaServiceTest` tem um teste para cada caso: troca, acréscimo, falta, liberação e pagamento.

## Testes

- **Testes de unidade** (`src/test/java/.../service`): regras com Mockito, sem banco.
- **Testes de controller** (`.../controller`): rotas, perfis, CSRF e mensagens, com `MockMvc`.
- **Testes no PostgreSQL** (`*PostgreSqlIntegrationTest`): só rodam num banco com "test" no nome. O `courttime_demo` é recusado.
- **Navegador real** (`qa/playwright`): a jornada dos quatro perfis, as capturas e a acessibilidade, num banco descartável no GitHub Actions.
