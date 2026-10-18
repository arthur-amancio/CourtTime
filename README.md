# CourtTime

Sistema Web de Gerenciamento e Agendamento de Quadras Esportivas. Trabalho de Conclusão de Curso do Técnico em Informática para Internet da ETEC Professor Armando José Farinazzo, Fernandópolis/SP, 2026.

O sistema possui uma página pública e autenticação por sessão com Spring Security. Usuários ativos armazenados no PostgreSQL podem entrar com email e senha, consultar a quadra da ETEC e visualizar sua disponibilidade por data quando os horários semanais estiverem cadastrados. Alunos ativos também podem registrar uma solicitação preliminar e acompanhar somente as próprias solicitações.

A consulta diária divide cada intervalo semanal em períodos de uma hora e considera o estado operacional da quadra e bloqueios específicos da data. Períodos fora do funcionamento cadastrado não são apresentados como disponíveis. O sistema utiliza o fuso `America/Sao_Paulo`. Nenhum horário oficial é criado automaticamente.

## Base de solicitações de reserva

O domínio de reservas registra solicitante, quadra, data, período de uma hora, finalidade e situação. Uma nova solicitação começa como `PENDENTE_DOCUMENTACAO` e só pode ser analisada administrativamente depois de avançar para `PENDENTE_ANALISE`, transição que será vinculada à conferência do termo oficial em etapa própria.

Na aprovação, o backend bloqueia o registro da única quadra durante a transação e verifica novamente horários semanais, bloqueios e reservas já aprovadas. Essa serialização impede que duas análises concorrentes aprovem períodos conflitantes.

O formulário do aluno utiliza apenas períodos realmente disponíveis e identifica o solicitante pela sessão autenticada. O envio não autoriza imediatamente o uso da quadra: a solicitação permanece como `PENDENTE_DOCUMENTACAO`. Nessa etapa, o aluno pode revisar os participantes da própria solicitação e enviar versões do PDF preparado externamente. O CourtTime preserva cada arquivo original sem modificá-lo e não declara que a assinatura foi validada.

## Stack

Java 17, Spring Boot 3.5.16, Maven Wrapper, Thymeleaf, Spring Security, PostgreSQL e Spring Data JPA.

## Pré-requisitos

- Java 17 (JDK), com `JAVA_HOME` apontando para sua instalação;
- PostgreSQL em execução;
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
| Painel `/admin/**` | Não | Sim | Não | Não |
| Gerenciar horários e bloqueios | Não | Sim | Não | Não |
| Gerenciar usuários | Não | Sim | Não | Não |

O formulário comum permite criar somente `ALUNO`, `SECRETARIA` e `SEGURANCA`. Ele não promove contas para `ADMINISTRADOR`, não permite alterar o perfil de administradores existentes e não oferece Super Admin. A desativação, mudança de email ou perfil e redefinição de senha incrementam a versão de acesso da conta; sessões antigas são encerradas na próxima requisição.

Concessões administrativas futuras não estão disponíveis na interface comum. Até que a instituição defina o procedimento responsável por autorizá-las, qualquer ampliação desse perfil exige uma alteração específica, revisada e rastreável na aplicação; não deve ser feita por edição manual recorrente do banco.

## Administração de usuários

Administradores acessam `/admin/usuarios` para listar, pesquisar, filtrar, cadastrar, editar, ativar, desativar e redefinir senhas. As senhas são armazenadas com BCrypt e nunca são exibidas. Contas são desativadas logicamente para preservar reservas e registros históricos. O último administrador ativo e o próprio operador autenticado não podem ser desativados.

As operações relevantes são registradas em `auditorias_usuario`, sem senha, hash ou credencial temporária.

## Participantes e PDFs enviados

Enquanto a solicitação está em `PENDENTE_DOCUMENTACAO`, o aluno proprietário pode cadastrar nome completo, RM ou RG e categoria `ALUNO` ou `OUTRO`, além de revisar as contagens separadas. O mínimo de dez alunos é apresentado como requisito planejado; o momento de sua exigência e o limite máximo ainda dependem da confirmação da ETEC e do documento oficial.

O upload aceita somente arquivos `.pdf` de até 10 MB como limite técnico. O conteúdo também deve iniciar com a assinatura de arquivo PDF. Cada envio cria uma nova versão com chave física aleatória, tamanho e SHA-256; versões anteriores não são substituídas. Os downloads passam por autorização da aplicação, usam resposta sem cache e nunca expõem o caminho físico. A conferência pela secretaria, a retenção e o descarte permanecem pendentes de definição institucional.

## Testes e build

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

## Integração contínua

O GitHub Actions executa automaticamente os testes do projeto em pushes para a `main` e branches `feat/**`, além dos Pull Requests. O workflow utiliza um PostgreSQL 16 temporário, com dados exclusivamente de teste, para validar o mapeamento JPA, consultas, transações, rollback e aprovações concorrentes.
