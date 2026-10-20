# Migração do visual aprovado para o Thymeleaf

**Só começa depois da aprovação do protótipo.** A ordem abaixo permite migrar tela por tela, com um PR pequeno por etapa, sem mexer em regras de negócio.

## Princípios

- Nenhuma mudança em controllers, services, repositories, entidades, segurança ou banco para o visual básico.
- Os textos de situação continuam vindo do backend (`StatusReserva.descricao`, `SituacaoSolicitacao.titulo/explicacao`, `SituacaoHorario.descricao`).
- Formulários continuam com `th:action`, `th:field` e token CSRF; nada passa a depender de JavaScript para funcionar.

## Etapa 1 — Base (1 PR)

1. Copiar os tokens e componentes de `assets/css/courttime.css` para `src/main/resources/static/css/style.css`, removendo o que for só do protótipo (`.faixa-prototipo`, `.ds-*`, `.direcao*`).
2. Mapear nomes atuais para os novos ou manter aliases durante a transição:

| Atual | Novo |
| --- | --- |
| `.button`, `.button--secondary`, `.button--small` | `.botao`, `.botao--secundario`, `.botao--sm` |
| `.status-pill--available` / `--unavailable` / `--ok` | `.badge--aprovada` / `.badge--cancelada` + ícone |
| `.form-message--success` / `--error` | `.alerta--sucesso` / `.alerta--perigo` |
| `.request-notice` | `.alerta--info` |
| `.empty-message` | `.vazio` ou `.alerta--neutro` |
| `.metric-card` | `.indicador` |
| `.table-cards` | `.tabela--cartoes` |
| `.details-list` | `.dados` |

3. Ícones: criar `templates/fragments/icones.html` com um `th:fragment="icone(nome)"` que imprime o `<svg>` inline (os desenhos estão em `ICONES` no `prototipo.js`).

## Etapa 2 — Layout por perfil (1 PR)

Criar `templates/fragments/layout.html` com:
- `th:fragment="lateral(pagina)"`: barra lateral com links por perfil usando o `perfil` já enviado ao `inicio.html` (expor o mesmo atributo às demais telas com um `@ControllerAdvice`; `sec:authorize` exigiria a dependência thymeleaf-extras-springsecurity, que não está no projeto). Apenas rotas que já existem.
- `th:fragment="topo-movel"`: botão “Menu”. Precisa de ~15 linhas de JavaScript para abrir/fechar; sem JS, usar `<details>` como alternativa.
- `th:fragment="rodape"`.
O formulário de logout continua `POST /logout` com CSRF (no protótipo é um link).

## Etapa 3 — Telas, em ordem de impacto

| Ordem | Protótipo | Template real | Observações |
| --- | --- | --- | --- |
| 1 | `index.html` | `index.html` | Conteúdo estático. Foto só após autorização (ver `RECURSOS-VISUAIS.md`) |
| 2 | `login.html` | `login.html` | **Remover** os atalhos “Somente no protótipo”. Manter `param.error`/`param.logout` |
| 3 | `inicio.html` | `inicio.html` | Já recebe `perfil` e `ultimaSolicitacao` |
| 4 | `quadra.html` | `quadra.html` | Blocos usam `consulta.horarios` (`situacao`, `periodoFormatado`). Calendário: ver nota A |
| 5 | `aluno-nova-solicitacao.html` | `solicitacoes/formulario.html` | Ver nota B |
| 6 | `aluno-solicitacoes.html`, `aluno-solicitacao.html` | `solicitacoes/lista.html`, `detalhe.html` | Etapas derivadas do `status` e da situação documental. Histórico: ver nota C |
| 7 | `aluno-participantes.html` | `solicitacoes/participantes/*` | Indicadores já existem em `resumo` |
| 8 | painel, fila, análise | `admin/painel.html`, `admin/solicitacoes/*` | Card “Canceladas”: ver nota D |
| 9 | usuários, horários, bloqueios | `admin/usuarios/*`, `admin/horarios/*`, `admin/bloqueios/*` | Visão da semana usa `painel.horarios` agrupado por `diaSemana` no template |
| 10 | secretaria | `secretaria/documentos/*`, `secretaria/contribuicoes/*` | Lista “O que conferir” é texto fixo, não registro |
| 11 | segurança | `seguranca/reservas/*` | Agrupamento por data feito no template com a lista já ordenada |

## Notas sobre o que exige decisão

- **A. Calendário mensal sem JavaScript.** Cada dia vira um link `GET /quadra?data=AAAA-MM-DD` (já suportado). O ponto “dia com funcionamento” usa só a grade semanal (dado existente). Mostrar “x livres” por dia exige chamar o cálculo atual para cada dia do mês — sem regra nova, mas com mais consultas; decidir se vale a pena.
- **B. Seleção de horário no formulário.** Hoje é um `<select>`. Os blocos podem ser `<input type="radio" name="horarioInicio">` estilizados como `.slot`, funcionando sem JavaScript; a troca de data continua sendo o `GET` atual. A barra fixa do celular é só CSS.
- **C. Histórico da solicitação.** Montado a partir de datas que já existem (`criadaEm`, versões de `DocumentoReserva`, `ConferenciaDocumento.conferidoEm`, `ContribuicaoApm.registradaEm`, `analisadaEm`, `canceladaEm`). Exige expor esses dados ao template do aluno; avaliar se é desejado.
- **D. Contagem de canceladas no painel.** O painel atual não mostra; é um `count` por `StatusReserva.CANCELADA`. Incluir só se o grupo aprovar.
- **E. Fotografia.** Depende de autorização formal.

## Checklist por PR de migração

- [ ] `./mvnw clean verify` passando (testes de controller verificam textos e rotas).
- [ ] Capturas da **aplicação em execução** em 390 e 1440 px.
- [ ] axe-core sem violações nas telas alteradas.
- [ ] Sem novas dependências; sem JavaScript obrigatório para concluir uma ação.
