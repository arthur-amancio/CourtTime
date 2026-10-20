# Relatório de acessibilidade e responsividade — protótipo visual

Verificações executadas em 20/10/2026 sobre o **protótipo visual** (não sobre a aplicação Spring Boot), servido localmente e aberto em Chromium headless (Playwright 1.56).

## O que foi executado

| Verificação | Escopo | Resultado |
| --- | --- | --- |
| Rolagem horizontal indevida (`scrollWidth > clientWidth`) | 25 telas/variações × 4 larguras (390, 430, 768 e 1440 px) = 100 renderizações | **0 ocorrências** na rodada final. Três foram encontradas e corrigidas: botões de controle sem quebra no detalhe da solicitação, email longo na lista de usuários e tabela larga em 768 px (agora vira cartões abaixo de 860 px) |
| Erros de JavaScript no console | 100 renderizações | **0 erros** |
| axe-core 4.10.2 (regras WCAG 2.0/2.1 A e AA) | 25 telas × 390 e 1440 px = 50 execuções | **0 violações** |
| Alvos de toque menores que 32 px | Botões, links-botão, campos, dias e horários | **0 ocorrências**. Padrão 44 px; tamanho pequeno 36 px |
| Fluxo por teclado e interação | Nova solicitação, detalhe, menu móvel | Ver abaixo |

### Fluxo testado automaticamente

- Enviar sem período → mensagem “Selecione um período disponível…” aparece e recebe foco.
- No calendário, setas do teclado movem o foco entre dias (22 → 23/10); Enter carrega os horários do dia.
- Clique em um período disponível → `aria-pressed="true"`, resumo mostra “10:00 – 11:00”.
- Enviar sem finalidade → campo marcado com `aria-invalid` e mensagem “Informe a finalidade.”
- Envio completo → vai para o detalhe com “Solicitação registrada” e situação **Pendente de documentação** (sem aprovação automática).
- Seletor de situação do detalhe alterna corretamente as cinco variações.
- Menu móvel (390 px): abre com `aria-expanded="true"`, move o foco para o primeiro link e fecha com Esc. Fechado, fica `visibility: hidden` e sai da ordem de tabulação.
- Barra fixa do celular mostra a data e o período escolhidos.

## Contraste (WCAG 2.1, mínimo 4,5:1 para texto normal)

| Par | Cores | Razão |
| --- | --- | --- |
| Texto / fundo | #1D2D25 sobre #F4F6F4 | 13,3:1 |
| Texto suave / branco | #526158 sobre #FFFFFF | 6,5:1 |
| Botão primário | #FFFFFF sobre #17633F | 7,3:1 |
| Menu / barra lateral | #DCEBE2 sobre #082A1B | 12,5:1 |
| Badge pendente de documentação | #6F4500 sobre #FDF3DC | 7,5:1 |
| Badge pendente de análise | #1D4E89 sobre #E7F0FB | 7,3:1 |
| Badge aprovada | #1F5F3A sobre #E3F2E8 | 6,6:1 |
| Badge rejeitada | #8B2E2E sobre #FBEAEA | 7,1:1 |
| Badge cancelada | #44514A sobre #ECEEED | 7,1:1 |
| Horário indisponível | #8B2E2E sobre #F8E9E9 | 7,1:1 |
| Contorno de foco | #875C00 sobre #F4F6F4 | 5,4:1 (mínimo 3:1 para elementos não textuais) |
| Botão desativado | #6B7A71 sobre #EBEFEC | 3,9:1 (desativado é isento, mas permanece legível) |

## Decisões de acessibilidade aplicadas

- Situação nunca indicada só por cor: todo badge e horário tem ícone **e** texto.
- Foco visível em todos os elementos interativos (contorno ouro de 3 px, já usado no CSS atual).
- Link “Pular para o conteúdo” em todas as telas.
- Breadcrumb com `aria-current="page"`; menu ativo com `aria-current="page"`; etapa atual com `aria-current="step"`.
- Tabelas com `caption` (oculto visualmente), `scope` nos cabeçalhos e rótulos `data-label` na versão em cartões.
- Mensagens de erro ligadas aos campos, com `aria-invalid` e foco no primeiro erro.
- Região `aria-live` anuncia a data carregada e o período selecionado.
- `prefers-reduced-motion` desativa transições.

## Responsividade

Layouts próprios por faixa, não só redução:

| Largura | Comportamento |
| --- | --- |
| ≥ 1025 px | Barra lateral fixa; calendário mensal ao lado dos horários; resumo da solicitação fixo à direita |
| 861–1024 px | Barra lateral vira menu deslizante com botão “Menu”; conteúdo em coluna única quando necessário |
| ≤ 860 px | Calendário mensal é trocado por faixa de dias com rolagem horizontal e contagem de horários livres; tabelas viram cartões; login empilha |
| ≤ 640 px | Horários em 2 colunas; barra fixa inferior com o período escolhido e botão “Continuar”; botões de ação em largura total |

## Limitações conhecidas

- Testes feitos em Chromium. Safari e Firefox não foram verificados nesta sessão.
- Não houve teste com leitor de tela real (NVDA/VoiceOver), apenas as regras automáticas e a revisão da marcação.
- Nas capturas de página inteira em 390/430 px, a barra fixa inferior da nova solicitação aparece no meio da imagem: é o comportamento `position: sticky` registrado no momento da captura, não uma sobreposição real.
- O botão “Registrar solicitação” usa `aria-disabled` enquanto não há período escolhido; ele continua clicável para explicar o motivo, o que deve ser validado com usuários.
- O protótipo não foi avaliado com usuários reais (alunos, secretaria, segurança).
