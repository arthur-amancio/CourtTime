# CourtTime

Sistema Web de Gerenciamento e Agendamento de Quadras Esportivas. Trabalho de Conclusão de Curso do Técnico em Informática para Internet da ETEC Professor Armando José Farinazzo, Fernandópolis/SP, 2026.

Esta etapa contém apenas a fundação do projeto e uma página inicial.

## Stack

Java 17, Spring Boot 3.5.16, Maven Wrapper, Thymeleaf, PostgreSQL, Spring Data JPA e Flyway.

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

As variáveis `COURTTIME_DB_URL` e `COURTTIME_DB_USERNAME` são opcionais quando os valores padrão servem. Acesse `http://localhost:8080/`. O Flyway executa a migration inicial, que não cria tabelas de negócio. O Hibernate valida o esquema e não cria tabelas.

## Testes e build

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```
