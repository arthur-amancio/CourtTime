# Dados, estados e indicadores do protótipo

Conferido no código da `main` (`436b262`). O objetivo é separar o que o protótipo mostra com dados que **já existem** do que **depende de adaptação** do backend.

## 1. Estados gravados × situação apresentada × etapas

| Camada | Onde está | Valores | Gravado no banco? |
| --- | --- | --- | --- |
| Status da reserva | `StatusReserva` | `PENDENTE_DOCUMENTACAO`, `PENDENTE_ANALISE`, `APROVADA`, `REJEITADA`, `CANCELADA` | **Sim** |
| Situação apresentada | `SituacaoSolicitacaoService` | título + explicação calculados a partir do status, do documento mais recente e da contribuição | Não (calculada) |
| Etapas do processo | Somente no protótipo | Solicitação registrada → Participantes e PDF → Conferência da secretaria → Contribuição presencial → Decisão administrativa | **Não.** É uma orientação visual |

Enquanto o status é `PENDENTE_DOCUMENTACAO`, a situação apresentada pode ser:

| Variação no protótipo (`?estado=`) | Título no serviço | Etapa destacada |
| --- | --- | --- |
| `documentacao` | Pendente de documentação | Participantes e PDF |
| `conferencia` | Documento aguardando conferência | Conferência da secretaria |
| `devolvida` | Documento devolvido para correção | Participantes e PDF (correção), com a conferência marcada como devolvida |
| `contribuicao` | Contribuição pendente | Contribuição presencial |

As demais são `analise` (Pronta para análise administrativa, status `PENDENTE_ANALISE`), `aprovada`, `rejeitada` e `cancelada` (“Cancelada pela administração”). O serviço também tem a situação “Contribuição precisa de revisão”, usada quando a contribuição não corresponde ao documento mais recente. Ela não foi demonstrada no protótipo.

Regras respeitadas no protótipo:
- a devolução documental **não** muda o status. A solicitação continua `PENDENTE_DOCUMENTACAO`;
- o registro da contribuição é o que muda o status para `PENDENTE_ANALISE`;
- só a aprovação administrativa gera `APROVADA`. Nenhuma tela sugere aprovação automática;
- “Cancelada” significa que a reserva já tinha sido aprovada e foi cancelada com motivo institucional;
- a conferência do PDF é manual. Nada indica validação automática da assinatura.

## 2. Histórico da solicitação

O painel “Histórico” do detalhe do aluno é uma **proposta**. As datas existem no banco, mas a maioria não é enviada à tela do aluno.

| Evento | Campo com data | Hoje o aluno vê? |
| --- | --- | --- |
| Solicitação registrada | `Reserva.criadaEm` | Não |
| PDF enviado (cada versão) | `DocumentoReserva.enviadoEm` | **Sim**, na página de documentos |
| Documento validado ou devolvido | `ConferenciaDocumento.conferidoEm` | Não (o aluno vê o resultado e a justificativa, sem a data) |
| Contribuição registrada | `ContribuicaoApm.registradaEm` | Não |
| Aprovada ou rejeitada | `Reserva.analisadaEm` | Não (o motivo da rejeição aparece) |
| Cancelada | `Reserva.canceladaEm` | Não (o motivo do cancelamento aparece) |

O que **não** existe e não deve ser inventado: edições de participantes com data, histórico de quem visualizou, ajustes de contribuição visíveis ao aluno, contato com o aluno e reagendamento. O protótipo mostra quem agiu apenas pelo papel (“secretaria”, “administração”), sem nomes de servidores.

Para migrar o histórico, é preciso decidir: (a) se o aluno deve ver essas datas; (b) se o controller do detalhe passa a enviar documento, conferência e contribuição. Sem essa decisão, o detalhe real continua sem o painel.

## 3. Indicadores

### Painel administrativo (`PainelAdministrativo`)

| Indicador | Situação | Origem |
| --- | --- | --- |
| Total de solicitações | **Disponível atualmente** | `totalSolicitacoes` |
| Pendentes de documentação | **Disponível atualmente** | `pendentesDocumentacao` |
| Pendentes de análise | **Disponível atualmente** | `pendentesAnalise` |
| Aprovadas | **Disponível atualmente** | `aprovadas` |
| Rejeitadas | **Disponível atualmente** | `rejeitadas` |
| Horários semanais | **Disponível atualmente** | `horarios.size()` |
| Bloqueios futuros | **Disponível atualmente** | `bloqueiosFuturos.size()` |
| Canceladas | **Dependente de adaptação** | Exige nova contagem por `StatusReserva.CANCELADA` no serviço e um campo novo no `record` |

### Início por perfil (`InicioController` envia só `nome`, `perfil` e `ultimaSolicitacao`)

| Indicador no protótipo | Situação |
| --- | --- |
| Aluno — solicitação mais recente | **Disponível atualmente** |
| Administrador — prontas para análise, bloqueios futuros, contas ativas | **Dependente de adaptação** (os dados existem em outras telas, mas não no início) |
| Secretaria — documentos a conferir, contribuições a registrar | **Dependente de adaptação** |
| Segurança — reservas autorizadas para hoje | **Dependente de adaptação** (os dados existem em `/seguranca/reservas`) |
| Contadores da barra lateral (ex.: “2”) | **Dependente de adaptação** |

No protótipo, esses itens têm a marca tracejada **“Depende de adaptação”**. A marca serve à revisão e não deve ir para a interface final. Nenhuma métrica financeira, de ocupação ou de receita foi criada.

## 4. Elementos exclusivos do protótipo (remover na migração)

- Faixa amarela “Protótipo visual…”.
- Atalhos de perfil no login (“Somente no protótipo”).
- Controles de troca de situação, de variante do calendário e de quadra inativa.
- Marcas “Depende de adaptação”.
- Toda a lógica de `prototipo.js` que simula envio, criação de reserva, validação de PDF e dados fictícios.
