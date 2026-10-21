# Plano técnico — implementação das regras da ETEC

Plano para levar as regras de [regras-etec.md](regras-etec.md) ao código, a
partir da `main` em `59c8163`. Cada etapa é um PR pequeno, com testes e CI no
PostgreSQL 16 e 18. O merge só acontece com autorização.

## Princípios

- **Nada se perde no `courttime_demo`.** Toda mudança de banco é **aditiva**:
  só entram colunas que aceitam nulo e tabelas novas, criadas pelo
  `ddl-auto=update`. Nenhuma coluna é renomeada ou apagada, e nenhum dado
  antigo é reescrito. Os registros antigos ganham uma regra de leitura
  explícita, como "vínculo não informado".
- **Nada é inventado.** O que depende de D1, D2 ou D3 fica com o comportamento
  provisório descrito em `regras-etec.md`, marcado no código com o código da
  decisão.
- **Mesma arquitetura.** O fluxo continua Controller → Service → Repository. Os
  formulários continuam em Thymeleaf. Nenhuma biblioteca nova entra.
- **Regressão sempre testada.** Antes de cada merge rodam a suíte Java, o
  workflow *UI — navegador real* (jornada dos quatro perfis e as sondas de
  `auditoria-finalizacao.cjs`) e uma sonda nova para a regra.

## Etapas

| # | PR | Regras | Tamanho | Depende de |
|---|---|---|---|---|
| 1 | `docs/regras-etec` (este) | todas | — | — |
| 2 | `feat/prazo-documental` | R01, R02 | pequeno | — |
| 3 | `feat/contribuicao-calculada` | R03 | pequeno | — |
| 4 | `feat/vinculo-outro` | R09 | médio | — |
| 5 | `feat/conferencia-govbr` | R07, R05 | pequeno | — |
| 6 | `feat/presenca-portaria` | R08.1, R08.3, R08.4 | médio | — |
| 7 | `feat/credito-remarcacao` | R04 | médio | 3 |
| 8 | `feat/pessoas-adicionais` | R08.2 | pequeno | D3 |
| 9 | `refactor/simplificacao-pontual` | — | pequeno, em partes | 2–7 |
| 10 | homologação | todas | — | 2–9 |

A ordem segue a combinada. A etapa 4 (R09) entrou porque o vínculo também
precisa aparecer no formulário oficial e na portaria, antes da etapa 6.

### 2. Prazo documental (R01, R02)

- `DocumentoReservaService.enviar`: recusa o envio quando a data atual em
  `America/Sao_Paulo` já é a data da utilização ou posterior. Mensagem:
  "O documento assinado deve ser entregue até o dia anterior ao uso." O ponto
  fica marcado com `D1` para receber o horário-limite quando ele for decidido.
- Tela de documentos: mostra "Envie o PDF assinado até dd/mm/aaaa", e, no dia
  da utilização, o aviso de que o envio não é mais possível.
- R02: retirar os textos que dizem que o limite máximo ainda depende de
  confirmação.
- **Testes:**
  - unitário: envio no dia da utilização, no dia anterior e depois da data;
  - MVC: a mensagem aparece na tela;
  - sonda no navegador: envio no dia recusado.

### 3. Contribuição calculada (R03)

- `ContribuicaoApmService`: valor devido = R$ 2,00 × participantes da versão
  conferida. Como a lista está travada depois do envio, é a contagem atual.
  Ganha a coluna nova `valor_devido`, que aceita nulo nos registros antigos.
- Tela da secretaria: mostra a conta ("11 participantes × R$ 2,00 = R$ 22,00")
  e um botão **Registrar recebimento**. O campo de valor livre deixa de
  existir; o ajuste com justificativa continua para correções.
- Retirar o aviso "referência institucional pendente".
- **Testes:**
  - unitário: 10 alunos mais 1 Outro dá R$ 22,00;
  - integração PostgreSQL: valor devido e recebido persistidos;
  - sonda no navegador.

### 4. Vínculo do participante Outro (R09)

- Novo enum `VinculoParticipante`: `EX_ALUNO`, `PAI_OU_MAE`, `IRMAO_OU_IRMA`,
  `PROFESSOR`. Os rótulos da tela seguem o relato.
- `ParticipanteReserva.vinculo`, coluna que aceita nulo. É obrigatória quando a
  categoria é Outro e proibida quando é Aluno. A regra fica no serviço, não só
  no formulário.
- Lista, conferência, análise e portaria mostram o vínculo. Registro antigo
  sem vínculo aparece como "vínculo não informado".
- `FormularioReservaPdf`: escreve o vínculo no espaço "( ) Outro: ____".
- A cópia de participantes na remarcação copia também o vínculo.
- **Testes:**
  - unitário e MVC: vínculo obrigatório e vínculo recusado para Aluno;
  - PDF: comparação da região, como os testes atuais do formulário;
  - jornada e sondas atualizadas para informar o vínculo.

### 5. Conferência da assinatura gov.br (R07, R05)

- `ConferenciaDocumento.assinaturaGovBrConferida`, coluna que aceita nulo nas
  conferências antigas. Validar exige a caixa "Conferi a assinatura gov.br no
  verificador oficial do ITI". Devolver não exige.
- Tela de conferência: link e instruções curtas para o `validar.iti.gov.br`.
  Tela do aluno: "assine pelo gov.br".
- Histórico de conferências: mostra "assinatura gov.br conferida pela
  secretaria". O texto nunca diz que o sistema validou.
- R05: o painel de cancelamento ganha o lembrete "Depois de registrar, avise o
  aluno pelo WhatsApp". Não há integração.
- **Testes:** validar sem a caixa marcada é recusado no servidor; o histórico
  mostra a confirmação.

### 6. Presença na portaria (R08.1, R08.3, R08.4)

- `ParticipanteReserva` ganha as colunas `presenca` (`PRESENTE`, `AUSENTE` ou
  nulo), `presencaRegistradaPor` e `presencaRegistradaEm`, todas aceitando
  nulo.
- Rota nova `POST /seguranca/reservas/{id}/presencas`, exclusiva do perfil
  Segurança, com CSRF. Só aceita reserva `APROVADA` e, pela proposta técnica,
  só no dia da reserva.
- A segurança continua sem acesso a pagamentos, documentos e decisões. Os
  testes de acesso negado atuais são mantidos e ampliados.
- O detalhe administrativo mostra a presença registrada.
- **Testes:**
  - MVC: perfis errados recebem 403, e a rota exige CSRF;
  - PostgreSQL: duas marcações simultâneas;
  - sonda: a segurança marca presença e a administração vê.

### 7. Crédito na remarcação (R04)

- Sem tabela nova. Na remarcação, o crédito é o valor efetivamente pago na
  reserva de origem: valor recebido depois dos ajustes, mais o crédito que ela
  já tenha aproveitado.
- `ContribuicaoApm` ganha as colunas `creditoAplicado` e `contribuicaoOrigem`,
  que aceitam nulo. A cobrança registrada é o valor devido menos o crédito,
  nunca negativa.
- Tela da secretaria na remarcação: "Crédito da reserva cancelada nº X:
  R$ 22,00 · devido: R$ 30,00 · **cobrar: R$ 8,00**". Se a cobrança for zero,
  o botão vira **Aplicar crédito**.
- Excedente (D2): fica gravado e aparece como "saldo excedente: pendente de
  decisão da ETEC", sem nenhum destino.
- A aprovação continua exigindo o documento validado e a contribuição
  registrada (R04.5).
- **Testes:**
  - unitário: crédito maior, igual e menor que o devido;
  - PostgreSQL: o crédito não é aplicado duas vezes, mesmo com duas
    secretarias ao mesmo tempo;
  - sonda: cancelar, remarcar e pagar só a diferença.

### 8. Pessoas adicionais (R08.2)

Só depois da decisão D3. A proposta é uma tabela nova
`pessoas_adicionais_reserva`, com nome, RM ou RG, vínculo, quem registrou e
quando, descrita como **registro**, não como autorização.

### 9. Simplificação pontual

Com uma lista curta, cada item em PR próprio e sem mudar o comportamento:

- `ReservaService`: separar a remarcação, que hoje é a parte mais longa;
- mensagens repetidas nos controllers (`obterUsuarioAutenticado` está copiado
  em vários deles);
- CSS da V1 que ainda sobra (página de erro).

Se faltar tempo antes da banca, esta é a primeira etapa a ser cortada.

### 10. Homologação

- CI e navegador real na versão final.
- No Windows, no computador do Arthur, conectado à sessão:
  `.\mvnw.cmd spring-boot:run` com PostgreSQL 18 sobre o `courttime_demo`
  existente. O teste confirma que as colunas novas entram sem perder dados.
- Capturas reais da versão final e atualização da documentação e da
  auditoria.

## Impacto no banco `courttime_demo`

| Etapa | Mudança | Destrutiva? |
|---|---|---|
| 3 | `contribuicoes_apm.valor_devido` | não, coluna que aceita nulo |
| 4 | `participantes_reserva.vinculo` | não, coluna que aceita nulo |
| 5 | `conferencias_documento.assinatura_gov_br_conferida` | não, coluna que aceita nulo |
| 6 | três colunas de presença em `participantes_reserva` | não, colunas que aceitam nulo |
| 7 | `contribuicoes_apm.credito_aplicado`, `contribuicao_origem_id` | não, colunas que aceitam nulo |
| 8 | tabela `pessoas_adicionais_reserva` | não, tabela nova |

Antes da homologação, fazer um backup do `courttime_demo` pelo pgAdmin
(Backup…) é recomendável, mas não obrigatório.
