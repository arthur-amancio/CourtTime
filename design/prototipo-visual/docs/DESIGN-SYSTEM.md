# Design system do CourtTime (protótipo)

Fonte única: `assets/css/courttime.css`. Página demonstrativa: `design-system.html`.

## Princípios

1. **Institucional antes de comercial.** Verde da ETEC, superfícies claras, nada de gradientes, vidro ou sombras pesadas.
2. **A situação é sempre explícita.** Cor + ícone + texto, com os termos reais do sistema.
3. **A regra aparece onde a decisão acontece.** Ex.: “O envio não autoriza o uso” ao lado do botão de enviar.
4. **Uma identidade para quatro perfis.** Mesma barra lateral, mesmos componentes; muda só o conteúdo.

## Cores

| Token | Valor | Uso |
| --- | --- | --- |
| `--verde-600` | #17633F | primária (preservada do sistema atual) |
| `--verde-700` | #114B31 | hover, primária escura (preservada) |
| `--verde-900` | #082A1B | barra lateral |
| `--verde-50` / `--verde-100` / `--verde-200` | #EEF6F1 / #DCECE2 / #B7D8C3 | fundos suaves, bordas de horário livre |
| `--ouro-700` | #875C00 | foco (preservado) e faixa do protótipo |
| `--cinza-50` | #F4F6F4 | fundo da página (preservado) |
| `--cinza-900` | #1D2D25 | texto (preservado) |
| `--cinza-600` | #526158 | texto secundário (preservado) |
| `--cinza-200` / `--cinza-300` | #D9E1DC / #C3CEC7 | bordas |

Semânticas (texto / fundo / borda):

| Situação | Token | Cores |
| --- | --- | --- |
| Pendente de documentação, aviso | `--aviso-*` | #6F4500 / #FDF3DC / #EBCF8F |
| Pendente de análise, informação | `--info-*` | #1D4E89 / #E7F0FB / #B3CBEA |
| Aprovada, sucesso | `--sucesso-*` | #1F5F3A / #E3F2E8 / #A9D3B8 |
| Rejeitada, erro, ação perigosa | `--perigo-*` | #8B2E2E / #FBEAEA / #E4B5B5 |
| Cancelada, inativo | `--neutro-*` | #44514A / #ECEEED / #CFD5D1 |

## Tipografia

Fonte do sistema (`system-ui, -apple-system, "Segoe UI", Roboto…`), a mesma da aplicação atual. Sem fontes externas.

| Token | Tamanho | Uso |
| --- | --- | --- |
| `--texto-4xl` | 32–42 px, 800 | título da página pública |
| `--texto-3xl` | 30 px, 800 | `h1` das telas |
| `--texto-xl` / `--texto-lg` | 20 / 18 px | `h2`, `h3`, data selecionada |
| `--texto-md` | 16 px, linha 1,55 | texto |
| `--texto-sm` / `--texto-xs` | 14 / 12 px | apoio, rótulos de tabela, sobretítulos |

Horários e datas usam `font-variant-numeric: tabular-nums` para alinhar colunas.

## Espaçamento, raios e larguras

- Escala de 4 px: `--esp-1` (4) a `--esp-16` (64).
- Raios: 6 px (controles), 10 px (cartões internos, horários), 14 px (painéis).
- Larguras: 1200 px (público), 1180 px (área autenticada), 46 rem (texto), barra lateral 264 px.
- Alvo de toque: 44 px padrão, 36 px no tamanho pequeno.

## Componentes

| Componente | Classes | Observações |
| --- | --- | --- |
| Botões | `.botao`, `--secundario`, `--fantasma`, `--perigo`, `--perigo-contorno`, `--sm`, `--lg`, `--bloco`, `.is-loading`, `[disabled]` | ação perigosa nunca é o botão principal |
| Campos | `.campo`, `.campo__ajuda`, `.campo__erro`, `.campo--erro`, `.campo__contador`, `.campos-linha`, `.arquivo` | rótulo sempre visível; erro ligado ao campo |
| Alertas | `.alerta` + `--sucesso`, `--info`, `--aviso`, `--perigo`, `--neutro` | ícone + título + texto |
| Badges | `.badge--documentacao`, `--analise`, `--aprovada`, `--rejeitada`, `--cancelada`, `--neutro` | um badge por situação real |
| Painel | `.painel`, `__cabecalho`, `__corpo`, `__rodape` | só para agrupamento semântico |
| Indicadores | `.indicadores`, `.indicador`, `--destaque` | apenas contagens que o sistema já calcula |
| Tabelas | `.tabela-wrapper`, `.tabela`, `.tabela--cartoes`, `data-label` | viram cartões abaixo de 860 px |
| Navegação | `.lateral`, `.topo-movel`, `.breadcrumb`, `.abas` | barra lateral por perfil |
| Calendário | `.calendario`, `.dia`, `.dia--funciona`, `.faixa-dias`, `.dia-chip` | mês no desktop, faixa de dias no celular |
| Horários | `.slots`, `.slot--disponivel`, `--reservado`, `--indisponivel`, `[aria-pressed]` | termos de `SituacaoHorario` |
| Etapas | `.etapas`, `.etapa--concluida`, `--atual`, `--interrompida`, `--neutra` | verticais no celular |
| Histórico | `.historico` | eventos com data e responsável |
| Estado vazio | `.vazio` | explica por que está vazio e o próximo passo |
| Cabeçalho e rodapé | `.topo-publico`, `.rodape` | |

## Estados

- **Hover:** fundo verde suave ou escurecimento da primária.
- **Focus:** contorno ouro de 3 px com afastamento de 2 px.
- **Disabled:** cinza com cursor “não permitido”; horários indisponíveis usam `aria-disabled` e listras.
- **Loading:** `.is-loading` mostra um indicador giratório e `aria-busy`.
- **Sucesso/erro:** alertas `role="status"` e `role="alert"`.
