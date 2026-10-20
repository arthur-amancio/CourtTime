# Redesign CourtTime — Fase A: design system

**Origem:** protótipo V2 aprovado como direção visual no [PR #49](https://github.com/arthur-amancio/CourtTime/pull/49), commit `766c430`.

**Escopo deste PR:** transportar tokens, cores semânticas, botões, campos,
alertas, badges, painéis, indicadores e tabelas de `courttime.css` para
`src/main/resources/static/css/style.css` **sem substituir o layout
das páginas**.

## Compatibilidade

- As classes de produção existentes, como `.button`, `.status-pill`,
  `.form-message`, `.metric-card`, `.details-list`, `.table-cards` e
  `.request-notice`, permanecem intactas.
- As novas classes `.botao`, `.badge`, `.alerta`, `.painel`, `.campo` e
  `.tabela` são usadas **somente** quando um template for migrado em fase
  posterior, evitando quebrar telas que ainda têm a marcação antiga.
- As cores de base `--page`, `--surface`, `--accent`, `--text` e
  `--muted` não foram alteradas. Os tokens novos apontam para elas.
- Nenhum controller, serviço, entidade, template, endpoint, JavaScript,
  formulário, token CSRF ou regra de acesso foi modificado.
- Não há dependência externa de fontes, imagens ou bibliotecas.

## Acessibilidade

Cada situação utiliza cor **e rótulo textual** no HTML (aplicado nas próximas
fases). A configuração de foco visível utiliza `#875c00`, preservando o
contorno do sistema atual. Os componentes novos continuam utilizáveis sem
JavaScript. Alvos dos botões têm 44 px por padrão, com ajuste no celular.

## Fases seguintes, após revisão

B. Layout e navegação dos quatro perfis, preservando `POST /logout` com CSRF.
C. Página inicial e login com identidade institucional compacta.
D. Calendário **A** para o aluno (sem contagem de vagas por dia), solicitação,
   participantes e upload PDF.
E. Administração.
F. Secretaria e Segurança.

**Não incorporar** diretamente o PR #49, seu JavaScript de dados fictícios,
atalhos de perfil, histórico não disponível, imagens externas sem autorização
ou indicadores sem origem no backend.

## Critérios de aceite desta fase

1. O CSS original permanece completo, com os tokens e componentes novos ao final.
2. O Java/Spring Boot continua compilando e passando na CI PostgreSQL 16.
3. A aplicação atual mantém sua navegação, formulários e mensagens intactos.
4. Antes da Fase B, comparar capturas da aplicação real com o baseline do PR #48
   e verificar responsividade e acessibilidade.
