# CourtTime V2 — ensaio no computador da escola (Windows)

> Guia de teste **local e manual**, com dados fictícios. Não é instrução de
> implantação em produção nem de disponibilização para a rede da escola.
> Revisado em 21/10/2026: inclui o formulário oficial preenchido e a remarcação.

## 1. Antes de começar

É necessário ter autorização para usar/instalar as ferramentas no computador.
Se a conta da escola não tiver permissão de instalação, **não tente contornar
as políticas de TI**: peça um equipamento de desenvolvimento autorizado ou
prepare o ambiente no computador pessoal e faça a apresentação por ele.

Requisitos: Windows 10/11, **JDK 17**, Git, **PostgreSQL 18** (ou 16) com acesso a um
banco local (pgAdmin facilita a configuração), PowerShell e acesso à Internet
na **primeira** compilação para baixar dependências do Maven Wrapper.
VS Code é opcional. O Maven global e o Node.js não são necessários para o
ensaio **manual**.

No PowerShell, confira:

```powershell
java -version
git --version
$env:JAVA_HOME
Get-Service *postgres* -ErrorAction SilentlyContinue
```

Se a máquina já possui uma cópia do projeto com mudanças locais, **não descarte
arquivos**: execute `git status` e preserve o trabalho antes de atualizar.

## 2. Baixar a versão integrada

Abra o terminal numa pasta em que você tenha permissão de escrita:

```powershell
git clone https://github.com/arthur-amancio/CourtTime.git
cd CourtTime
git switch main
git pull --ff-only origin main
git log -1 --oneline
```

Se já possui a pasta CourtTime, entre nela e execute `git status`, seguido de
`git switch main` e `git pull --ff-only origin main` **somente se não
houver mudanças locais incompatíveis**. Ensaie sempre a `main` mais recente;
anote o commit exibido por `git log -1 --oneline`.

## 3. Isolar o PostgreSQL

No **pgAdmin**, conectado ao seu PostgreSQL local, crie **uma vez** um banco
vazio chamado `courttime_demo`. Não utilize `courttime`, `courttime_test`,
`courttime_e2e` nem bancos da escola/produção. Não execute comandos
`DROP DATABASE` para iniciar o ensaio.

Na raiz do projeto, abra a configuração:

```powershell
notepad .\src\main\resources\application.properties
```

Mantenha a URL apontando para
`jdbc:postgresql://localhost:5432/courttime_demo`. Ajuste somente usuário e
senha **do PostgreSQL local**, se forem diferentes nesta máquina.

Certifique-se de que variáveis antigas não substituam essa configuração:

```powershell
Remove-Item Env:COURTTIME_DB_URL -ErrorAction SilentlyContinue
Remove-Item Env:COURTTIME_DB_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:COURTTIME_DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:COURTTIME_POSTGRES_TEST -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_JPA_HIBERNATE_DDL_AUTO -ErrorAction SilentlyContinue
Remove-Item Env:COURTTIME_BOOTSTRAP_ADMIN_ENABLED -ErrorAction SilentlyContinue
```

## 4. Iniciar e preparar dados fictícios

Execute a verificação inicial na **raiz**:

```powershell
.\scripts\Verificar-Ambiente-CourtTime.ps1
```

O script é de leitura e avisa se houver Java/JAVA_HOME, configuração ou
PostgreSQL incompatível. Caso a política do Windows bloqueie execução de
scripts, peça orientação à TI; não desabilite proteções globais da escola.

Depois inicie o servidor:

```powershell
.\mvnw.cmd spring-boot:run
```

Espere a confirmação de inicialização e abra
[http://localhost:8080/](http://localhost:8080/) no **mesmo computador**.
Na primeira execução o Hibernate cria as tabelas. Pare com `Ctrl+C` antes
de executar o povoamento opcional.

**Para já ter os quatro perfis e horários de demonstração:** no pgAdmin,
abra o Query Tool **do banco `courttime_demo`**, confirme
`SELECT current_database();` e carregue `scripts/popular-demo.sql`.
Substitua **somente no editor do Query Tool** o texto
`TROQUE_A_SENHA_DEMO_AQUI` por uma senha **fictícia** de 8–72 caracteres
(sem aspas ou caracteres especiais que quebrem a string SQL). Execute o script
completo. Não salve nem versiona a cópia com senha. Se a extensão `pgcrypto`
não puder ser criada, solicite permissão à TI ou utilize contas criadas pela
interface após o bootstrap descrito no
[guia do projeto](guia-do-courttime.md).

Inicie novamente com `.\mvnw.cmd spring-boot:run`.

## 5. Teste manual dos quatro perfis

O script cria usuários `@exemplo.invalid`, exclusivamente fictícios.
A senha inicial é a que você escolheu no Query Tool; se uma conta já existia,
o script **não** altera a senha dela.

| Perfil | E-mail de demonstração | Verificação |
| --- | --- | --- |
| Administrador | `admin.demo@exemplo.invalid` | Entrar, abrir painel, conferir grade semanal de **segunda a quinta** e usuários |
| Aluno | `aluno01.demo@exemplo.invalid` | Consultar quadra, escolher data futura e período disponível, criar solicitação |
| Secretaria | `secretaria1.demo@exemplo.invalid` | Abrir conferência de documentos e contribuições, sem aprovar reserva |
| Segurança | `seguranca1.demo@exemplo.invalid` | Visualizar somente reservas autorizadas; nenhuma contribuição ou PDF |

Para percorrer uma reserva completa, use **data futura e horário livre**,
registre **dez participantes da categoria Aluno** com identidades
explicitamente fictícias, envie **PDF sintético** e siga a ordem:

1. Aluno: solicitação → participantes → **Documentos → Gerar formulário
   preenchido** (CPF fictício válido, por exemplo `123.456.789-09`, e o curso)
   → envio do PDF.
2. Secretaria: devolver uma vez com justificativa (o aluno vê o motivo e envia
   nova versão) → validar a versão mais recente → registrar contribuição
   fictícia.
3. Administrador: analisar e aprovar.
4. Segurança: conferir reserva autorizada.
5. Administrador: cancelar **com motivo**, e verificar que a segurança deixa
   de ter a autorização.
6. Aluno: na reserva cancelada, **Solicitar outro horário** e conferir que a
   nova solicitação começa em *Pendente de documentação*, vinculada à anterior.
7. Administrador: em **Análise de solicitações**, conferir a lista
   *Rejeitadas e canceladas* e, no painel, o indicador *Canceladas*.

O CourtTime **gera o formulário oficial preenchido**, mas **não assina nem
verifica a assinatura** do PDF enviado, e não processa pagamentos. A primeira
geração depois de iniciar a aplicação demora alguns segundos (o PDFBox monta o
cache de fontes); faça uma antes de apresentar. Não use CPF, RM ou PDF de
pessoas reais para testar.

Para testar as telas, abra o DevTools do navegador (`F12`), ative o modo
responsivo e examine 390 px e 1440 px. Confira o menu móvel, campos, foco
de teclado (`Tab`, `Shift+Tab`, `Esc`), mensagens de erro e ausência
de rolagem horizontal. Em outra janela anônima, confirme que um perfil não
abre a área de outro. Esse teste manual **não substitui** auditoria com
leitor de tela ou Safari/Firefox.

## 6. Problemas comuns e cuidados

| Sintoma | Verificação |
| --- | --- |
| `java` não encontrado/versão incorreta | JDK 17 instalado e `JAVA_HOME` apontando para ele; reabra o terminal |
| Falha no Maven Wrapper | Internet/proxy permitindo acesso ao Maven Central na primeira compilação |
| PostgreSQL recusa conexão | Serviço, porta 5432, banco `courttime_demo`, usuário e senha do `application.properties` |
| `psql` não encontrado no verificador | Se o PostgreSQL funciona no pgAdmin, localize o diretório `bin` do PostgreSQL; não significa sozinho que o servidor caiu |
| Nenhum horário disponível | Verifique os horários de demonstração e selecione data futura coberta pela grade |
| Login inválido | Confirme o e-mail `@exemplo.invalid` e a senha digitada no Query Tool; script não redefine contas existentes |
| Tela antiga | Atualize a `main`, reinicie o Spring Boot e recarregue o navegador sem cache |

**Nunca** rode `Reiniciar-Demonstracao.ps1`, `DROP DATABASE`, testes E2E
ou testes de integração contra um banco com dados a preservar. O ensaio usa
somente `courttime_demo`. O comando `.\mvnw.cmd clean verify` pode
executar testes sem a variável de integração, mas para evitar configurações
herdadas, prefira acompanhar a CI no GitHub. O teste de navegador automatizado
usa outra instância e **somente** o banco descartável `courttime_e2e`.

Ao terminar, use `Ctrl+C` para encerrar o servidor. A URL
`localhost` funciona apenas na própria máquina; **não configura acesso
pela rede ou pela Internet**.

## 7. Evidências e reporte

- Código: [repositório CourtTime](https://github.com/arthur-amancio/CourtTime)
- CI: [GitHub Actions](https://github.com/arthur-amancio/CourtTime/actions)
- Detalhes funcionais: [guia do CourtTime](guia-do-courttime.md)
- Roteiro da banca: [apresentação](roteiro-apresentacao-tcc.md)

Se houver defeito, anote: commit exibido por `git log -1 --oneline`,
perfil, URL, passos de reprodução, resultado esperado e observado,
mensagem de erro (removendo dados pessoais e senhas) e captura de tela.
