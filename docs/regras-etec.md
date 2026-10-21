# CourtTime — regras institucionais da ETEC

**Fonte:** relato de Arthur Amancio Francisco sobre a conversa com Leandro,
responsável pelo procedimento da quadra na ETEC Prof. Armando José Farinazzo,
registrado em 21/10/2026. Os textos abaixo resumem o que foi explicado. **Não
são transcrições literais** e não devem ser citados como palavras do Leandro.

Cada regra tem um código (R01 a R09), que vai aparecer nos commits, nos PRs e
nos testes. Para cada regra há três coisas:

- **Relato:** o que foi decidido.
- **Requisitos:** o que o sistema deve fazer, escrito de forma que um teste
  consiga verificar.
- **Hoje:** o que o CourtTime da `main` faz, para mostrar a diferença.

O plano de implementação está em
[plano-regras-etec.md](plano-regras-etec.md).

Requisitos que **continuam valendo** sem mudança:

- mínimo de dez participantes na categoria Aluno;
- os quatro perfis e o controle de acesso por perfil e por dono da solicitação;
- calendário, horários semanais e bloqueios;
- prevenção de reservas conflitantes;
- formulário oficial preenchido;
- histórico de todas as decisões;
- CSRF, proteção dos documentos e controle transacional.

---

## R01 — Antecedência obrigatória

**Relato.** O aluno deve solicitar a quadra, cadastrar todos os participantes e
entregar o documento assinado **até o dia anterior** ao uso. No dia do uso, o
administrador pode aprovar, desde que tudo esteja correto e antes do início da
utilização. Não basta criar a solicitação um dia antes: a documentação também
precisa estar pronta.

**Requisitos.**

- R01.1 — A solicitação continua sendo criada só para datas a partir de amanhã.
- R01.2 — O envio do PDF assinado é recusado a partir do dia da utilização. A
  mensagem deve dizer o motivo.
- R01.3 — Como a lista de participantes trava depois do envio, ela também fica
  pronta até o dia anterior.
- R01.4 — A aprovação continua permitida no próprio dia, até o horário de
  início.

**Hoje.** O sistema faz R01.1 a R01.4. O envio é recusado a partir do dia da
utilização, e a tela de documentos mostra a data-limite.

**Em aberto.** O **horário-limite** no dia anterior. "Até o dia anterior" já
garante que o envio no próprio dia é recusado. Um horário mais cedo, como 17h,
ainda depende de confirmação. Veja a decisão D1.

**Atenção.** Se a secretaria devolver o documento no próprio dia da
utilização, o aluno não consegue mais reenviar. É consequência direta da regra,
e o aviso vai aparecer na tela.

## R02 — Quantidade de participantes

**Relato.** Pode haver mais de 30 participantes, embora normalmente haja menos.

**Requisitos.**

- R02.1 — Não existe limite máximo de participantes.
- R02.2 — Com mais de 30 participantes, o formulário oficial ganha páginas de
  participantes adicionais.

**Hoje.** Já está assim. Foi conferido com 31 participantes: o PDF sai com 3
páginas. Os textos sobre um limite "a confirmar" foram retirados.

## R03 — Contribuição à APM

**Relato.** A contribuição é de R$ 2,00 por participante, de qualquer
categoria. O cálculo usa a quantidade de participantes do documento conferido:
11 participantes dão R$ 22,00. O pagamento continua presencial, registrado
pela secretaria, sem integração bancária nem pagamento on-line.

**Requisitos.**

- R03.1 — O sistema calcula o **valor devido** como R$ 2,00 × o total de
  participantes da versão conferida, contando Aluno e Outro.
- R03.2 — A secretaria registra o recebimento desse valor. Ela não digita mais
  o valor livremente.
- R03.3 — O valor devido e o valor recebido ficam no histórico, com quem
  registrou e quando.
- R03.4 — O ajuste posterior com justificativa continua existindo, para
  corrigir um registro.
- R03.5 — Não há Pix, cartão, boleto nem pagamento pela internet.

**Hoje.** A secretaria digita o valor e a tela avisa que a referência de R$ 2,00
depende de confirmação. O termo oficial fala em R$ 2,00 "por participante a
cada hora". Como todo período do CourtTime tem uma hora, as duas formas dão o
mesmo valor.

## R04 — Cancelamento e reaproveitamento do pagamento

**Relato.** Quando a escola cancela uma reserva já paga, o dinheiro não se
perde e normalmente não é devolvido: vira crédito para uma remarcação. Na
remarcação, o valor pago é aproveitado. Se a nova contribuição for maior, o
aluno paga só a diferença: com R$ 22,00 de crédito e R$ 30,00 devidos, paga
R$ 8,00. A nova reserva continua precisando de documentação e de aprovação; o
crédito não autoriza nada sozinho.

**Requisitos.**

- R04.1 — O valor efetivamente pago numa reserva cancelada pela instituição
  fica disponível como crédito para a remarcação dela. "Efetivamente pago" é o
  valor depois dos ajustes, somado a algum crédito que essa reserva já tenha
  aproveitado.
- R04.2 — Na remarcação, a secretaria vê o crédito, o valor devido e a
  diferença a cobrar. A diferença é o valor devido menos o crédito, e nunca
  fica negativa.
- R04.3 — Quando o crédito cobre todo o valor devido, nenhuma cobrança nova é
  registrada. Fica registrado só o uso do crédito.
- R04.4 — Um crédito é usado uma única vez. Isso já é garantido, porque cada
  cancelada só pode ser remarcada uma vez.
- R04.5 — O crédito não pula etapas: a remarcação continua precisando do PDF
  assinado, da conferência e da aprovação.

**Hoje.** A remarcação existe, mas não aproveita a contribuição.

**Em aberto.** O que fazer com o **saldo excedente**, quando a remarcação tem
menos participantes que a original. Até a decisão D2, o sistema só registra o
excedente e não dá a ele nenhum destino.

## R05 — Comunicação de cancelamento

**Relato.** O administrador registra o cancelamento e o motivo no CourtTime e
avisa o aluno manualmente pelo WhatsApp. Não há integração com o WhatsApp.

**Requisitos.**

- R05.1 — O cancelamento continua exigindo um motivo, que o aluno vê no
  sistema.
- R05.2 — A tela de cancelamento lembra o administrador de avisar o aluno pelo
  WhatsApp.
- R05.3 — Nenhuma mensagem é enviada automaticamente, e nada é integrado ao
  WhatsApp.

**Hoje.** O sistema já faz R05.1 e R05.3. Falta só o lembrete do R05.2.

## R06 — Armazenamento dos documentos

**Relato.** Leandro guarda os documentos antigos até hoje. O CourtTime deve
mantê-los arquivados para consulta autorizada, sem exclusão automática nesta
etapa.

**Requisitos.**

- R06.1 — Nenhum PDF enviado é apagado automaticamente.
- R06.2 — A consulta continua restrita: o aluno vê só os próprios documentos, a
  secretaria e a administração veem os demais, e a segurança não vê nenhum.

**Hoje.** Já está assim.

**Em aberto, institucional.** A política definitiva de retenção, acesso e
descarte, considerando a LGPD. Não é uma decisão técnica.

## R07 — Assinatura pelo gov.br

**Relato.** O documento deve ser assinado pelo gov.br. A secretaria confere a
assinatura. O CourtTime orienta essa verificação e registra a conferência
humana. Não há validador criptográfico próprio, e o sistema não afirma que
verifica a autenticidade sozinho.

**Requisitos.**

- R07.1 — A tela do aluno orienta a assinatura pelo gov.br.
- R07.2 — A tela de conferência orienta a secretaria a verificar a assinatura
  no verificador oficial do ITI (`validar.iti.gov.br`).
- R07.3 — Para validar um documento, a secretaria precisa confirmar que
  conferiu a assinatura gov.br. Essa confirmação fica registrada, com quem
  conferiu e quando.
- R07.4 — Os textos dizem sempre "conferido pela secretaria", nunca "validado
  pelo sistema".

**Hoje.** O sistema já registra quem conferiu e quando, e já avisa que não
valida a assinatura. Faltam a orientação específica do gov.br e a confirmação
obrigatória.

## R08 — Controle de presença na portaria

**Relato.** A portaria consulta as reservas autorizadas e registra quem
compareceu, quem não compareceu e quem apareceu a mais, sem estar no documento.
A segurança não altera pagamentos, documentos, horários nem decisões.

**Requisitos.**

- R08.1 — Numa reserva aprovada, a segurança marca cada participante como
  presente ou ausente. Cada marcação registra quem marcou e quando.
- R08.2 — A segurança registra as pessoas adicionais, com nome e RM ou RG,
  como **registro de comparecimento**. O registro não autoriza a pessoa
  sozinho.
- R08.3 — A segurança continua sem acesso a pagamentos, documentos, horários e
  decisões. O que ela altera é só a presença.
- R08.4 — A administração consulta a presença registrada.

**Hoje.** A segurança só consulta.

**Em aberto.** Quem autoriza a entrada de uma pessoa extra e como essa
autorização é registrada (decisão D3). Também falta confirmar em que período a
presença pode ser marcada. A proposta técnica é: só no dia da reserva.

## R09 — Participantes da categoria "Outro"

**Relato.** Na categoria Outro entram ex-alunos da ETEC, familiares próximos
dos alunos (pai, mãe e irmãos) e professores da instituição. Amigos ou
convidados sem esses vínculos não podem participar. O vínculo de cada um
precisa ficar claro.

**Requisitos.**

- R09.1 — Ao cadastrar alguém como Outro, o aluno escolhe obrigatoriamente o
  vínculo: ex-aluno da ETEC, pai ou mãe de aluno, irmão ou irmã de aluno, ou
  professor da ETEC.
- R09.2 — Não existe opção "amigo" nem campo livre: quem não tem um desses
  vínculos não pode ser cadastrado.
- R09.3 — O vínculo aparece na lista de participantes, na conferência da
  secretaria, na análise da administração, na portaria e no campo
  "( ) Outro: ____" do formulário oficial.
- R09.4 — Participantes da categoria Outro cadastrados antes desta regra
  aparecem como "vínculo não informado". A secretaria vê o aviso antes de
  validar.

**Hoje.** A categoria Outro existe, mas não guarda o vínculo.

**A confirmar.** O relato e o termo oficial não coincidem. O termo admite
pais, filhos e cônjuges de alunos, egressos, professores e seus filhos e
cônjuges, e funcionários e seus filhos e cônjuges. O termo **não cita irmãos**,
e o relato não cita filhos, cônjuges nem funcionários. A lista do relato vale
até alguém da ETEC dizer o contrário. Vale confirmar com o Leandro se irmãos
podem mesmo participar e se funcionários continuam permitidos.

---

## Decisões ainda abertas

| Código | Pergunta | Até lá, o sistema… |
|---|---|---|
| D1 | Qual é o horário-limite do envio no dia anterior? | recusa o envio a partir do dia da utilização |
| D2 | O que fazer com o crédito excedente quando a remarcação tem menos participantes? | registra o excedente, sem destino |
| D3 | Quem autoriza uma pessoa extra na portaria, e como isso é registrado? | registra o comparecimento, sem autorizar |
| — | Qual é a política de retenção, acesso e descarte dos documentos (LGPD)? | guarda tudo, com acesso restrito |
| — | Em que período a portaria pode marcar presença? | proposta: só no dia da reserva |
| — | Qual é a data da banca? | define a prioridade das etapas |
