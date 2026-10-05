# CourtTime

Sistema Web de Gerenciamento e Agendamento de Quadras Esportivas. Trabalho de Conclusão de Curso do Técnico em Informática para Internet da ETEC Professor Armando José Farinazzo, Fernandópolis/SP, 2026.

O sistema possui uma página pública e autenticação por sessão com Spring Security. Usuários ativos armazenados no PostgreSQL podem entrar com email e senha e acessar a página protegida `/inicio`.

## Stack

Java 17, Spring Boot 3.5.16, Maven Wrapper, Thymeleaf, Spring Security, PostgreSQL, Spring Data JPA e Flyway.

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

Defina a senha apenas no seu ambiente, nunca em arquivos versionados. No PowerShell, ajuste o caminho do JDK 17 e execute:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$senha = Read-Host 'Senha do PostgreSQL' -AsSecureString
$env:COURTTIME_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $senha).Password
Remove-Variable senha
.\mvnw.cmd spring-boot:run
```

As variáveis `COURTTIME_DB_URL` e `COURTTIME_DB_USERNAME` são opcionais quando os valores padrão servem. O Flyway cria e atualiza a tabela `usuarios`, e o Hibernate apenas valida o esquema.

## Usuário local para desenvolvimento

Não existe cadastro público nesta etapa. Para criar um único usuário local de teste, configure as quatro variáveis abaixo antes de iniciar a aplicação:

| Variável | Conteúdo |
| --- | --- |
| `COURTTIME_DEV_USER_NAME` | Nome exibido após o login |
| `COURTTIME_DEV_USER_EMAIL` | Email usado no login |
| `COURTTIME_DEV_USER_PASSWORD` | Senha que será armazenada como hash BCrypt |
| `COURTTIME_DEV_USER_PROFILE` | `ALUNO`, `ADMINISTRADOR`, `SECRETARIA` ou `SEGURANCA` |

Se as variáveis não forem configuradas, nenhum usuário será criado. Se o email já existir, os dados e a senha não serão alterados.

No PowerShell, informe os valores apenas na sessão atual:

```powershell
$env:COURTTIME_DEV_USER_NAME = Read-Host 'Nome do usuário de teste'
$env:COURTTIME_DEV_USER_EMAIL = Read-Host 'Email do usuário de teste'
$senhaUsuario = Read-Host 'Senha do usuário de teste' -AsSecureString
$env:COURTTIME_DEV_USER_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaUsuario).Password
$env:COURTTIME_DEV_USER_PROFILE = 'ALUNO'
Remove-Variable senhaUsuario
.\mvnw.cmd spring-boot:run
```

Acesse `http://localhost:8080/`, selecione **Entrar** e use o email e a senha configurados. Após o login, a aplicação abre `/inicio`. O botão **Sair** encerra a sessão.

## Testes e build

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

## Integração contínua

O GitHub Actions executa automaticamente os testes do projeto em pushes para a `main` e em Pull Requests destinados à `main`.
