# Roteiro de apresentação do CourtTime à banca

Guia para demonstrar o sistema ao vivo. Versão validada: `main` em `83ef3e9`.

Leia junto com `docs/guia-do-courttime.md`, que explica o sistema e traz a
instalação completa no Windows.

## Antes da apresentação

### 1. Banco de demonstração isolado

Nunca use um banco com dados existentes. No `psql`, conectado ao `postgres`:

```sql
DROP DATABASE IF EXISTS courttime_demo;
CREATE DATABASE courttime_demo;
```

O `DROP` no início é proposital: é assim que se reinicia a demonstração do zero.

### 2. Subir a aplicação

Siga `docs/guia-do-courttime.md`. Em resumo: variáveis de ambiente apontando
para `courttime_demo`, primeira execução com o bootstrap do administrador,
depois pare, remova as variáveis do bootstrap e suba de novo.

### 3. Contas fictícias

Criadas em **Administração → Usuários**, exceto o administrador, que vem do
bootstrap. Use sempre domínio de exemplo, nunca email real de ninguém.

| Perfil | Email | Nome exibido |
|---|---|---|
| Administrador | `admin.demo@exemplo.local` | Administração Demo |
| Aluno | `aluno.demo@exemplo.local` | Aluno Demonstração |
| Secretaria | `secretaria.demo@exemplo.local` | Secretaria Demo |
| Segurança | `seguranca.demo@exemplo.local` | Portaria Demo |

Senhas de 8 a 72 caracteres, digitadas na hora. **Não anote senhas no TCC nem
em slides.**

### 4. Grade de horários

Em **Administração → Horários**, cadastre pelo menos um intervalo no dia da
semana correspondente à data que você vai usar na demonstração. Sem grade, não
aparece nenhum período disponível e a demonstração trava logo no início.

Sugestão: segunda a sexta, das 08:00 às 12:00.

### 5. Dados fictícios da solicitação

- **Data:** no mínimo o dia seguinte. A regra de antecedência recusa hoje.
- **Horário:** um período de uma hora dentro da grade.
- **Finalidade:** "Treino da turma do 3º INFONET".
- **Participantes:** dez, categoria Aluno, com nomes claramente fictícios
  (`Participante Fictício 1` a `10`) e RM fictício (`RM-1001` em diante).
- **PDF:** um arquivo sintético qualquer. **Não apresente como termo assinado.**

## Roteiro, na ordem

Prepare quatro janelas anônimas do navegador, uma por perfil, para não precisar
sair e entrar o tempo todo.

### Ato 1 — Aluno (3 a 4 minutos)

1. **Login** → cai no **Início**. Mostre o cartão "Sua solicitação mais
   recente" (vazio na primeira vez).
2. **Consultar horários** → escolha a data. Mostre os períodos disponíveis.
3. **Nova solicitação** → destaque que a primeira data possível é amanhã, e que
   isso vem do termo da ETEC.
4. Preencha e envie. Mostre que a situação nasce **Pendente de documentação** e
   que **nada foi autorizado ainda**.
5. **Participantes** → cadastre os dez. Mostre o contador de alunos.
6. **Documentos** → envie o PDF sintético. Diga, com estas palavras: *"o termo é
   preparado e assinado fora do sistema; o CourtTime guarda a versão enviada"*.
7. Volte ao **Início**: agora o cartão mostra **Documento aguardando
   conferência** e o próximo passo.

### Ato 2 — Secretaria (2 a 3 minutos)

1. **Login** → **Conferir documentos**.
2. Abra o documento pendente, **baixe o PDF** e diga que a conferência é
   **manual**: o sistema não valida assinatura criptograficamente.
3. **Validar documento**.
4. **Contribuições presenciais** → registre o valor fictício. Mostre o aviso de
   que a referência de R$ 2,00 **depende de confirmação da ETEC** e que o
   sistema **não calcula** o valor.
5. Aponte que a solicitação passou para **Pronta para análise administrativa**,
   e que **a secretaria não aprova reservas**.

### Ato 3 — Administração (3 minutos)

1. **Login** → **Painel** → **Solicitações**.
2. Abra a solicitação. Mostre solicitante, data, horário, participantes,
   situação documental e contribuição, tudo numa tela só.
3. **Aprovar**. Explique que, na aprovação, o sistema bloqueia o registro da
   quadra na transação e **reconfere** grade, bloqueios e reservas já aprovadas.
4. Volte ao formulário do aluno na mesma data: **o período aprovado não aparece
   mais** como disponível.

### Ato 4 — Segurança (2 minutos)

1. **Login** → **Reservas autorizadas**.
2. Abra a reserva. Mostre nome, RM/RG e categoria dos participantes — o que a
   portaria precisa para conferir a entrada.
3. Destaque o que **não** aparece: nenhum valor financeiro, nenhum PDF, nenhum
   email. É minimização de dados, não esquecimento.
4. Mostre no celular: as tabelas viram cartões e nada sai da tela.

### Ato 5 — Cancelamento institucional (2 minutos)

1. Volte à **Administração** → **Cancelar autorização**, com motivo obrigatório.
2. Na **Segurança**: a reserva **não é mais encontrada**.
3. No **Aluno**: aparece o motivo do cancelamento.
4. Diga que documento, contribuição, participantes e o registro de quem aprovou
   **continuam preservados** — cancelar não apaga histórico.

## Se algo der errado — plano de contingência

| Problema | O que fazer |
|---|---|
| Nenhum período aparece | Falta grade de horários. Cadastre em Administração → Horários, no dia da semana certo |
| "A solicitação deve ser feita com pelo menos um dia de antecedência" | Você escolheu hoje. Use amanhã |
| Aplicação não sobe com erro de administrador | As variáveis do bootstrap ficaram definidas. Remova-as e suba de novo |
| Não consegue validar o documento | A lista de participantes mudou depois do envio, ou há menos de dez alunos |
| Banco em estado confuso | `DROP DATABASE courttime_demo;` e recomece do zero |
| **A demonstração ao vivo falhar** | Mostre a execução automatizada: o teste `JornadaCompletaE2ETest` percorre exatamente este roteiro no GitHub Actions, com PostgreSQL isolado. É a mesma jornada, executada pela máquina |

Tenha a aba do GitHub Actions aberta antes de começar. É a sua rede de proteção.

## O que está implementado e comprovado

Percorrido de ponta a ponta por teste automatizado, com servidor real, login
real, token CSRF real e upload real:

- login e sessão por perfil, com encerramento da sessão quando a conta é desativada;
- consulta de disponibilidade com grade semanal e bloqueios;
- solicitação com antecedência mínima de um dia;
- cadastro de participantes e exigência de dez alunos na conferência;
- envio versionado de PDF, guardado fora da área pública;
- conferência documental da secretaria, com validação ou devolução justificada;
- registro de contribuição presencial, vinculado à conferência exata;
- aprovação administrativa com bloqueio transacional e reconferência;
- consulta da segurança, restrita ao necessário;
- cancelamento institucional com motivo, preservando todo o histórico;
- autorização por perfil, proteção CSRF e bloqueio de acesso a dados de outro aluno.

## Limitações reais — diga à banca antes que perguntem

- O sistema **não gera** o formulário oficial da ETEC preenchido. O termo é
  preparado fora do sistema.
- O sistema **não verifica criptograficamente** a assinatura do PDF. A
  conferência da secretaria é **manual e humana**.
- **Não há reagendamento** após cancelamento, nem reaproveitamento da contribuição.
- **Não há registro** de contato com o aluno.
- **Não há registro de ocorrências** pela segurança.
- **Não há** regra de retenção dos documentos enviados.
- O sistema **não calcula** o valor da contribuição.
- Decisões ainda pendentes da ETEC: limite de participantes, prazo para a
  decisão administrativa, coleta de CPF e curso, e o vínculo da categoria "Outro".

Apresentar essas lacunas como decisões conscientes é mais forte do que ser
pego por elas numa pergunta.
