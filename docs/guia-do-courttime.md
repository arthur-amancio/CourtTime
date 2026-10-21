# CourtTime — guia do projeto

Sistema de gerenciamento e agendamento da quadra esportiva da ETEC Prof. Armando
José Farinazzo, Fernandópolis/SP. Trabalho de Conclusão de Curso do Ensino Médio
integrado ao Técnico em Informática para Internet, 2026.

Equipe: Arthur Amancio Francisco, Carlos E. R. Alves, Gustavo T. Sabadini e
Heitor S. Rodrigues. Orientador: André L. Z. Mosca.

## Para que serve

A escola empresta a quadra mediante um formulário em papel, assinado e entregue
na Secretaria. O CourtTime informatiza a solicitação e o acompanhamento desse
processo, respeitando as exigências do termo oficial da ETEC.

O sistema **não** substitui o termo. Ele organiza quem pediu, quando, com quais
participantes, se a documentação foi conferida, se a contribuição à APM foi
recebida e se a direção autorizou.

## Os quatro perfis

| Perfil | O que faz |
|---|---|
| **Aluno** | solicita o uso, cadastra participantes, envia o PDF assinado e acompanha a situação |
| **Secretaria** | confere o documento enviado e registra a contribuição recebida presencialmente |
| **Administrador** | gerencia contas, horários e bloqueios; aprova, rejeita e cancela reservas |
| **Segurança** | consulta as reservas autorizadas e a identificação dos participantes na portaria |

A secretaria **não aprova** reservas. A segurança **não edita** nada.

## Como uma solicitação caminha

1. O aluno consulta a quadra e escolhe um período de uma hora disponível.
2. Registra a solicitação. Ela nasce como `PENDENTE_DOCUMENTACAO`.
3. Cadastra os participantes, com nome e RM ou RG, marcando Aluno ou Outro.
4. Gera no próprio sistema o **formulário oficial da ETEC preenchido** (CPF e
   curso são digitados na hora e não ficam gravados), imprime, assina à mão ou
   pelo gov.br e envia o PDF assinado.
5. A secretaria confere a versão mais recente: valida ou devolve com justificativa.
6. Se validada, a secretaria registra a contribuição presencial. A solicitação
   passa a `PENDENTE_ANALISE`.
7. A administração analisa e aprova ou rejeita. Na aprovação o sistema bloqueia
   o registro da quadra na transação e reconfere horários, bloqueios e reservas
   já aprovadas, para que duas análises simultâneas não autorizem o mesmo período.
8. A segurança passa a ver a reserva aprovada.
9. A administração pode cancelar institucionalmente uma reserva aprovada, com
   motivo obrigatório. A reserva sai da consulta da segurança e o aluno vê o
   motivo.
10. O aluno pode pedir **outro horário** a partir da reserva cancelada. A nova
    solicitação recomeça do zero (novo PDF, conferência, contribuição e
    aprovação); só os participantes podem ser copiados, se o aluno quiser.
    Rejeitadas e canceladas continuam consultáveis pela administração.

Enviar o documento **não** autoriza o uso. Registrar a contribuição **não**
autoriza o uso. Só a aprovação da administração autoriza.

## Regras que o sistema aplica hoje

- A solicitação precisa ser feita com pelo menos **um dia de antecedência**.
- O PDF assinado precisa ser enviado **até o dia anterior ao uso** (R01). No
  dia da utilização, o envio é recusado, mesmo depois de uma devolução.
- **Não há limite máximo** de participantes (R02). Acima de 30, o formulário
  oficial ganha páginas adicionais.
- O período é sempre de **uma hora**, alinhado à grade cadastrada.
- São necessários no mínimo **dez alunos** participantes para validar o documento.
- A aprovação é recusada se o horário de utilização **já começou ou já passou**.
- A contribuição só pode ser registrada depois de o documento ser validado.
- A contribuição é calculada: **R$ 2,00 por participante**, Aluno ou Outro
  (R03). A secretaria só registra o recebimento.
- Um administrador não analisa a própria solicitação.
- O último administrador ativo não pode ser desativado.

## O que ainda NÃO existe

Para não prometer na banca o que o código não faz:

- O sistema **gera o formulário oficial preenchido** (data, horário,
  participantes, CPF e curso digitados na hora), mas **não assina** nada: as
  assinaturas e os campos da direção e do vigilante continuam à mão.
- O sistema **não verifica** se o PDF enviado está realmente assinado. A
  conferência da secretaria é manual.
- A **remarcação** após cancelamento cria uma solicitação nova, do início: não
  reaproveita aprovação, PDF nem contribuição (só os participantes, se o aluno
  pedir).
- **Não há registro** de contato com o aluno.
- **Não há registro de ocorrências** pela segurança.
- **Não há** regra de retenção dos documentos enviados.

## Decisões da ETEC

Em 21/10/2026, o procedimento da quadra foi explicado pelo Leandro. As nove
regras (R01 a R09) e os requisitos que saem delas estão em
[regras-etec.md](regras-etec.md). O plano de implementação está em
[plano-regras-etec.md](plano-regras-etec.md). Enquanto os PRs de cada regra não
forem integrados, vale o comportamento descrito neste guia.

Continuam em aberto o horário-limite do envio no dia anterior, o destino do
crédito excedente na remarcação, a autorização de pessoas extras na portaria e
a política de retenção dos documentos.

---

# Execução local no Windows

**Caminho recomendado:** siga o [desenvolvimento local rápido](desenvolvimento-local-rapido.md).
A senha fica uma única vez em `application-local.properties` (ignorado pelo
Git), o `scripts/popular-demo.sql` cria contas e horários fictícios e, depois
disso, basta `.\mvnw.cmd spring-boot:run` todos os dias. Os dados continuam no
banco depois de reiniciar.

Os passos abaixo são a **alternativa sem arquivo local**, só com variáveis de
ambiente (é assim que a CI roda). Não é preciso fazer os dois.


## Pré-requisitos

- JDK 17, com `JAVA_HOME` apontando para a instalação.
- PostgreSQL em execução.
- Git.

Não é preciso instalar Maven: o projeto usa o Maven Wrapper (`mvnw.cmd`).

## 1. Banco isolado de demonstração

Nunca use um banco existente. No `psql`, conectado ao banco `postgres`:

```sql
CREATE DATABASE courttime_demo;
```

## 2. Variáveis de ambiente

As senhas são digitadas na sessão do PowerShell e **nunca** vão para o Git.

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:COURTTIME_DB_URL = 'jdbc:postgresql://localhost:5432/courttime_demo'
$env:COURTTIME_DB_USERNAME = 'postgres'
$senha = Read-Host 'Senha do PostgreSQL' -AsSecureString
$env:COURTTIME_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $senha).Password
Remove-Variable senha
```

## 3. Primeira conta administrativa

O bootstrap cria **apenas o primeiro ADMINISTRADOR**. Se já existir um
administrador ativo e você tentar de novo com outro email, a aplicação
**falha ao iniciar**. É proposital.

```powershell
$env:COURTTIME_BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:COURTTIME_DEV_USER_NAME  = 'Administrador Demo'
$env:COURTTIME_DEV_USER_EMAIL = 'admin.demo@exemplo.local'
$env:COURTTIME_DEV_USER_PROFILE = 'ADMINISTRADOR'
$s = Read-Host 'Senha do admin (8 a 72 caracteres)' -AsSecureString
$env:COURTTIME_DEV_USER_PASSWORD = [System.Net.NetworkCredential]::new('', $s).Password
Remove-Variable s
.\mvnw.cmd spring-boot:run
```

Acesse `http://localhost:8080/` e entre com esse email e senha.

## 4. Limpar o bootstrap

Pare a aplicação e remova as variáveis antes de rodar de novo:

```powershell
Remove-Item Env:COURTTIME_BOOTSTRAP_ADMIN_ENABLED
Remove-Item Env:COURTTIME_DEV_USER_PASSWORD
Remove-Item Env:COURTTIME_DEV_USER_EMAIL
Remove-Item Env:COURTTIME_DEV_USER_NAME
Remove-Item Env:COURTTIME_DEV_USER_PROFILE
.\mvnw.cmd spring-boot:run
```

## 5. Demais contas

Logado como administrador, crie em `/admin/usuarios` as contas fictícias de
**aluno**, **secretaria** e **segurança**. Use emails de exemplo, como
`aluno.demo@exemplo.local`. Nunca use dados reais de pessoas da escola.

## 6. Grade de horários

Em `/admin/horarios`, cadastre pelo menos um intervalo semanal. **Sem isso
nenhum período aparece como disponível** e o aluno não consegue solicitar.

Em `/admin/bloqueios` é possível bloquear datas específicas.

## 7. Cenário de teste

Para exercitar o fluxo completo é preciso respeitar as regras reais:

- a data precisa ser de amanhã em diante;
- a solicitação precisa de **no mínimo dez alunos** participantes;
- o PDF enviado é fictício e **não deve ser apresentado como termo assinado**.

## 8. Testes

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

Os testes de integração exigem PostgreSQL. O GitHub Actions executa o
`clean verify` completo com PostgreSQL 16 e 18 temporários a cada push e Pull
Request.
