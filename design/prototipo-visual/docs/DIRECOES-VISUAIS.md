# Direções visuais do CourtTime

Base de comparação: `main` em `436b262` (Merge do PR #46). O visual atual usa verde `#17633F`/`#114B31`, fundo `#F4F6F4`, fonte do sistema, cabeçalho branco simples, botões grandes e tabelas que viram cartões no celular. Ele é correto e acessível, mas pouco hierarquizado: todas as telas têm o mesmo peso, a disponibilidade é uma tabela de duas colunas e não existe navegação persistente entre as áreas de cada perfil.

## 1. Pesquisa de referências

| Referência | O que observei | O que aproveitar | O que evitar |
| --- | --- | --- | --- |
| Playtomic e Playtomic Manager | Reserva por blocos de horário, agenda do clube, indicadores de ocupação | Grade de horários como blocos clicáveis; agenda do gestor separada da experiência do jogador | Linguagem comercial, métricas de receita e ocupação, pagamento |
| ReserveApp | Agenda semanal com horários e bloqueios; reserva por hora com disponibilidade real | Separar grade semanal de bloqueios pontuais (o CourtTime já faz isso) | Mapa de quadras, preços e categorias |
| Timezer | Visão mensal com estado de cada reserva; bloqueio de horários; menu único para agenda, clientes e configurações | Calendário mensal como seletor de data; status visíveis de relance | Pagamentos, pacotes, lembretes automáticos por WhatsApp |
| QuadraLink | Fluxo em etapas numeradas; quadro de reservas para o operador; página de acompanhamento da reserva | Etapas numeradas e página de situação da solicitação | Confirmação automática, PIX, PDV |
| Sistemas acadêmicos e painéis institucionais | Barra lateral fixa por perfil, breadcrumbs, tabelas densas, alertas explicativos | Navegação persistente e textos que explicam a regra institucional | Excesso de menus e jargão |

Padrões comuns que entraram no protótipo: seleção de data por calendário e por faixa de dias no celular, horários em blocos com legenda, resumo da escolha fixo ao lado (desktop) ou na base da tela (celular), etapas da solicitação, badges com ícone e texto, barra lateral por perfil.

Padrões rejeitados por não existirem no CourtTime: pagamento online, confirmação automática, reagendamento, cálculo de valor, métricas financeiras, lembretes automáticos, avaliação, mapa de quadras.

## 2. As três direções

### A — Institucional Premium
Superfícies claras, verde sóbrio, tipografia firme, área de imagem da escola com tamanho controlado e grade rigorosa.
- **Pontos fortes:** credibilidade, coerência com a ETEC e com o `AGENTS.md` (sem aparência de SaaS).
- **Limites:** sozinha, não resolve a tarefa principal do aluno, que é achar um horário livre rápido.

### B — Sport Booking
Calendário em destaque, horários em blocos grandes, ações evidentes, foco no celular.
- **Pontos fortes:** o aluno entende a disponibilidade em segundos; ótimo alvo de toque.
- **Limites:** tende a parecer aplicativo comercial de reserva e sugere confirmação imediata, o que contraria o fluxo institucional.

### C — Dashboard Contemporâneo
Barra lateral, indicadores, filas e tabelas refinadas.
- **Pontos fortes:** produtividade para administrador e secretaria; navegação estável.
- **Limites:** fria para a página pública e para o aluno; convida a inventar métricas.

## 3. Protótipo do Lovable (direção D)

Analisado em 20/10/2026 em `https://screen-inspired-scene-builder.lovable.app/`, pelo navegador, nas rotas `/`, `/entrar`, `/inicio`, `/quadra`, `/solicitacoes/nova` e `/solicitacoes/r3` (perfis aluno e administrador). As rotas administrativas `/admin` e `/admin/horarios` retornaram 404 no momento da análise. Capturas de referência em `referencias/lovable/` (sem a fotografia da página inicial; veja abaixo).

**Pontos positivos:** login dividido (painel institucional + formulário), barra lateral verde escura por perfil, grade de horários em blocos, etapas da solicitação com 7 passos, legenda Disponível/Ocupado/Bloqueado, texto “sem pagamento online”.

**Divergências em relação ao CourtTime real:**

| No Lovable | No CourtTime real | Tratamento no protótipo |
| --- | --- | --- |
| Card “Próxima reserva — Futsal · 3º DS” na página pública | A página pública não expõe reservas; a segurança só vê reservas autorizadas após login | Removido |
| Fotografia de quadra coberta sem origem identificada | Não há foto autorizada da ETEC | Não usada; espaço reservado com ilustração própria |
| Fontes Manrope e Bricolage Grotesque (externas) | Fonte do sistema | Mantida a fonte do sistema |
| Estado “Ocupado” | `SituacaoHorario`: Disponível, Reservado, Indisponível | Usados os termos reais |
| “A direção analisa e aprova” | Quem decide é o perfil **administrador** | Texto ajustado |
| Código “CT-2026-016” | Identificador numérico (`Solicitação #16`) | Usado `#16` |
| Status “Pendente documentação” / “Em conferência” | “Pendente de documentação”, “Documento aguardando conferência” | Termos do `SituacaoSolicitacaoService` |
| Etapa “Liberada à Segurança” | Não é um estado; a reserva aprovada passa a aparecer para a segurança | Explicado em texto, não como etapa |
| Alternância de tema escuro | Não existe | Não incluída |
| Atalhos “Entrar como” sem aviso forte | Não existe login por perfil | Mantidos só como “Somente no protótipo” |
| Código React + Tailwind | Thymeleaf + CSS próprio, sem JavaScript | Nada importado; layout reconstruído em HTML/CSS |

**Decisão: adotar partes do Lovable.** Foram reaproveitadas as ideias de login dividido, barra lateral verde escura, blocos de horário e etapas da solicitação. Foram descartados a fotografia, as fontes externas, o card público de reserva, os termos inexistentes e todo o código.

## 4. Direção escolhida

**Identidade única: A + B nas telas do aluno e na página pública; A + C nas áreas de secretaria, administração e segurança.**

Por quê:
1. A base A mantém o verde atual e o tom institucional pedido no `AGENTS.md`.
2. B entra só onde gera ganho real: calendário, blocos de uma hora, resumo da escolha e barra de ação fixa no celular.
3. C organiza filas e cadastros com a mesma barra lateral para os quatro perfis, sem métricas inventadas.
4. Um único arquivo de tokens e componentes serve tudo, o que facilita a migração para Thymeleaf.

Esta é a recomendação do protótipo. **Ela não está aprovada** e depende da revisão externa e da decisão do grupo.
