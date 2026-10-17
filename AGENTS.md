# CourtTime — Development Rules

- Este é um projeto acadêmico de estudantes. Escreva código simples, organizado e compreensível para a apresentação à banca. Siga KISS e YAGNI.
- Não invente requisitos ou regras de negócio. Mudanças de negócio devem partir de decisões já definidas pelo grupo.
- Não adicione arquitetura, pacotes ou dependências sem necessidade. Mantenha Java 17, Spring MVC com Thymeleaf, Spring Data JPA com Hibernate e PostgreSQL. Evite frontend separado, API REST, Docker, JWT, React e Angular sem solicitação.
- Nunca armazene senhas, tokens, chaves ou outros segredos no Git.
- Não trabalhe diretamente na `main`. Use uma branch por tarefa e não faça merge sem autorização.
- Antes de concluir, execute os testes, revise `git diff`, faça um commit claro e publique a branch. Trabalho concluído nunca deve ficar somente local.
- Pare ao concluir a tarefa solicitada; não inicie a próxima etapa automaticamente.

## Direção visual e UX

- Trate o CourtTime como uma aplicação institucional, não como uma página promocional de SaaS. Priorize conteúdo específico, navegação objetiva e ações reconhecíveis.
- Evite elementos decorativos em excesso, como grandes áreas de destaque, gradientes, efeitos de vidro, formas abstratas, cartões genéricos, sombras intensas e animações sem função. Use cartões apenas quando houver agrupamento semântico.
- Não represente funcionalidades inexistentes. A navegação deve incluir somente páginas e ações que já fazem parte do sistema.
- Preserve a simplicidade compatível com um TCC técnico: hierarquia visual clara, boa leitura, contraste, foco de teclado visível e adaptação para computadores e celulares.

## Fluxo de colaboração assistida por IA

- Os papéis abaixo são responsabilidades lógicas no ChatGPT, não processos autônomos ou aprovações independentes.
- **Chief Architect:** verifica requisitos, decisões arquiteturais, riscos e escopo; não inventa regras institucionais.
- **Engineering Manager:** decompõe demandas em tarefas pequenas com critérios de aceite e dependências.
- **Backend Engineer:** implementa Java 17, Spring MVC, Spring Security, JPA e PostgreSQL dentro das convenções existentes.
- **Frontend Engineer:** mantém Thymeleaf, CSS e JavaScript existentes, com acessibilidade e design institucional.
- **QA & Security:** revisa autorização por perfil, CSRF, validação server-side, concorrência nas reservas, regressões e evidências dos testes.
- **DevOps Engineer:** acompanha Maven Wrapper, GitHub Actions e configurações; não faz deploy nem altera segredos sem aprovação.
- Consulte `docs/engineering-workflow.md` para o fluxo, a matriz de responsabilidade e as regras de entrega.
- Não confunda revisão simulada por múltiplos papéis do mesmo assistente com revisão humana independente.
- Antes de editar, leia as instruções vigentes e os arquivos relacionados. Preserve decisões, padrões, fluxos e testes já existentes.
- Para cada tarefa, registre escopo, critérios de aceite, testes executados (ou motivo da não execução), riscos e links para commits/PR.
- Crie PR draft para revisão; nunca faça merge, deploy, alterações destrutivas no banco ou mudança de permissões sem autorização explícita.
