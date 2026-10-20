# Recursos visuais — fontes e autorização

Pesquisa feita em 20/10/2026. **Nenhuma fotografia externa foi incorporada ao protótipo.** Não há hotlinking: a página inicial usa uma composição gráfica própria (SVG inline) identificada como “Espaço reservado para a fotografia oficial”.

## Fotografias avaliadas

| # | Fonte | Arquivo | O que mostra | Qualidade | Pessoas identificáveis | Licença / autorização | Decisão |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Página da ETEC no Centro Paula Souza — https://www.cps.sp.gov.br/etecs/etec-prof-armando-jose-farinazzo/ | `DJI_0001-scaled.jpg` (2560×1920, foto aérea de drone) | Prédio da ETEC visto do alto, com a fachada identificada (“Etec Prof. Armando José Farinazzo”), rua e bairro | Boa resolução, 4:3, bem enquadrada | Não (carros e rua; sem pessoas reconhecíveis) | Site com “© Centro Paula Souza — Todos os direitos reservados”; sem licença de reuso para imagens | **Melhor candidata.** Usar somente após autorização escrita da ETEC/CPS. Até lá, apenas referência |
| 2 | Notícia “Etec realiza recepção e integração dos novos discentes” (2024) — https://etecfernandopolis.com.br/site/noticia/2024/etecrealiza-recepcao-e-integracao-dos-novos-discentes/ | Imagem principal da notícia (1600×1200) | Quadra coberta da ETEC durante evento, com arquibancada cheia | Boa | **Sim — centenas de alunos, muitos menores** | Rodapé apenas “Copyright © 2026”, sem licença | **Não usar no produto.** Serve só como referência da estrutura da quadra (cobertura metálica, piso verde e vermelho) |
| 3 | Notícias de interclasse (2023 e 2026) e assembleia da APM (2024), etecfernandopolis.com.br | Não baixadas | Eventos na quadra | — | Provável presença de alunos | Sem licença | Não usadas; mesma regra do item 2 |
| 4 | Protótipo do Lovable | Foto de quadra coberta | Origem não identificada; não há evidência de que seja a quadra da ETEC | — | Não | Desconhecida (possível banco de imagens ou IA) | **Descartada** — não pode ser apresentada como foto da escola |

## Como incluir a foto oficial depois

1. Pedir à direção da ETEC (ou ao CPS) autorização por escrito para uso da foto 1 no CourtTime, ou uma foto própria da quadra **sem alunos identificáveis**.
2. Salvar o arquivo autorizado em `src/main/resources/static/img/` (nunca usar link externo).
3. Trocar o SVG de `.foto-reservada` por `<img src="…" alt="Fachada da ETEC Prof. Armando José Farinazzo" width height loading="lazy">`, mantendo `aspect-ratio` para não distorcer.
4. Registrar aqui: arquivo, autor, data da autorização e quem autorizou.

## Outros elementos visuais

- **Marca CourtTime:** símbolo próprio (planta simplificada de quadra em SVG). Não usa logotipo da ETEC nem do Centro Paula Souza.
- **Ícones:** desenhados para o protótipo em SVG inline (traço 24 px), sem biblioteca externa.
- **Fontes:** fonte do sistema operacional; nenhuma fonte externa carregada.
- **Nomes de pessoas, RMs e RGs:** fictícios. RGs aparecem como `00.000.000-0` de propósito.
