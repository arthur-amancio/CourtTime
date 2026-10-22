# Auditoria da finalização do TCC — 21/10/2026

Auditoria da `main` em `b709a55`, com os PRs #60 (remarcação), #61 (formulário
oficial) e #62 (guia local e CI em PostgreSQL 16 e 18) já integrados. Para cada
ponto, conferimos o código, as rotas e a execução num navegador real.

## Como foi testado

- **Suíte Java** (`./mvnw clean verify`) no GitHub Actions, com PostgreSQL 16 e
  18 temporários: 45 classes e 378 métodos de teste (cada teste parametrizado
  conta uma vez), entre eles 9 classes de integração no PostgreSQL. Há testes
  de concorrência de aprovação, cancelamento, bloqueio, grade e remarcação.
- **Navegador real** (workflow *UI — navegador real*): a aplicação Spring Boot
  sobe num PostgreSQL descartável `courttime_e2e` e o Chromium percorre:
  - a jornada dos quatro perfis: 338 verificações;
  - as sondas da finalização (`qa/playwright/auditoria-finalizacao.cjs`): 83;
  - as capturas de layout em 390, 430, 768 e 1440 px: 330;
  - a acessibilidade com axe-core: 44 varreduras.

  O workflow também confere as páginas dos PDFs gerados e que nenhum CPF foi
  gravado no banco.
- O banco `courttime_demo` do computador do Arthur **não foi acessado**. O
  computador não estava conectado a esta sessão, então a execução real no
  Windows (`.\mvnw.cmd spring-boot:run` com PostgreSQL 18) não foi repetida
  aqui. A CI confere a leitura do `application-local.properties` pelo
  verificador em PowerShell e roda a suíte também no PostgreSQL 18.

| Execução | Código | Resultado |
|---|---|---|
| [39579726505](https://github.com/arthur-amancio/CourtTime/actions/runs/39579726505) | `main` b709a55 | jornada 338/338, layout 330/330, axe 0 violações |
| [39580714917](https://github.com/arthur-amancio/CourtTime/actions/runs/39580714917) | `main` + sondas | sondas 74/83: os 9 defeitos abaixo reproduzidos |
| [39582115571](https://github.com/arthur-amancio/CourtTime/actions/runs/39582115571) | `main` + #63 + #64 + #65 | sondas 83/83, jornada 338/338, layout 330/330, axe 0 violações |
| [39582151313](https://github.com/arthur-amancio/CourtTime/actions/runs/39582151313) | mesmo código | `clean verify` aprovado no PostgreSQL 16 e 18 |

## 1. Funcionalidades concluídas

| Perfil | O que funciona | Comprovação |
|---|---|---|
| Todos | login por sessão, CSRF, saída por POST, sessão encerrada ao desativar a conta ou redefinir a senha | jornada; sondas |
| Todos | páginas 403 e 404 em português, sem detalhe técnico | sondas |
| Aluno | início, consulta da quadra com calendário, grade e bloqueios | jornada |
| Aluno | nova solicitação com antecedência de um dia, período de uma hora e finalidade | jornada; sondas |
| Aluno | minhas solicitações e detalhes, só as próprias (outra pessoa recebe 404) | jornada; sondas |
| Aluno | participantes: incluir, editar, remover, RM/RG único, mínimo total de dez pessoas | jornada; sondas |
| Aluno | formulário oficial preenchido, inclusive com 31 participantes e nome longo; CPF e curso não gravados | sondas; `FormularioReservaPdfTest` |
| Aluno | envio versionado do PDF, devolução com justificativa e nova versão | jornada |
| Aluno | aprovação, cancelamento com motivo e remarcação uma única vez, com cópia opcional de participantes | jornada; sondas |
| Secretaria | fila de conferência, download do PDF, validação e devolução justificada | jornada |
| Secretaria | contribuição presencial após a validação, ajuste com justificativa | jornada |
| Administrador | painel, fila de análise, aprovação com reconferência e concorrência, rejeição com motivo | jornada; integração PostgreSQL |
| Administrador | cancelamento institucional, horários semanais, bloqueios | jornada |
| Administrador | usuários: cadastro, edição, desativação e redefinição de senha | jornada; sondas |
| Segurança | reservas aprovadas de hoje em diante, participantes sem CPF, e-mail, valor ou PDF; cancelada some | jornada; sondas |

## 2. Defeitos reproduzidos e corrigidos

| # | Defeito | Onde | Correção |
|---|---|---|---|
| D1 | o painel contava as canceladas no total sem mostrar um indicador para elas (total 8, soma 6) | painel administrativo | PR #63 |
| D2 | rejeitadas e canceladas sumiam da área administrativa depois da decisão | análise de solicitações | PR #63 |
| D3 | o detalhe de uma rejeitada não mostrava motivo, responsável nem data; a aprovada não mostrava quem aprovou | detalhe administrativo | PR #63 |
| D4 | data ou horário com formato inválido mostrava o erro técnico em inglês "Failed to convert property value…" | bloqueio e solicitação | PR #64 |
| D5 | o texto da cancelada mandava ver o motivo "abaixo" na lista e no início, onde ele não aparece | aluno | PR #64 |
| M1 | remover participante não pedia confirmação, ao contrário das outras exclusões | participantes | PR #64 |

Cada correção tem teste de regressão (MVC, serviço ou PostgreSQL) e uma
verificação nas sondas do navegador (PR #65).

## 3. Melhorias registradas, sem urgência

- A página de erro funciona e está em português, mas ainda usa as classes
  visuais da V1 (`site-header`, `auth-panel`).
- O GitHub avisa que as actions `checkout@v4`, `setup-java@v4`,
  `setup-node@v4` e `upload-artifact@v4` do workflow de navegador usam Node 20,
  e que o `ubuntu-latest` vai migrar para o Ubuntu 26. Hoje não quebra nada.
- O inventário técnico (`docs/inventario-tecnico.md`) teve contagens e o
  atributo `remarcacaoDe` atualizados; as demais tabelas continuam fiéis à
  versão `436b262` e merecem uma revisão antes do capítulo final do TCC.

## 4. Pendências exclusivamente institucionais

> **Atualização de 21/10/2026:** a maior parte destas pendências foi respondida pela ETEC. Veja [regras-etec.md](regras-etec.md). A lista abaixo é o retrato do momento da auditoria.

Nenhuma destas foi implementada por suposição. O comportamento atual fica
preservado até a ETEC decidir.

1. **Prazo do documento assinado:** a antecedência de um dia vale para o
   pedido (como hoje) ou para a entrega do termo assinado (como diz o termo)?
2. **Limite máximo de participantes:** 30 é regra ou só a capacidade do
   formulário? Hoje não há limite; acima de 30, o PDF ganha páginas.
3. **Contribuição à APM:** os R$ 2,00 por participante e hora incluem os
   participantes "Outro"? Hoje a secretaria digita o valor recebido.
4. **Contribuição após remarcação:** a nova solicitação aproveita o valor já
   pago? Hoje não aproveita.
5. **Registro de contato com o aluno** depois de um cancelamento: não existe.
6. **Retenção e exclusão dos documentos:** não há prazo definido; nada é
   apagado.
7. **Verificação da assinatura do termo:** a conferência é visual. A
   verificação técnica (gov.br) depende de decisão.
8. **Ocorrências da portaria:** não há registro; dependem de quais dados e de
   quem lê.
9. **Vínculos da categoria Outro:** falta definir como descrever o vínculo.

## 5. Issues abertas

**#15 — alinhamento à grade e preservação de reservas aprovadas.** Todos os
critérios técnicos estão na `main`. O conteúdo do PR #17 (precisão total do
alinhamento e testes de concorrência) entrou pela integração `7c86979`, com
outros commits. Os testes `periodWithSecondOffsetIsNotTreatedAsAlignedWithGeneratedSlots`,
`postgresRejectsRequestWithSecondOffsetOutsideGeneratedGrid`,
`approvalAndConflictingBlockAreSerializedByCourtLock`,
`approvalAndGridChangingEditCannotBothPersist` e
`concurrentOverlappingWeeklySchedulesCannotBothPersist` rodam na CI.
**Proposta: encerrar.**

**#18 — participantes e fluxo documental.** A parte técnica foi entregue:
participantes, PDF versionado, conferência, contribuição, análise, segurança,
cancelamento, formulário oficial e remarcação. Vários itens da lista já foram
decididos e estão no sistema:

- o modelo oficial do termo está no projeto;
- a secretaria confere a documentação;
- o mínimo atual do sistema é dez pessoas de qualquer categoria; o termo oficial versionado ainda diverge;
- a categoria Outro não conta para o mínimo;
- a substituição acontece por devolução e nova versão.

Restam só itens institucionais (retenção, regra da APM, assinatura).
**Proposta: encerrar a #18 e acompanhar a seção 4 numa issue própria de
decisões da ETEC.**

---

## 6. Fechamento: regras da ETEC e versão da banca — 21/10/2026, noite

Depois da auditoria acima, as nove regras explicadas pelo Leandro
([regras-etec.md](regras-etec.md)) foram implementadas em PRs pequenos. Cada PR
passou pela CI com PostgreSQL 16 e 18 e pelo navegador real antes de ser
integrado.

| PR | Conteúdo |
|---|---|
| #69 | Foto aérea e fachada da escola, faixa de logotipos ETEC · CPS · Governo de SP |
| #70 | PDF assinado só até o dia anterior ao uso (R01); sem limite máximo (R02) |
| #71 | Contribuição calculada em R$ 2,00 por participante (R03) |
| #72 | Conferência da assinatura gov.br (R07) e lembrete do WhatsApp (R05) |
| #73 | Vínculo obrigatório do participante Outro, também no formulário oficial (R09) |
| #74 | Presença marcada pela portaria no dia da reserva (R08.1, R08.3, R08.4) |
| #75 | Pagamento da reserva cancelada vira crédito na remarcação (R04) |
| #76 | Simplificação: `@UsuarioAtual`, 12 controllers em vez de 14, CSS da V1 removido |
| #77 | Dados de demonstração com participantes Outro e vínculo |
| #78 | Actions da CI atualizadas (sem aviso de Node 20) |
| #79 | Teste de integração da presença no PostgreSQL |
| #80 | README, roteiro da banca e inventário técnico na versão final |
| #81 | Textos do aluno orientam a assinatura pelo gov.br |

### Verificação da versão final (`main` em `8756428`)

| Execução | Resultado |
|---|---|
| [CI 39680043729](https://github.com/arthur-amancio/CourtTime/actions/runs/39680043729) | `clean verify` aprovado no PostgreSQL 16 e 18: 46 classes e 410 métodos de teste, 8 delas de integração no PostgreSQL; dados de demonstração conferidos e repetíveis |
| [UI 39680422803](https://github.com/arthur-amancio/CourtTime/actions/runs/39680422803) | jornada 348/348, sondas 86/86, layout 330/330, axe 44 varreduras sem violação, nenhum CPF gravado |
| Capturas da versão final | 49 telas reais e o formulário oficial gerado, na branch `evidencias/capturas-final` |

### O que continua dependendo da ETEC

- **D1:** horário-limite do envio no dia anterior. Até a decisão, o envio é
  aceito durante o dia anterior inteiro.
- **D2:** destino do crédito que sobra quando a remarcação tem menos
  participantes. Até a decisão, o saldo só é mostrado.
- **D3:** quem autoriza pessoas a mais na portaria (R08.2). Até a decisão,
  elas não são registradas.
- **R09:** confirmar se irmãos podem participar e se funcionários continuam
  permitidos. O termo oficial e o relato não coincidem.
- **R06:** política de retenção e descarte dos documentos (LGPD). Até a
  decisão, nada é apagado.
- Data da banca.

### Não verificado nesta sessão

A execução no Windows do Arthur, com `.\mvnw.cmd spring-boot:run` sobre o
`courttime_demo` existente, não foi repetida porque o computador não estava
ligado à sessão. As colunas novas aceitam nulo, então o `ddl-auto=update` deve
acrescentá-las sem perder dados. Vale conferir antes da banca e rodar o
`popular-demo.sql` atualizado.
