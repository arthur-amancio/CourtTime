# Fluxo de engenharia assistida por IA — CourtTime

## Objetivo e limites
Este documento organiza responsabilidades **lógicas** para sessões do ChatGPT e colaboradores humanos. Não cria agentes independentes, processos paralelos permanentes, revisão independente nem execução em segundo plano. O responsável humano mantém a decisão final.

## Fontes de verdade
1. Requisitos institucionais confirmados pelo grupo e documentação do TCC.
2. Código, testes, README e AGENTS.md vigentes na branch-base.
3. Issues e decisões documentadas e aprovadas.
Em conflito, interromper a implementação e pedir esclarecimento. Não inferir campos de termos institucionais, horários oficiais ou políticas de aprovação.

## Papéis
| Papel | Responsabilidade | Entrega |
| --- | --- | --- |
| Chief Architect | Avaliar impacto, dependências e aderência à arquitetura existente | Decisão técnica e riscos |
| Engineering Manager | Dividir escopo e definir critérios verificáveis | Plano curto e checklist |
| Backend Engineer | Implementar regras de negócio e persistência | Código e testes |
| Frontend Engineer | Manter telas Thymeleaf, UX e acessibilidade | Templates e validações |
| QA & Security | Revisar autorização, segurança, concorrência e regressões | Evidências e pendências |
| DevOps Engineer | Verificar CI, configuração e deploy | Estado dos checks e riscos |

Um único assistente pode desempenhar mais de um papel; isso **não** equivale a revisores independentes.

## Procedimento por demanda
1. Ler AGENTS.md, README, arquivos afetados, testes e PRs relevantes antes de propor alterações.
2. Definir problema, escopo, critérios de aceite, dependências, riscos e mudanças fora do escopo.
3. Criar uma branch por tarefa a partir da base atualizada. Não trabalhar diretamente na main.
4. Fazer a menor alteração que resolva o problema; preservar Java 17, Spring Boot 3.5.16, Maven Wrapper, Thymeleaf, Spring Security, JPA e PostgreSQL.
5. Validar autorização por perfil e proprietário, CSRF em formulários, validação no servidor, tratamento de erros e concorrência de reservas quando aplicável.
6. Executar `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`) em ambiente apropriado. Os testes de integração dependem do PostgreSQL configurado; não declarar sucesso sem execução.
7. Revisar o diff e abrir PR draft com evidências, riscos e limitações. Verificar GitHub Actions (CI existente em `.github/workflows/ci.yml`, com PostgreSQL 16).
8. Solicitar aprovação humana para merge, deploy, exclusão, migrações destrutivas e alterações de permissões ou segredos. Encerrar ao cumprir o escopo.

## Critérios de conclusão
- Critérios de aceite demonstrados; código e documentação coerentes.
- Testes executados e resultados informados, ou impedimento explicitado.
- Sem segredos, credenciais ou dados pessoais adicionados ao Git.
- Branch publicada e PR draft criado; checks acompanhados quando disponíveis.
- Pendências e limitações claramente registradas.

## Restrições específicas do CourtTime
- Sistema institucional acadêmico; não criar frontend separado, JWT, Docker ou infraestrutura sem necessidade aprovada.
- Não inventar horários oficiais, participantes, documentos ou regras de negócio.
- Preservar a semântica de solicitações preliminares e o controle transacional contra reservas concorrentes.
- Evitar alterações cosméticas não relacionadas e dependências adicionais desnecessárias.
