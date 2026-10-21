# Roteiro de apresentação do CourtTime à banca

Guia para demonstrar o sistema ao vivo. Use a `main` mais recente e anote o
commit com `git log -1 --oneline` antes de começar.

Leia junto com `docs/guia-do-courttime.md`, que explica o sistema e traz a
instalação completa no Windows.

## Antes da apresentação

### 0. Conferir o ambiente

Rode primeiro, de preferência um dia antes:

```powershell
.\scripts\Verificar-Ambiente-CourtTime.ps1
```

Ele não altera nada. Confere Java 17, `JAVA_HOME`, Maven Wrapper, `psql`, as
variáveis de conexão, e avisa sobre três variáveis perigosas esquecidas
ligadas: `COURTTIME_POSTGRES_TEST`, que habilita os testes de integração;
`SPRING_JPA_HIBERNATE_DDL_AUTO` com `create` ou `create-drop`, que faz a própria
aplicação apagar as tabelas ao iniciar; e `COURTTIME_BOOTSTRAP_ADMIN_ENABLED`,
que impede a aplicação de iniciar quando já existe administrador.

### 1. Banco de demonstração isolado

O caminho mais simples é o do [desenvolvimento local rápido](desenvolvimento-local-rapido.md):
banco `courttime_demo`, senha em `application-local.properties` e
`scripts/popular-demo.sql` para criar as contas e os horários fictícios. Os
dados continuam no banco de um dia para o outro; não é preciso recomeçar.

Nunca use um banco com dados de outra pessoa. **Só se quiser zerar o banco de
demonstração de propósito**, use o script, que confere o banco antes de apagar
qualquer coisa:

```powershell
.\scripts\Reiniciar-Demonstracao.ps1
```

Ele recusa qualquer nome que não termine em `_demo`, recusa `postgres`,
`courttime` e `courttime_test`, mostra quantas tabelas e contas existem no
banco, recusa apagar com a aplicação ainda no ar, e **exige que você digite o
nome do banco** antes de apagar. Se o PostgreSQL recusar qualquer passo, o
script para e diz o motivo, em vez de anunciar sucesso.

O script **não apaga os PDFs** já enviados: eles ficam na pasta definida em
`COURTTIME_DOCUMENT_STORAGE_PATH` (padrão `./data/documentos`). Sem o banco,
esses arquivos ficam órfãos e não aparecem no sistema. Para recomeçar de fato
do zero, apague também o conteúdo dessa pasta **apenas se ela for exclusiva da
demonstração**.

**Equivalente manual**, se preferir fazer à mão. Confirme primeiro onde você
está conectado:

```sql
SELECT current_database();
```

Só continue se a resposta for **exatamente** `courttime_demo`. Depois, do banco
`postgres`:

```sql
DROP DATABASE IF EXISTS courttime_demo;
CREATE DATABASE courttime_demo;
```

Nunca execute o `DROP` sem antes confirmar o nome. É o comando mais perigoso
deste guia.

### 2. Subir a aplicação

Na raiz do projeto: `.\mvnw.cmd spring-boot:run` e abra `http://localhost:8080/`.
Com o `application-local.properties` pronto, nenhuma variável de ambiente é
necessária. (A alternativa só por variáveis, com o bootstrap do administrador,
está em `docs/guia-do-courttime.md`.)

### 3. Contas fictícias

Criadas pelo `scripts/popular-demo.sql`, com a senha fictícia que você digitou
no Query Tool do pgAdmin. Use sempre domínio de exemplo, nunca email real de
ninguém.

| Perfil | Email |
|---|---|
| Administrador | `admin.demo@exemplo.invalid` |
| Aluno | `aluno01.demo@exemplo.invalid` (há 12 contas de aluno) |
| Secretaria | `secretaria1.demo@exemplo.invalid` |
| Segurança | `seguranca1.demo@exemplo.invalid` |

**Não anote senhas no TCC nem em slides.**

### 4. Grade de horários

O `popular-demo.sql` cria faixas fictícias de segunda a quinta. Escolha uma
data num desses dias. Sem grade, não aparece nenhum período disponível e a
demonstração trava logo no início; nesse caso cadastre um intervalo em
**Administração → Horários**.

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
6. **Documentos** → **Gerar formulário preenchido** com um CPF fictício válido
   (ex.: `123.456.789-09`) e o curso. Abra o PDF: é o formulário original da ETEC
   com data, horário e participantes; assinaturas e campos da direção em branco.
   Diga que CPF e curso **não ficam gravados**.
7. Ainda em **Documentos**, envie um PDF sintético. Diga, com estas palavras:
   *"o termo é assinado fora do sistema; o CourtTime guarda a versão enviada"*.
8. Volte ao **Início**: agora o cartão mostra **Documento aguardando
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
5. Abra uma solicitação já decidida: o painel **Decisão registrada** mostra
   resultado, responsável e data. Rejeitadas e canceladas ficam na lista
   própria, no fim da página de análise.

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
5. Ainda no **Aluno**: **Solicitar outro horário**. Mostre o aviso da reserva de
   origem, escolha o período liberado e envie. Diga que a nova solicitação
   recomeça do zero (novo PDF, conferência, contribuição e aprovação) e que só os
   participantes podem ser copiados.

## Se algo der errado — plano de contingência

| Problema | O que fazer |
|---|---|
| Nenhum período aparece | Falta grade de horários. Cadastre em Administração → Horários, no dia da semana certo |
| "A solicitação deve ser feita com pelo menos um dia de antecedência" | Você escolheu hoje. Use amanhã |
| Aplicação não sobe com erro de administrador | As variáveis do bootstrap ficaram definidas. Remova-as e suba de novo |
| Não consegue validar o documento | A lista de participantes mudou depois do envio, ou há menos de dez alunos |
| Banco em estado confuso | Rode `.\scripts\Reiniciar-Demonstracao.ps1` e recomece do zero |
| **A demonstração ao vivo falhar** | Mostre a execução automatizada: o teste `JornadaCompletaE2ETest` e o workflow **UI — navegador real** percorrem este roteiro no GitHub Actions, com PostgreSQL isolado. É a mesma jornada, executada pela máquina |

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
- formulário oficial da ETEC gerado preenchido, inclusive com mais de 30
  participantes, sem gravar CPF e curso;
- remarcação da reserva cancelada, uma única vez, pelo próprio aluno;
- autorização por perfil, proteção CSRF e bloqueio de acesso a dados de outro aluno.

Além do teste Java, o workflow **UI — navegador real** percorre a mesma
jornada no Chromium, com centenas de verificações, inclusive no celular.

## Limitações reais — diga à banca antes que perguntem

- O sistema **gera o formulário oficial preenchido**, mas **não assina**: as
  assinaturas e os campos da direção e do vigilante são preenchidos à mão.
- O sistema **não verifica criptograficamente** a assinatura do PDF. A
  conferência da secretaria é **manual e humana**.
- A remarcação após cancelamento recomeça do zero: **não reaproveita** aprovação, PDF nem contribuição.
- **Não há registro** de contato com o aluno.
- **Não há registro de ocorrências** pela segurança.
- **Não há** regra de retenção dos documentos enviados.
- O sistema **não calcula** o valor da contribuição.
- Decisões ainda pendentes da ETEC: limite de participantes, prazo para a
  decisão administrativa, coleta de CPF e curso, e o vínculo da categoria "Outro".

Apresentar essas lacunas como decisões conscientes é mais forte do que ser
pego por elas numa pergunta.
