# Checklist de inicialização no Windows

Este checklist prepara o ambiente de demonstração local. Ele não deve ser
usado para executar a jornada destrutiva de Playwright contra
`courttime_demo`; essa jornada exige outro banco, `courttime_e2e`.

## Primeira configuração

- [ ] `java -version` informa Java 17.
- [ ] PostgreSQL 18 (ou 16) está instalado e o serviço está em execução.
- [ ] O banco local de demonstração existe e não contém dados pessoais reais.
- [ ] URL, usuário e senha em `src/main/resources/application.properties`
  apontam para o PostgreSQL local esperado.
- [ ] O diretório privado de documentos é local, gravável e não servido como
  conteúdo estático.
- [ ] `COURTTIME_BOOTSTRAP_ADMIN_ENABLED` está ausente ou `false`, exceto na
  criação deliberada do primeiro administrador.
- [ ] `COURTTIME_POSTGRES_TEST` e qualquer opção destrutiva de DDL estão
  ausentes no terminal da demonstração.

No PowerShell, na raiz do projeto:

```powershell
.\scripts\Verificar-Ambiente-CourtTime.ps1
.\mvnw.cmd spring-boot:run
```

Não passe senhas na linha de comando. Quando for necessário usar outra senha
por sessão, sem alterar o `application.properties`, use `Read-Host -AsSecureString` e
converta-a apenas para a variável de ambiente do processo, conforme o README.

## Verificação funcional

- [ ] `http://localhost:8080/login` responde localmente.
- [ ] Os quatro usuários **fictícios** entram com os perfis ALUNO,
  ADMINISTRADOR, SECRETARIA e SEGURANÇA.
- [ ] O aluno abre `/quadra` e consulta uma data configurada na grade.
- [ ] O administrador abre solicitações, usuários, horários e bloqueios.
- [ ] A secretaria abre conferência documental e contribuições.
- [ ] A segurança abre somente reservas autorizadas.
- [ ] Logout é realizado pelo botão da interface e exige POST/CSRF.

## Persistência e reinicialização

1. Crie um registro sintético identificável na interface.
2. Encerre o Spring Boot normalmente com `Ctrl+C`.
3. Inicie novamente com `.\mvnw.cmd spring-boot:run`.
4. Confirme que o registro continua disponível e que nenhum bootstrap foi
   reativado.
5. Execute novamente `.\scripts\Verificar-Ambiente-CourtTime.ps1`.

Falhas esperadas no verificador devem ser corrigidas; não remova suas travas.
O script `scripts/popular-demo.sql` só deve ser executado após revisar o banco
alvo e confirmar que contém exclusivamente dados fictícios. Nunca o execute no
banco pessoal ou em uma base que precise ser preservada.

## Separação da automação E2E

- `courttime_demo`: uso manual e persistente; a jornada Playwright mutável é
  proibida.
- `courttime_e2e`: banco vazio, descartável e exclusivo da execução E2E na
  porta HTTP 18081.
- Evidências: devem mostrar apenas pessoas e documentos sintéticos.

As instruções completas permanecem em `docs/desenvolvimento-local-rapido.md` e
`qa/playwright/README.md`.
