# Relatório de acessibilidade e responsividade — protótipo visual (V2)

Todas as verificações foram feitas sobre o **protótipo visual**, servido localmente e aberto em Chromium headless (Playwright 1.56, idioma `pt-BR`). Elas **não** testam a aplicação Spring Boot.

Última execução: 20/10/2026, sobre o código da V2. Resultado bruto dos testes de teclado e zoom: `capturas/v2/testes-teclado-zoom.json`.

## Resumo

| Verificação | Escopo | Resultado V2 |
| --- | --- | --- |
| Rolagem horizontal indevida | 34 telas e variações × 390, 430, 768 e 1440 px = 136 renderizações | **0** |
| Erros de JavaScript no console | 136 renderizações | **0** |
| axe-core 4.10.2 (WCAG 2.0/2.1 A e AA) | 34 telas e variações × 390 e 1440 px = 68 execuções | **0 violações** |
| Alvos de toque < 32 px | Botões, links-botão, campos, dias, horários | **0** (o `input type="file"` fica visualmente oculto de propósito; o alvo é o botão “Selecionar PDF”, com 36 px) |
| Teclado, menu, zoom, envio de PDF e calendário | Roteiro automatizado abaixo | **52 de 52 verificações OK** |

As 34 telas e variações incluem as 25 da V1, as duas variantes do calendário e as oito situações do detalhe da solicitação.

## Menu móvel (390, 430 e 768 px)

Em cada largura, todos os itens abaixo passaram:

- O botão **Menu** abre com Enter, marca `aria-expanded="true"` e leva o foco ao primeiro link do menu.
- Em 14 Tab e 14 Shift+Tab, o foco não saiu do menu.
- O conteúdo de fundo, a faixa do protótipo e o link “Pular” ficam com `inert`, e a rolagem da página fica bloqueada.
- **Esc** fecha o menu, volta `aria-expanded` para `false`, remove o `inert` e devolve o foco ao botão Menu.
- O botão **Fechar** (×) também fecha e devolve o foco.
- Escolher uma opção fecha o menu e navega.
- Fechado, o menu fica com `visibility: hidden` e não entra na ordem de tabulação.
- Ao passar de 1024 px, o menu fecha sozinho (implementado; não coberto pelo roteiro automático).

Correção feita durante os testes: a primeira versão perdia o foco ao abrir por teclado, porque a transição de `visibility` deixava o menu invisível no instante do `focus()`. A transição foi ajustada e o teste passou.

## Nova solicitação — larguras, zoom e barra fixa

| Cenário | Sem rolagem horizontal | Horário escolhido pelo teclado | Barra não cobre Finalidade, Registrar e Cancelar quando focados |
| --- | --- | --- | --- |
| 390 × 844 | OK | OK | OK |
| 430 × 932 | OK | OK | OK |
| 768 × 1024 | OK | OK | OK (barra só existe até 640 px) |
| 1440 × 900 | OK | OK | OK |
| Zoom 200% (1440 → 720 × 450 CSS px) | OK | OK | OK |
| Zoom 400% (1440 → 360 × 225 CSS px) | OK | OK | OK |
| Teclado virtual simulado (390 × 420, finalidade focada) | — | — | OK: a barra deixa de ser fixa |

Mudanças que garantem esses resultados:

- A barra de resumo **some** (`visibility: hidden`) quando o painel “Finalidade e envio” entra na tela. Por isso ela nunca fica sobre o campo nem sobre os botões.
- `scroll-padding-bottom` reserva espaço para a barra quando um campo recebe foco.
- Com até 560 px de altura útil (zoom alto, celular deitado ou teclado virtual aberto), a barra e o topo móvel deixam de ser fixos.

O teclado virtual foi **simulado** reduzindo a altura da janela. Não foi testado em celular físico.

## Envio de PDF

| Teste | Resultado |
| --- | --- |
| Enviar sem arquivo | Mensagem “Selecione um arquivo PDF antes de enviar.” e foco no seletor |
| Arquivo `.txt` | “Selecione um arquivo no formato PDF (.pdf).” e `aria-invalid="true"` |
| PDF com mais de 10 MB | “O arquivo PDF deve possuir no máximo 10 MB.” (mesmo texto e limite do `DocumentoReservaService`) |
| PDF válido | Mostra o nome, o tamanho e “selecionado, ainda não enviado”; o botão passa a “Trocar arquivo” |
| Remover | Limpa a seleção e devolve o foco ao seletor |
| Teclado | O `input type="file"` nativo é alcançado por Tab; o botão “Selecionar PDF” mostra o contorno de foco |
| Nome acessível | “Arquivo PDF assinado Selecionar PDF” |

O input nativo continua no HTML, focável e funcional. O texto em inglês do Chromium (“Choose File”) não aparece mais. Nenhum arquivo é enviado ou guardado, e nenhuma assinatura é “validada”.

## Calendário

| Teste | Resultado |
| --- | --- |
| Faixa de dias (390 px): setas ← → e Enter | OK |
| Calendário mensal (1440 px): seta ↓ pula uma semana | OK |
| Consulta: nenhuma data a partir de hoje desabilitada | OK |
| Nova solicitação: hoje desabilitado (antecedência de 1 dia) | OK |
| Dia inteiro bloqueado mostra períodos Indisponíveis; domingo mostra “Sem funcionamento” | OK |
| Quadra inativa: nenhum período selecionável | OK |

## Contraste (WCAG 2.1, mínimo 4,5:1 para texto)

| Par | Razão |
| --- | --- |
| Texto #1D2D25 / fundo #F4F6F4 | 13,3:1 |
| Texto suave #526158 / branco | 6,5:1 |
| Botão primário: branco / #17633F | 7,3:1 |
| Menu #DCEBE2 / barra lateral #082A1B | 12,5:1 |
| Badges: documentação 7,5:1 · análise 7,3:1 · aprovada 6,6:1 · rejeitada 7,1:1 · cancelada 7,1:1 | ≥ 6,6:1 |
| Marca “Depende de adaptação” #5A3D00 / #FFFCF4 | > 9:1 |
| Contorno de foco #875C00 / fundo | 5,4:1 (mínimo 3:1 para elementos não textuais) |

## Pendências e limites

- Testado só em Chromium. Safari (iOS) e Firefox ficaram sem teste.
- Sem leitor de tela real (NVDA, VoiceOver, TalkBack). Foram verificadas as regras automáticas e a marcação.
- Teclado virtual simulado por redução de altura, não em aparelho físico.
- O zoom foi emulado por viewport e escala do dispositivo, que é o comportamento de reflow do navegador, e não pelo zoom da interface do Chrome.
- Sem teste com usuários (alunos, secretaria, segurança).
- O botão “Registrar solicitação” usa `aria-disabled` enquanto nenhum período foi escolhido. Ele continua clicável para explicar o motivo; convém validar com usuários.
- Nas capturas de página inteira em celular, a barra fixa aparece no meio da imagem: é a posição `sticky` no momento da captura, não sobreposição.
