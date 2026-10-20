# Baseline real de navegador — `436b262`

Estas imagens foram capturadas em 20/10/2026 por Chromium controlado via
Playwright, com a aplicação Spring Boot efetivamente em execução. Não são
mockups nem renderizações estáticas de Thymeleaf.

## Ambiente

| Item | Versão ou identificação |
|---|---|
| Código testado | `436b262edd5505abc616e83053de4244ba855e23` |
| Java | Eclipse Temurin 17.0.20.1 |
| PostgreSQL | 16.15, banco descartável `courttime_qa_redesign_final` |
| Chromium | 154.0.8037.57 |
| Playwright | 1.56.1 |
| axe-core | 4.10.3 |
| Viewports | 390 × 844, 430 × 932, 768 × 1024 e 1440 × 1000 |

Todos os nomes, emails, RMs, finalidades e valores exibidos foram gerados para
o teste. O PDF também é sintético, identificado como não oficial, e não
representa assinatura. Nenhuma senha aparece nas imagens ou nos JSONs.

## Amostra preservada

Foram mantidas quatro telas com dados e estruturas diferentes, cada uma nos
quatro viewports:

- Aluno: consulta da quadra;
- Secretaria: registro presencial de contribuição;
- Administração: solicitação elegível já aprovada;
- Segurança: conferência somente leitura de uma reserva autorizada.

A execução completa produziu 62 PNGs. Para não aumentar o repositório com telas
redundantes, a amostra versionada contém 16; todos os demais são reproduzidos
pelo roteiro em `qa/playwright`.

## Resultados verificáveis

- `resultados-e2e.json`: 306 verificações aprovadas, nenhuma reprovada;
- `resultados-axe.json`: 40 varreduras WCAG 2 A/AA e 2.1 A/AA, sem violação
  detectada pelo axe-core;
- nenhuma das páginas auditadas teve rolagem horizontal global;
- controles de formulário tinham rótulo; cada página tinha um `h1` e
  `lang="pt-BR"`; não foi encontrado `tabindex` positivo;
- o foco visível e o link “Pular para o conteúdo” foram exercitados por teclado.

O resultado zero do axe-core não prova ausência total de barreiras. A revisão
manual das capturas confirmou que conteúdo e ações permaneceram legíveis, mas
leitores de tela reais e usuários com tecnologia assistiva não fizeram parte
desta execução.

## Reprodução

Siga o [README da automação](../../../qa/playwright/README.md). A jornada cria
um banco novo e dados sintéticos; não deve ser apontada para `courttime`,
`courttime_demo` ou qualquer ambiente com dados existentes.
