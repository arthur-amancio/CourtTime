# CourtTime — desenvolvimento local rapido (Windows + VS Code + pgAdmin)

Esta configuracao e **somente para desenvolvimento local**. Ela nao modifica a
configuracao do banco de producao, nao habilita testes destrutivos nem carrega
dados ficticios automaticamente.

## Em resumo

**Uma unica vez:**

1. Instale o **JDK 17** e o **PostgreSQL 18** (o 16 tambem funciona; a CI testa
   os dois). No instalador do PostgreSQL, anote a senha do usuario `postgres`.
2. No pgAdmin, crie o banco vazio **`courttime_demo`**.
3. Confira a senha do PostgreSQL em
   **`src/main/resources/application.properties`** (linha
   `spring.datasource.password`). Por decisao do grupo, a configuracao local
   fica nesse unico arquivo e vai para o GitHub (banco local, sem API ou chave
   externa).
4. Rode `.\mvnw.cmd spring-boot:run` uma vez (cria as tabelas) e pare com Ctrl+C.
5. No pgAdmin, execute `scripts/popular-demo.sql` no `courttime_demo` (secao 2).

**Todo dia:** `.\mvnw.cmd spring-boot:run` e abra `http://localhost:8080/`.
Nenhuma variavel de ambiente e necessaria.

**Para nao perder os dados do `courttime_demo`:**

- nao defina `COURTTIME_POSTGRES_TEST` nem `SPRING_JPA_HIBERNATE_DDL_AUTO` na
  sua maquina (o verificador `scripts\Verificar-Ambiente-CourtTime.ps1` acusa os
  dois);
- nao rode `scripts\Reiniciar-Demonstracao.ps1`, a menos que queira apagar o
  banco de demonstracao de proposito;
- atualizacoes do sistema so **acrescentam** colunas e tabelas
  (`ddl-auto=update`); os registros existentes continuam la.

As secoes abaixo detalham cada passo.

## 1. Banco e senha local

1. Tenha Java 17 e PostgreSQL 18 (ou 16) instalados. Mantenha o servico PostgreSQL em
   execucao. O pgAdmin e opcional para iniciar a aplicacao: ele e apenas a
   interface grafica de administracao.
2. No pgAdmin, crie um banco vazio chamado **courttime_demo**. Nao use o banco
   `courttime` que eventualmente contenha trabalho existente.
3. Em `src/main/resources/application.properties`, confira a senha local do
   PostgreSQL no valor padrao de `spring.datasource.password`
   (`${COURTTIME_DB_PASSWORD:senha}`). Como esse arquivo vai para o GitHub, use
   uma senha exclusiva desse PostgreSQL local, que nao seja usada em nenhum
   outro lugar.

O `application.properties` ja aponta para `courttime_demo` com o usuario
`postgres`. Portanto, **nenhuma variavel de ambiente e necessaria**: basta
iniciar pelo botao do VS Code ou por `.\mvnw.cmd spring-boot:run`. As variaveis
`COURTTIME_DB_*` continuam aceitas e substituem esses valores (e assim que a CI
roda).

## 2. Criar esquema e popular o banco

Pela primeira vez, na raiz do repositorio no terminal do VS Code:

```powershell
.\mvnw.cmd spring-boot:run
```

Aguarde a aplicacao inicializar: o Hibernate `ddl-auto=update` cria as tabelas
necessarias e `QuadraInitializer` cadastra a unica quadra. A partir da segunda
execucao, os dados persistem normalmente.

No pgAdmin, abra **Query Tool** do banco **courttime_demo**, abra o arquivo
`scripts/popular-demo.sql`, substitua **somente no Query Tool** o valor de
`TROQUE_A_SENHA_DEMO_AQUI` por uma senha ficticia de 8 a 72 caracteres e
execute todo o script. A senha sera usada para todas as contas criadas agora,
com BCrypt via extensao `pgcrypto`. Nao grave nem comite uma copia da query
contendo essa senha. O script usa transacao, recusa qualquer banco diferente de
`courttime_demo`, preserva registros preexistentes e pode ser repetido sem
duplicar a base ja inserida. Dependendo do usuario PostgreSQL, criar a extensao
`pgcrypto` pode exigir permissao adicional.

As contas seguem os emails ficticios:

| Perfil | Contas | Exemplos de login |
| --- | ---: | --- |
| Administrador | 1 | `admin.demo@exemplo.invalid` |
| Secretaria | 2 | `secretaria1.demo@exemplo.invalid`, `secretaria2.demo@exemplo.invalid` |
| Seguranca | 2 | `seguranca1.demo@exemplo.invalid`, `seguranca2.demo@exemplo.invalid` |
| Aluno | 12 | `aluno01.demo@exemplo.invalid` ate `aluno12.demo@exemplo.invalid` |

**Atencao:** se o email ja existir, o script **nao troca** a senha anterior.
Essas contas existem apenas no banco descartavel de demonstracao e nao podem
ser usadas em nenhuma instalacao publicada na Internet.

### Dados gerados

- Oito faixas semanais ficticias entre **segunda e quinta**, de manha e a noite.
- Bloqueios ativos e inativos ao longo dos proximos **oito meses**, com motivos
  claramente identificados como dados de demonstracao.
- Solicitações `PENDENTE_DOCUMENTACAO` em varios meses, incluindo historico
  sintetico e futuro. Algumas recebem dez participantes ficticios.
- Nenhum arquivo PDF ficticio nem falsa contribuicao a APM: os estados de
  `APROVADA` e `PENDENTE_ANALISE` dependem da conferência documental e da
  contribuição e devem ser exercitados pelo fluxo real do sistema.

Os horarios nao representam o funcionamento oficial da quadra da ETEC.

## 3. Uso diario (sem configurar senha novamente)

1. Confirme que o **servico PostgreSQL** esta rodando (nao precisa abrir o
   pgAdmin).
2. Abra a pasta CourtTime no VS Code.
3. Execute `.\mvnw.cmd spring-boot:run` no terminal integrado.
4. Abra `http://localhost:8080/` e entre com uma das contas ficticias.

Nao rode `clean verify` com `COURTTIME_POSTGRES_TEST=true` no banco de
apresentacao. Os testes PostgreSQL sao destrutivos por projeto e devem usar o
banco dedicado `courttime_test`, protegido pelas verificacoes atuais.

### Primeira geracao do formulario oficial

Na primeira vez que alguem gera o formulario oficial preenchido depois de
iniciar a aplicacao, o PDFBox monta um cache das fontes do Windows e leva alguns
segundos. As geracoes seguintes sao imediatas. Faca uma geracao de teste antes
de apresentar.

## 4. Interface e apresentacao

As proximas melhorias de visual devem ocorrer em PRs pequenos de Thymeleaf e
CSS, separadas destes dados ficticios. Nao execute `Reiniciar-Demonstracao.ps1`
por rotina: o script **apaga** o banco demo, com confirmacao, e so serve
quando voce realmente quiser comecar de novo.

### Problemas frequentes

- Erro de conexao: confira `courttime_demo`, o servico PostgreSQL, usuario e
  senha em `src/main/resources/application.properties`.
- O bootstrap administrativo antigo reclama de contas existentes: remova
  `COURTTIME_BOOTSTRAP_ADMIN_ENABLED` do ambiente se ainda estiver definido.
- Os usuarios nao aparecem: verifique se voce executou o SQL no banco
  `courttime_demo` depois da primeira inicializacao do Spring Boot.
- Perfis da secretaria/seguranca nao mostram reservas aprovadas: apenas pedidos
  preliminares foram gerados; complete um fluxo real pelo Aluno/Secretaria/Admin.
