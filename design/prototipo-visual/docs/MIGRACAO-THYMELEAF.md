# Plano de migração do visual para o Thymeleaf

**Só começa depois da aprovação final do layout.** Cada fase vira um PR pequeno, em branch própria, revisado antes da próxima. Nenhuma fase muda regras de negócio.

## Regras que valem para todas as fases

- Formulários continuam com `th:action`, `th:field` e token CSRF. O **logout continua `POST /logout` com CSRF** (no protótipo ele aparece como link).
- Autorização continua no servidor: perfil pelo Spring Security e propriedade da solicitação pelos serviços. Esconder um link nunca substitui a verificação.
- Toda ação precisa funcionar **sem JavaScript**. O JavaScript só melhora a experiência: menu móvel, nome do arquivo escolhido, contador de caracteres.
- **Não copiar** a lógica de `prototipo.js` que simula reservas, horários, status ou validação de envio. Os dados vêm dos controllers atuais.
- Textos de situação continuam vindo do backend: `StatusReserva.descricao`, `SituacaoSolicitacao.titulo/explicacao` e `SituacaoHorario.descricao`.
- Remover os elementos exclusivos do protótipo (lista em `DADOS-E-ESTADOS.md`, seção 4).
- Nenhuma dependência nova. `sec:authorize` exigiria `thymeleaf-extras-springsecurity`, que não está no projeto; usar o `perfil` enviado pelo controller.

Teste mínimo de cada PR: `./mvnw clean verify` (os testes de controller conferem rotas, textos e CSRF), capturas da **aplicação em execução** em 390 e 1440 px, axe-core sem violações nas telas alteradas e navegação completa por teclado.

---

## Fase A — Design system e componentes básicos

| Item | Conteúdo |
| --- | --- |
| Templates afetados | Nenhum muda de estrutura. As classes antigas continuam funcionando |
| CSS | Levar os tokens (`:root`), botões, campos, alertas e badges de `courttime.css` para `static/css/style.css`. Manter **aliases** das classes antigas (`.button` → `.botao`, `.status-pill` → `.badge`, `.form-message` → `.alerta`, `.request-notice` → `.alerta--info`, `.empty-message` → `.vazio`/`.alerta--neutro`, `.metric-card` → `.indicador`, `.table-cards` → `.tabela--cartoes`, `.details-list` → `.dados`) |
| JavaScript | Nenhum |
| Dados existentes | Todos os textos e enums atuais |
| Dados não disponíveis | — |
| Testes | `./mvnw clean verify`; comparação visual antes e depois das 27 telas; contraste das novas cores |
| Riscos | Classes antigas quebradas; contraste menor em algum estado |
| Aceite | Telas reais iguais ou melhores, sem mudança de HTML; nenhuma violação nova no axe |

## Fase B — Layout e navegação

| Item | Conteúdo |
| --- | --- |
| Templates afetados | Novo `templates/fragments/layout.html` (barra lateral, topo móvel, rodapé) e `fragments/icones.html`; todas as telas autenticadas passam a usar os fragmentos |
| CSS | `.lateral`, `.topo-movel`, `.breadcrumb`, `.rodape` e responsividade até 1024 px |
| JavaScript | ~40 linhas para o menu móvel: `aria-expanded`, `inert` no fundo, Tab preso no menu, Esc e restauração do foco (mesmo comportamento testado no protótipo). Sem JavaScript, a barra lateral aparece no topo da página como lista de links |
| Dados existentes | `perfil` e `nome` (hoje enviados ao `inicio.html`) |
| Dados não disponíveis | `perfil` e `nome` nas demais telas: expor com um `@ControllerAdvice` simples. Contadores da barra lateral **ficam fora** sem decisão do grupo |
| Testes | Testes de controller de todas as rotas; logout por POST com CSRF; cada perfil vê só os próprios links; teclado em 390, 768 e 1440 px |
| Riscos | Link para rota não autorizada; perda do CSRF no logout; foco preso se o JavaScript falhar |
| Aceite | Os quatro perfis navegam só por rotas existentes; menu passa nos mesmos testes de teclado do protótipo |

## Fase C — Página pública e login

| Item | Conteúdo |
| --- | --- |
| Templates afetados | `index.html`, `login.html` |
| CSS | `.apresentacao`, `.faixa-info`, `.passos`, `.regras`, `.login*` (inclui a faixa compacta do celular) |
| JavaScript | Nenhum |
| Dados existentes | Conteúdo estático; `param.error` e `param.logout` do login |
| Dados não disponíveis | Fotografia oficial: depende de autorização (`RECURSOS-VISUAIS.md`). Até lá, a ilustração própria |
| Testes | Login válido e inválido; mensagem de logout; nenhuma reserva ou participante exposto na página pública |
| Riscos | Esquecer de remover os atalhos “Somente no protótipo” |
| Aceite | Formulário de login visível sem rolagem em 390 × 844; atalhos de perfil ausentes |

## Fase D — Aluno

| Item | Conteúdo |
| --- | --- |
| Templates afetados | `inicio.html`, `quadra.html`, `solicitacoes/formulario.html`, `lista.html`, `detalhe.html`, `participantes/*`, `documentos/lista.html` |
| CSS | Calendário, `.faixa-dias` (opção A), `.slots`, `.etapas`, `.upload`, `.barra-acao-movel`, `.tabela--cartoes` |
| JavaScript | Opcional: nome do arquivo escolhido no envio de PDF; contador de caracteres. A barra fixa funciona só com CSS |
| Dados existentes | `consulta.horarios` (situação por período), grade semanal para o calendário da opção A, `consulta.quadraAtiva`, `dataMinima`, `situacao.titulo/explicacao`, `resumo` de participantes, versões do documento com `enviadoEm`, resultado e justificativa da conferência |
| Dados não disponíveis | Opção B do calendário (vagas por dia; ver `CALENDARIO-MOBILE.md`); histórico com datas de criação, conferência, contribuição e decisão (ver `DADOS-E-ESTADOS.md`) |
| Implementação dos horários | Trocar o `<select>` por `<input type="radio" name="horarioInicio">` estilizado como `.slot`; dias do calendário viram links `GET ?data=`; o envio continua sendo o `POST /solicitacoes` atual |
| Implementação do upload | Manter `<input type="file" accept=".pdf">` real com `<label>` “Selecionar PDF”; o servidor continua validando tipo, assinatura de arquivo PDF e 10 MB |
| Testes | Criação de solicitação com período livre e ocupado; antecedência de 1 dia; quadra inativa; dono da solicitação; upload de PDF válido, não PDF e acima de 10 MB; lista bloqueada durante a conferência; zoom 200% e 400% |
| Riscos | Radios sem `name`/valor certo quebram o `th:field`; etapas confundidas com status; mostrar o histórico sem os dados existirem |
| Aceite | Fluxo completo sem JavaScript; mesmos textos de situação do serviço; nenhum indicativo de aprovação automática |

## Fase E — Administrativo

| Item | Conteúdo |
| --- | --- |
| Templates afetados | `admin/painel.html`, `admin/solicitacoes/*`, `admin/usuarios/*`, `admin/horarios/*`, `admin/bloqueios/*` |
| CSS | `.indicadores`, `.semana` (visão da grade), tabelas, filtros, painel de decisão |
| JavaScript | Nenhum obrigatório |
| Dados existentes | Total, pendentes de documentação, pendentes de análise, aprovadas, rejeitadas, horários semanais e bloqueios futuros (`PainelAdministrativo`); fila e reservas autorizadas; dados de usuários e paginação |
| Dados não disponíveis | Contagem de **canceladas**; contas ativas no início; visão da semana agrupada (dá para agrupar no template, sem consulta nova) |
| Testes | Aprovar, rejeitar sem motivo (deve falhar), cancelar com motivo; concorrência na aprovação; último administrador não desativável; filtros de usuários |
| Riscos | Botões de ação perigosa com destaque errado; formulário de decisão sem CSRF |
| Aceite | Só indicadores existentes, ou aprovados e implementados em PR próprio |

## Fase F — Secretaria e Segurança

| Item | Conteúdo |
| --- | --- |
| Templates afetados | `secretaria/documentos/*`, `secretaria/contribuicoes/*`, `seguranca/reservas/*` |
| CSS | `.arquivo-pdf`, `.reserva-portaria`, `.dia-reservas`, tabelas em cartões |
| JavaScript | Nenhum |
| Dados existentes | Fila documental, histórico de conferências, contribuições e ajustes, reservas autorizadas e participantes (sem PDF, sem valores) |
| Dados não disponíveis | Agrupamento “Hoje / Amanhã” pode ser feito no template; contadores no início dependem de adaptação |
| Testes | Validar e devolver (justificativa obrigatória); registrar contribuição só após validação; a segurança não acessa PDF, valores nem reservas não aprovadas |
| Riscos | Expor dado financeiro ou PDF à segurança; lista “O que conferir” confundida com registro |
| Aceite | Cada perfil vê somente o que já vê hoje |

## Ordem e dependências

A → B → C → D → E → F. As fases D, E e F são independentes entre si depois da B. Qualquer item marcado como “dependente de adaptação” precisa de uma decisão do grupo e de um PR de backend próprio, com testes, **antes** de aparecer na interface.
