# CourtTime — redesign dos 15 slides

Revisão visual de 05/11/2026, derivada do PR #95 (`faa1c8c`). Escopo exclusivamente documental: apresentação, notas e prancha. Nenhuma alteração em Java, banco, regras, monografia, Render, Neon ou PR #94; nenhum merge ou deploy.

## Entregas verificadas

- [PowerPoint editável — 15 slides](https://drive.google.com/file/d/17cjUL-nYFUOsk4RPHyikH9VrfF1eXUdF/view).
- [PDF correspondente — 15 páginas](https://drive.google.com/file/d/19exVG9ocDdUw2bIm4lLNVblMnsNaaE4Q/view).
- [Prancha vetorial com os 15 slides](https://drive.google.com/file/d/1M1BTW5WsnoRhRQyDRJbHkqDkMAsf7ILH/view).

PPTX/PDF atualizados nos IDs principais. A prancha foi colocada na mesma pasta final, sem criar pastas. Antes da substituição, foi criado um ZIP de recuperação na raiz privada do proprietário; metadados confirmaram `shared: false` e somente a permissão do proprietário. O histórico dos dois arquivos também preserva a revisão anterior. Nenhuma permissão foi alterada. Arquivos acadêmicos e capturas privadas não foram adicionados a este repositório.

## Auditoria e resultado visual

O PowerPoint original foi aberto no LibreOffice Impress, exportado para PDF e renderizado em 15 imagens de 1440 × 810. Todas foram inspecionadas individualmente antes da edição; a auditoria não se limitou ao XML. A versão redesenhada foi aberta pelo mesmo software, exportada e renderizada em aproximadamente 1920 × 1080 (1921 pixels de largura por arredondamento). Todos os slides foram inspecionados individualmente. Um passe de correção removeu sombras herdadas, sobreposição de legendas, quebra do link, conector sobre texto e avanço da conclusão ao rodapé; uma nova inspeção integral confirmou as correções.

| Slide | Problema anterior | Solução entregue e conferida |
| --- | --- | --- |
| 1 | Créditos e subtítulo sem hierarquia | Logo original, composição em duas áreas e créditos legíveis. |
| 2 | Lista genérica e captura pequena | Três perguntas do processo e consulta real ampliada. |
| 3 | Repetição da composição anterior | Informação, responsabilidade e acesso em três colunas. |
| 4 | Linhas soltas sem estrutura | Quatro objetivos numerados, distribuídos em grade. |
| 5 | Percentuais sem gráfico e metadados concorrentes | Três barras nativas proporcionais, contagem/base 28 e limite exploratório. |
| 6 | Papéis sem ligação visual | Fluxo de quatro perfis; aprovação administrativa explícita. |
| 7 | Captura pequena e aprovação pouco visível | Rascunho, mínimo, PDF e aprovação; divergência institucional destacada. |
| 8 | Lista de tecnologias sem responsabilidades | Interface, aplicação, dados/PDF e qualidade; hospedagem demonstrativa identificada. |
| 9 | Caixas pequenas e fluxo incompleto | Navegador → Controller → Service → Repository → PostgreSQL; retorno Thymeleaf e PDFs privados. |
| 10 | Atributos densos e conectores cortados | Recorte fiel do DER oficial com três entidades, poucos atributos e cardinalidades. |
| 11 | Uma captura estreita | Dois recortes reais ampliados, com legendas separadas. |
| 12 | Tabela financeira competindo com a demonstração | URL clicável, quatro momentos e duração proposta de 240 segundos. |
| 13 | Tabela com hierarquia fraca | Três métricas, suas unidades, versões e testes ignorados explícitos. |
| 14 | Quantidade de páginas como resultado central | Funcionalidades implementadas e homologação institucional separadas. |
| 15 | Encerramento genérico | Logo original, agradecimento discreto e perguntas. |

Sistema visual: formato 16:9, margem principal de 0,65 polegada, títulos de 32 pt, corpo predominantemente de 18–24 pt, Arial no PPTX. Azul-marinho `#0A2136`, teal `#087E70`, branco e cinza claro; mint `#43D4B1` nos fundos escuros. Fontes menores ficam em legendas, versões, fontes e rodapés. O PDF incorpora Liberation Sans, substituta métrica de Arial disponível no ambiente de exportação. Não foram usadas imagens de banco, interfaces inventadas, gráficos 3D ou fontes raras.

O PPTX contém 231 caixas de texto nativas, três gráficos com planilhas internas e 15 notas. Arquitetura e modelagem usam formas e conectores editáveis. Os slides não foram achatados em imagens. Logo e capturas reais preservam suas proporções; os recortes são reversíveis no PowerPoint.

## Conteúdo e roteiro

Pesquisa: 26/28 (92,9%) para interesse na consulta; 25/28 (89,3%) para utilidade da solicitação; 24/28 (85,7%) para celular. As barras usam contagem observada e complemento até 28. Fonte: seção 3.2 e Apêndice B da monografia Pesquisa Integrada V2. São registros exploratórios, sem pressupor unicidade, representatividade ou teste de usabilidade.

Testes: 515 Java executados no runtime `3cdfccc`, zero falhas/erros, três opcionais S3 ignorados; 1.128 verificações de navegador e 44 varreduras axe na revisão `85ebf93`. Não somar unidades nem atribuir todos os resultados ao mesmo HEAD. A monografia registra 479 Java da referência anterior `b842f35`; esse texto permaneceu intacto. Os 515 provêm da atualização de runtime documentada no PR #94 e no material oficial do PR #95. Esta missão não reexecutou testes de software.

| Integrante | Exposição | Tempo proposto | Apoio na demo |
| --- | --- | --- | --- |
| Arthur | Slides 1–5 | 120 s | Aproximadamente 60 s |
| Carlos | Slides 6–9 | 120 s | Aproximadamente 60 s |
| Gustavo | Slides 10–12 | 120 s | Aproximadamente 60 s |
| Heitor | Slides 13–15 | 120 s | Aproximadamente 60 s |

Total: oito minutos de exposição e quatro de demonstração, cerca de três por integrante. Notas contêm falas naturais, fontes e apoio, sem transformar falas em texto projetado. É planejamento, sujeito ao professor e ao ensaio humano. As transições de perfil prevalecem sobre a troca exata de operador.

Demo: 0:00–1:10 Aluno; 1:10–2:00 Secretaria; 2:00–2:35 Administração; 2:35–4:00 Segurança. A portaria usa outra reserva, preparada até o dia anterior para a data real de uso. A reserva pública #2 vale somente para 05/11/2026, 09h–10h, America/Sao_Paulo. Para outra data, nova reserva pelo fluxo normal; sem editar banco, datas ou infraestrutura. Documentos, conferência e contribuição são simulados na base demonstrativa. Contingência: capturas identificadas e PDF local.

Pendências institucionais permanecem explícitas nas notas: dez alunos versus dez participantes, vínculos de OUTRO, horário-limite documental, crédito excedente, retenção e compatibilização do termo. A assinatura e sua conferência criptográfica são externas; o software registra a conferência humana.

## Conferências finais

- 15 slides no PPTX, 15 páginas no PDF e 15 notas de apresentador.
- Todos os textos nativos do PPTX encontrados no PDF, sem omissões.
- Logo e capturas com proporção preservada; nenhuma imagem única substitui um slide inteiro.
- Link da demonstração presente e clicável no PDF e no PPTX.
- Arquivos publicados novamente baixados do Drive e comparados byte a byte e por SHA-256.
- Pasta, IDs principais e permissões preservados; backup privado e histórico de versões conferidos.
- Monografia sem escrita, com tamanho e data de modificação inalterados no Drive.
- Abertura/exportação validadas em LibreOffice Impress. Não houve teste no Microsoft PowerPoint do computador da ETEC nem ensaio com projetor físico; esses permanecem a cargo do grupo.
- Não foram executados Maven/Playwright: esta é uma mudança documental. `git diff --check` é a validação do registro no repositório.

| Artefato | SHA-256 dos bytes publicados e baixados |
| --- | --- |
| PPTX | `ccc48f4676f5cbcb8b20ce5f5944d947e2bcf6b1d195a0c8649d80f158d880ff` |
| PDF | `aecd4ff354c4d122ba6261902e9f29af48ec2b116b495cf9433284ec6b8faf6e` |
| Prancha PDF | `c359b9b34e1ed11b55269c9a7f16916e381978ad3289780eb81288090dda7412` |

Monografia consultada, 98 páginas: SHA-256 `fb2b13bc8bacf50a66d441fb123e9c1412b1b7f159f7888445fd9561513e40e9`; nenhuma substituição realizada.
