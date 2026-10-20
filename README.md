# CourtTime

Sistema Web de Gerenciamento e Agendamento de Quadras Esportivas. Trabalho de Conclusão de Curso do Técnico em Informática para Internet da ETEC Professor Armando José Farinazzo, Fernandópolis/SP, 2026.

O sistema possui uma página pública e autenticação por sessão com Spring Security. Usuários ativos armazenados no PostgreSQL podem entrar com email e senha, consultar a quadra da ETEC e visualizar sua disponibilidade por data quando os horários semanais estiverem cadastrados. Alunos ativos também podem registrar uma solicitação preliminar e acompanhar somente as próprias solicitações.

A consulta diária divide cada intervalo semanal em períodos de uma hora e considera o estado operacional da quadra e bloqueios específicos da data. Períodos fora do funcionamento cadastrado não são apresentados como disponíveis. O sistema utiliza o fuso `America/Sao_Paulo`. Nenhum horário oficial é criado automaticamente.

## Base de solicitações de reserva

O domínio de reservas registra solicitante, quadra, data, período de uma hora, finalidade e situação. Uma nova solicitação começa como `PENDENTE_DOCUMENTACAO`. O registro presencial da contribuição promove a solicitação para `PENDENTE_ANALISE` somente quando a versão mais recente do PDF está conferida, corresponde à revisão atual da lista e possui ao menos dez participantes na categoria `ALUNO`.

Na aprovação, o backend bloqueia a solicitação e o registro da única quadra durante a transação. Antes de autorizar, revalida o administrador, o documento e sua conferência manual, a revisão e o mínimo de alunos, a contribuição vinculada à conferência exata, os horários semanais, os bloqueios e as reservas já aprovadas. Essa serialização impede decisões repetidas e aprovações concorrentes para períodos conflitantes.

O formulário do aluno utiliza apenas períodos realmente disponíveis e identifica o solicitante pela sessão autenticada. O envio não autoriza imediatamente o uso da quadra: a solicitação permanece como `PENDENTE_DOCUMENTACAO`. Nessa etapa, o aluno pode revisar os participantes da própria solicitação e enviar versões do PDF preparado externamente. O CourtTime preserva cada arquivo original sem modificá-lo e não declara que a assinatura foi validada.

## Stack

Java 17, Spring Boot 3.5.16, Maven Wrapper, Thymeleaf, Spring Security, PostgreSQL e Spring Data JPA.

## Ambiente local rapido (VS Code + pgAdmin)

Para desenvolvimento diario com senha persistente em arquivo local ignorado pelo Git,
contas ficticias, horarios e bloqueios de exemplo, siga
[desenvolvimento local rapido](docs/desenvolvimento-local-rapido.md).
Nao e necessario redefinir variaveis de ambiente em toda execucao.

## Ensaio da interface V2 no computador da escola

Para testar a versão integrada no Windows com banco isolado e quatro perfis,
siga o [roteiro de ensaio V2](docs/teste-escola-v2.md). A execução manual não
usa a base automatizada `courttime_e2e` e não requer publicar o sistema na rede.

## Pré-requisitos

- Java 17 (JDK), com `JAVA_HOME` apontando para sua instalação;
- PostgreSQL 18 em execução (o 16 também funciona; a CI testa as duas versões);
- Git.

Não é necessário instalar Maven globalmente.

## Banco de dados

Crie um banco chamado `courttime` no PostgreSQL, se ele ainda não existir. Você pode fazer isso no pgAdmin ou com `CREATE DATABASE courttime;` no `psql` conectado ao banco `postgres`. A aplicação usa estas variáveis de ambiente:

| Variável | Valor esperado |
| --- | --- |
| `COURTTIME_DB_URL` | URL JDBC; padrão: `jdbc:postgresql://localhost:5432/courttime` |
| `COURTTIME_DB_USERNAME` | Usuário do banco; padrão: `postgres` |
| `COURTTIME_DB_PASSWORD` | Senha do usuário; obrigatória |
| `COURTTIME_DOCUMENT_STORAGE_PATH` | Diretório privado dos PDFs; padrão: `./data/documentos` |

Defina a senha apenas no seu ambiente, nunca em arquivos versionados. No PowerShell, ajuste o caminho do JDK 17 e execute:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$senha = Read-Host 'Senha do PostgreSQL' -AsSecureString
$env:COURTTIME_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $senha).Password
Remove-Variable senha
.\mvnw.cmd spring-boot:run
```

As variáveis `COURTTIME_DB_URL` e `COURTTIME_DB_USERNAME` são opcionais quando os valores padrão servem. Em produção, configure `COURTTIME_DOCUMENT_STORAGE_PATH` para um diretório persistente, privado e incluído na rotina autorizada de backup. O diretório não deve ser publicado por servidor web. Durante o desenvolvimento, o JPA/Hibernate cria e atualiza as tabelas no PostgreSQL com base nas entidades da aplicação.

## Primeiro administrador

Não existe cadastro público. O primeiro administrador pode ser provisionado uma única vez por um operador com acesso ao ambiente da aplicação. O mecanismo somente é executado quando habilitado explicitamente e não sobrescreve contas ou senhas existentes.

Configure as variáveis abaixo apenas para a primeira inicialização:

| Variável | Conteúdo |
| --- | --- |
| `COURTTIME_DEV_USER_NAME` | Nome exibido após o login |
| `COURTTIME_DEV_USER_EMAIL` | Email usado no login |
| `COURTTIME_DEV_USER_PASSWORD` | Senha que será armazenada como hash BCrypt |
| `COURTTIME_BOOTSTRAP_ADMIN_ENABLED` | `true` para autorizar explicitamente o bootstrap |
| `COURTTIME_DEV_USER_PROFILE` | Opcional; se informado, deve ser `ADMINISTRADOR` |

Se as variáveis não forem configuradas, nenhum usuário será criado. Se o email já existir, os dados e a senha não serão alterados.

No PowerShell, informe os valores apenas na sessão atual:

```powershell
$env:COURTTIME_DEV_USER_NAME = Read-Host 'Nome do usuário de teste'
$env:COURTTIME_DEV_USER_EMAIL = Read-Host 'Email do usuário de teste'
$senhaUsuario = Read-Host 'Senha do usuário de teste' -AsSecureString
$env:COURTTIME_DEV_USER_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaUsuario).Password
$env:COURTTIME_DEV_USER_PROFILE = 'ADMINISTRADOR'
$env:COURTTIME_BOOTSTRAP_ADMIN_ENABLED = 'true'
Remove-Variable senhaUsuario
.\mvnw.cmd spring-boot:run
```

Depois da criação, interrompa a aplicação e remova a autorização de bootstrap:

```powershell
Remove-Item Env:COURTTIME_BOOTSTRAP_ADMIN_ENABLED
Remove-Item Env:COURTTIME_DEV_USER_PASSWORD
```

Se já houver um administrador ativo, o bootstrap não cria outro. Caso o mesmo email já corresponda ao administrador existente, nenhuma senha ou informação será sobrescrita. Contas de aluno, secretaria e segurança devem ser criadas em **Administração → Usuários**.

Acesse `http://localhost:8080/`, selecione **Entrar** e use o email e a senha configurados. Após o login, a aplicação abre `/inicio`. O botão **Sair** encerra a sessão.

## Permissões

| Rota ou ação | Aluno | Administrador | Secretaria | Segurança |
| --- | --- | --- | --- | --- |
| Página pública e login | Sim | Sim | Sim | Sim |
| Consultar quadra e horários | Sim | Sim | Sim | Sim |
| Criar e consultar as próprias solicitações | Sim | Não | Não | Não |
| Gerenciar participantes das próprias solicitações pendentes | Sim | Não | Não | Não |
| Enviar e baixar PDFs das próprias solicitações | Sim | Não | Não | Não |
| Conferir PDFs e consultar o histórico documental | Não | Não | Sim | Não |
| Registrar contribuições presenciais e ajustes | Não | Não | Sim | Não |
| Painel `/admin/**` | Não | Sim | Não | Não |
| Analisar solicitações e baixar o PDF conferido | Não | Sim | Não | Não |
| Consultar reservas autorizadas e participantes | Não | Não | Não | Sim |
| Gerenciar horários e bloqueios | Não | Sim | Não | Não |
| Gerenciar usuários | Não | Sim | Não | Não |

O formulário comum permite criar somente `ALUNO`, `SECRETARIA` e `SEGURANCA`. Ele não promove contas para `ADMINISTRADOR`, não permite alterar o perfil de administradores existentes e não oferece Super Admin. A desativação, mudança de email ou perfil e redefinição de senha incrementam a versão de acesso da conta; sessões antigas são encerradas na próxima requisição.

Concessões administrativas futuras não estão disponíveis na interface comum. Até que a instituição defina o procedimento responsável por autorizá-las, qualquer ampliação desse perfil exige uma alteração específica, revisada e rastreável na aplicação; não deve ser feita por edição manual recorrente do banco.

## Administração de usuários

Administradores acessam `/admin/usuarios` para listar, pesquisar, filtrar, cadastrar, editar, ativar, desativar e redefinir senhas. As senhas são armazenadas com BCrypt e nunca são exibidas. Contas são desativadas logicamente para preservar reservas e registros históricos. O último administrador ativo e o próprio operador autenticado não podem ser desativados.

As operações relevantes são registradas em `auditorias_usuario`, sem senha, hash ou credencial temporária.

## Participantes e PDFs enviados

Antes do primeiro envio, ou depois de uma devolução documental para correção, o aluno proprietário pode cadastrar nome completo, RM ou RG e categoria `ALUNO` ou `OUTRO`, além de revisar as contagens separadas. Um mesmo RM ou RG não pode ser repetido na solicitação. A conferência e a aprovação exigem no mínimo dez participantes na categoria `ALUNO`; o limite máximo ainda depende da confirmação da ETEC e do documento oficial.

O upload aceita somente arquivos `.pdf` de até 10 MB como limite técnico. O conteúdo também deve iniciar com a assinatura de arquivo PDF. Cada envio cria uma nova versão com chave física aleatória, tamanho, SHA-256 e a revisão da lista de participantes; versões anteriores não são substituídas. Enquanto a versão aguarda conferência, a lista e um novo upload permanecem bloqueados. Os downloads passam por autorização da aplicação, usam resposta sem cache e nunca expõem o caminho físico.

### Formulário oficial preenchido

Na mesma tela, enquanto a solicitação aguarda documentação, o aluno informa CPF e curso e baixa o **formulário oficial da ETEC preenchido** com o nome do solicitante, a data, o horário, a data de preenchimento e os participantes. O arquivo original (`src/main/resources/modelos/formulario-reserva-quadra.pdf`) é usado como modelo sem alteração: o texto é sobreposto com Apache PDFBox. CPF e curso não são gravados; o CPF é enviado por POST e conferido pelos dígitos verificadores. Acima de 30 participantes, a página oficial de participantes é repetida, sem omitir ninguém. Assinaturas, presença, nome do vigilante e data de aprovação nunca são preenchidos pelo sistema. Detalhes e coordenadas em `docs/integracao-pdf-oficial.md`.

A secretaria pode conferir somente a versão mais recente, validá-la ou devolvê-la com justificativa. A validação exige que a lista não tenha mudado e contenha ao menos dez alunos. Cada decisão preserva o PDF e registra resultado, responsável e data. A conferência é manual: não comprova criptograficamente uma assinatura digital e não aprova a reserva. Retenção, descarte e eventual verificação técnica de assinatura permanecem pendentes de definição da ETEC.

## Contribuições presenciais

Após a validação da versão documental mais recente, a secretaria pode registrar uma única contribuição presencial por solicitação. O valor informado, a secretaria responsável e o instante do registro ficam vinculados à conferência documental exata. Na mesma transação, a solicitação passa para `PENDENTE_ANALISE`. Ajustes de valor ou observações exigem justificativa e criam eventos históricos com o estado anterior e o novo; não existe exclusão, estorno, reembolso nem pagamento eletrônico nesse fluxo.

O valor de R$ 2,00 por participante/hora é apenas uma referência institucional ainda sujeita à confirmação. Por isso, o CourtTime não calcula, preenche nem valida automaticamente o valor recebido. O registro da contribuição não aprova a reserva: a administração ainda revisa os dados e registra aprovação ou rejeição. A rejeição exige motivo; a aprovação comum não exige justificativa.

## Análise administrativa

O painel administrativo apresenta somente solicitações prontas para análise. O detalhe reúne solicitante, data, horário, finalidade, participantes, metadados do PDF e da conferência e a contribuição presencial. O administrador pode baixar o PDF original por uma rota privada, aprovar sem justificativa ou rejeitar com motivo obrigatório. A aprovação revalida todos os pré-requisitos dentro da transação e somente `APROVADA` representa autorização de uso.

O modelo oficial do termo e o procedimento institucional para avaliar a assinatura continuam pendentes do arquivo solicitado ao Leandro e da validação da ETEC. O CourtTime não recria o termo, não modifica PDFs enviados e não afirma realizar validação criptográfica de assinatura.

## Consulta da segurança

A segurança acessa somente reservas `APROVADA` para a data atual ou datas futuras. A lista e o detalhe apresentam data, horário, quadra, solicitante e a identificação dos participantes necessária à conferência presencial. Reservas pendentes, rejeitadas, canceladas ou antigas não ficam disponíveis nesse perfil; PDFs, contribuições, emails e histórico interno também não são expostos.

O registro de ocorrências permanece indisponível até a ETEC definir quais ocorrências podem ser registradas, quais dados são necessários e quem poderá consultá-los.

## Cancelamento institucional

Uma reserva já aprovada pode ser cancelada por um administrador ativo mediante motivo institucional obrigatório. O registro preserva o responsável, a data, a aprovação anterior, o PDF original, os participantes, a conferência documental e a contribuição. Após a transição para `CANCELADA`, a reserva deixa de aparecer como autorização para a segurança e o horário volta a ser considerado disponível.

O cancelamento não declara que houve contato com o aluno e não cria um reagendamento automaticamente.

### Remarcação após cancelamento

No detalhe de uma reserva cancelada, o aluno encontra **Solicitar outro horário**. A tela é o formulário comum de solicitação, com aviso da reserva de origem e a finalidade já preenchida, e passa pelas mesmas regras: antecedência mínima, grade semanal, bloqueios e disponibilidade real. A nova solicitação:

- começa em `PENDENTE_DOCUMENTACAO` e fica vinculada à cancelada (`reservas.remarcacao_de_id`, coluna única);
- pode copiar os participantes da cancelada (opção marcada por padrão), que continuam editáveis antes do envio do PDF;
- **não** reaproveita aprovação, PDF nem contribuição — esses dependem de regra institucional ainda não definida.

Cada reserva cancelada pode ser remarcada uma vez; a cancelada não é alterada e continua no histórico. A reserva de origem fica travada durante a operação, então dois envios simultâneos não criam duas remarcações. Registro de contato com o aluno, reaproveitamento da contribuição e dispensa de nova assinatura continuam pendentes das regras formais da ETEC.

## Testes e build

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

## Integração contínua

O GitHub Actions executa automaticamente os testes do projeto em pushes para a `main` e branches `feat/**`, além dos Pull Requests. O workflow roda a suíte duas vezes, com PostgreSQL 16 e 18 temporários, com dados exclusivamente de teste, para validar o mapeamento JPA, consultas, transações, rollback e aprovações concorrentes.
