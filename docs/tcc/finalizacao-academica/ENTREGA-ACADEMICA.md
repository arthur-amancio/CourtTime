# CourtTime — candidato acadêmico para revisão

29/10/2026. Continuação do checkpoint anterior; escopo exclusivamente acadêmico. A referência funcional permanece `4fb7a06`, incorporada em `2e7c07f` antes desta revisão. Nenhum arquivo de runtime, banco ou teste de domínio foi alterado.

**Classificação: pronto para revisão do orientador.** O PDF é uma versão candidata, ainda sujeita à aprovação acadêmica e às confirmações institucionais abaixo. Não constitui aprovação da banca, homologação da ETEC nem versão definitiva para impressão.

## Entregas e preservação

- [PDF A4 paginado](CourtTime-candidato.pdf): 82 páginas, 33 figuras, sete quadros e 24 referências. A contagem começa na folha de rosto; os números são impressos a partir da introdução, na folha física 16, com número 15.
- [O mesmo PDF no Drive](https://drive.google.com/file/d/17kUeBkY0cUHn0T6VEYYijNXZBmV95Lwz/view): download posterior conferido por SHA-256, idêntico ao arquivo versionado. A prova intermediária de 84 páginas foi identificada como arquivo, para evitar confusão com o candidato.
- [Inventário e explicações das 33 figuras](INVENTARIO-FIGURAS.md), [catálogo com hashes](catalogo-figuras.json), [bibliografia e pesquisa](BIBLIOGRAFIA-E-PESQUISA.md) e [prova completa de impressão](AUDITORIA-A4.md).
- [Documento de trabalho atualizado](https://docs.google.com/document/d/1NwFW5kRauj1vNHkWRuP1Kxbgm4eg82ez7Uwos67mGNU/edit).
- [Candidato editável congelado para revisão](https://docs.google.com/document/d/1h5l4LAgdLetneb-vtNf-KGEiELmNRACUobQzdhOHvC0/edit).
- [Arquivo anterior com duas abas e 57 imagens preservadas](https://docs.google.com/document/d/1uOYDs8srmcJuJanmZwvMb68J7i1huWvpC48SyG7U5Kk/edit). A aba histórica continha 26 imagens; a principal continha 31. A cópia atual possui somente a versão principal, com 33 figuras.
- [Pasta privada de entrega](https://drive.google.com/drive/folders/14oKiQNnPwLuvLQ4XwPkcfWcGDDlPX5Dq). Permissões verificadas nos arquivos novos; não foram modificadas permissões preexistentes. O consolidado oficial permaneceu intacto.

Os números do sumário e das listas são materializados para esta exportação. A numeração do rodapé foi aplicada no PDF por `paginar.py`; não é um campo automático inserido no Google Docs. Uma futura edição exige nova exportação, cálculo das listas e conferência do PDF. O PDF versionado é a referência desta prova.

## Revisão dos capítulos

| Parte | Correção efetiva | Evidência/limite |
|---|---|---|
| Pré-textuais | Capa e rosto, folha de aprovação sem examinadores inventados, dedicatória e agradecimentos preservados, epígrafe com fonte secundária explícita, resumo/abstract e palavras-chave | Folhas físicas 1–15; sumário e listas com páginas reais |
| I — Fundamentação | Fontes primárias para conceitos, estudos relacionados e comparação exploratória de produtos; distinção entre autenticação e comprovação de identidade conforme NIST | Nenhuma alegação de teste independente dos produtos comerciais; livros citados em nível conceitual, sem páginas inventadas |
| II — Identidade visual | Preservação das seis marcas; adequação ao uso institucional; briefing sem monetização fictícia | Logotipos e paleta existentes; autoria de ferramenta gráfica específica não presumida |
| III — Levantamento | Conferência da planilha primária: 28 respostas; contagens agregadas e limitações explícitas | Não se comprovam 28 pessoas únicas ou amostra representativa; conversa informal não é entrevista transcrita |
| IV — Requisitos | Rascunho separado da solicitação formal; ALUNO + OUTRO; presença, adicionais, repasse e limites | RF01–RF18, RNF01–RNF06, casos de uso e mensagens; termo original preservado |
| V — Análise | Duas vistas de classes e duas do DER reconciliadas com `rascunho`; dicionário atualizado; explicação de responsabilidades sem duplicar versões antigas | SVG/PNG editáveis e gerador; não houve alteração do arquivo nativo Astah |
| VI — Telas | Catorze recortes móveis de capturas reais da execução anterior, com dados fictícios, legendas e fontes | Não são novos testes do software; o inventário identifica arquivo, recorte e versão |
| VII — Tecnologias | Versões conferidas no repositório; testes vinculados a data, commit e ambiente; ausência de pagamento online e validação criptográfica explícitas | Resultados anteriores preservados como históricos verificáveis, sem afirmar nova execução |
| Considerações | Resultados observados separados de implantação/homologação; limitações e revisão final do orientador explícitas | Não se afirma aceitação institucional |
| Referências | 24 entradas normalizadas, títulos destacados, referências e citações relacionadas | Metadados parciais identificados no relatório bibliográfico |

## Checklist do manual institucional

Referência principal: [Manual ETEC](https://drive.google.com/file/d/1gTywCwLM8aKDV47bcC60M8l03YXWlqKp/view). Os números abaixo referem-se às folhas físicas do PDF do manual. Aplicou-se a seção de **monografia**, não as instruções diferentes de artigo iniciadas mais adiante. A orientação posterior do professor prevalece quando formalmente fornecida.

| Exigência/localização | Situação no CourtTime | Evidência/correção | Pendência |
|---|---|---|---|
| Estrutura de monografia — p. 3 | Atendida no candidato | Pré-textuais, introdução, sete capítulos, considerações e referências | Aprovação acadêmica final |
| Margens de 2,5 cm; cabeçalho/rodapé 1,25 cm — p. 2 | Aplicadas | Estilo nativo e auditoria geométrica A4 | Prova física recomendada |
| Arial 12, texto justificado, recuo 2,5 cm e entrelinha 1,5 — p. 2 | Aplicados no corpo | Estilos conferidos; fontes incorporadas no PDF | Nenhuma divergência bloqueadora identificada |
| Capa e títulos Arial 14; autores em ordem alfabética | Atendidos | Folhas 1–3; cidade/ano mantidos na mesma página | Confirmar nomenclatura oficial da habilitação na revisão do orientador |
| Folha de aprovação | Estrutura preservada | Linhas para examinadores; nenhuma assinatura ou aprovação simulada | **P1:** nomes, data e assinaturas somente após informação oficial |
| Dedicatória, agradecimentos e epígrafe opcionais | Preservados e formatados | Blocos inferiores, recuo de 8 cm; fonte da epígrafe explicitada | Autores podem revisar seu texto pessoal |
| Resumo de 150–500 palavras, parágrafo único — p. 10 | Atendido | Objetivo, método, resultado e limites; sem dados inventados | Avaliação linguística final pelo orientador |
| Palavras-chave em minúsculas, separadas por ponto e vírgula — p. 10 | Atendidas | Cinco termos em português e cinco em inglês | Nenhuma |
| Abstract correspondente — p. 11 | Presente | Mesma abrangência e mesmos limites do resumo | Revisão humana de inglês recomendada |
| Listas próprias quando pertinentes | Presentes | 33 figuras, sete quadros e 15 siglas | Não há tabela quantitativa ou gráfico a listar |
| Sumário com introdução, capítulos e pós-textuais — p. 20 | Presente | 63 títulos localizados nas páginas efetivas | Recalcular após qualquer edição |
| Contar desde o rosto e numerar da introdução, canto inferior direito — p. 20 | Atendido no PDF | 67 números, de 15 a 81, nas 82 folhas físicas | Campo de página não automatizado no Docs |
| Seções primárias em maiúsculas e títulos em negrito | Aplicados | Hierarquia preservada, sete capítulos identificados | Revisão do orientador pode ajustar o modelo de numeração |
| Introdução com contexto, problema, objetivos e hipótese — seção Introdução | Contexto, pergunta e objetivos presentes | Conteúdo original preservado e redação atualizada | **P1:** orientador confirmar se a pergunta e a premissa técnica atendem à formulação de hipótese exigida; não se atribuiu à equipe uma pesquisa hipotético-dedutiva não documentada |
| Requisitos e modelagem coerentes | Reconciliados | `rascunho` nas classes, DER e dicionário; mínimo de dez totais explícito | **P1:** decisão institucional sobre o termo “dez alunos” |
| Figura com número, título e fonte | Atendidas | 33 legendas e 33 arquivos correspondentes; títulos 12 e fontes 10 | Originais e recortes identificados |
| Quadros com identificação e fonte | Atendidos | Sete quadros; cabeçalhos repetidos e linhas protegidas | Continuações são intencionais; eventual refinamento de colunas é P2 |
| Citações sustentadas por fontes existentes | Conferidas no escopo declarado | Matriz citação–referência e fontes primárias no relatório bibliográfico | Limites de acesso integral e metadados incompletos explicitados |
| Referências em margem única à esquerda, espaço simples e destaque uniforme — p. 60 | Aplicadas | 24 entradas; títulos em negrito; autores >3 com et al.; DOI somente quando confirmado | **P1:** completar “Romário” caso os autores obtenham identificação autoral mais completa |
| Elementos opcionais como glossário/apêndices | Não incluídos sem necessidade | Siglas listadas; histórico conservado em arquivo separado | Glossário é **P2**, não requisito presumido do Draw Code |
| Impressão frente e verso — p. 2 | PDF preparado | Todas as folhas A4 | Configuração de impressão e aprovação física cabem à equipe |

## Draw Code × CourtTime após a revisão

O [Draw Code](https://drive.google.com/file/d/1v6WvjKwL0_DxJN_5IONjLzs65J4ppIRX/view) foi consultado como referência de apresentação e organização. Foram aproveitados critérios de hierarquia e exposição, sem copiar seu texto, pesquisa ou imagens. Seu PDF tem 90 folhas; a quantidade de páginas não é meta do CourtTime.

| Elemento | Referência Draw Code | CourtTime revisado | Adequação/situação |
|---|---|---|---|
| Capa, rosto e aprovação | Elementos separados | Presentes | Dados de aprovação continuam em branco |
| Dedicatória e agradecimentos | Pré-textuais próprios | Textos existentes preservados | Opcionais no manual |
| Resumo/abstract | Síntese bilíngue | Presentes | Sem resultados de pesquisa fabricados |
| Listas e sumário | Itens paginados | Listas de figuras/quadros/siglas e sumário | Páginas reais do candidato |
| I — Teoria | Conceitos e estudos relacionados | Conceitos de SI, reservas, segurança, usabilidade e pesquisas | Fontes próprias do CourtTime |
| II — Negócio/identidade | Apresentação do projeto e marca | Briefing e identidade institucional | Sem modelo comercial artificial |
| III — Pesquisa | Levantamento próprio do Draw Code | 28 registros primários do CourtTime | Não transferir amostra ou percentuais do outro trabalho |
| IV — Casos de uso | Diagramas e mensagens | Quatro perfis, RF/RNF, casos e retornos | Regra de rascunho reconciliada |
| V — Classes e atributos | Modelo e dicionário | Classes, dicionário e DER selecionados | Fonte única por figura; `rascunho` incluído |
| VI — Telas | Apresentação por funcionalidade | Catorze telas/recortes da aplicação real | Dados fictícios e versão identificados |
| VII — Tecnologias | Ferramentas do respectivo projeto | Stack efetiva do CourtTime | Não se importaram ferramentas do Draw Code |
| Considerações | Síntese e limites | Resultados e limitações explícitos | Sem homologação presumida |
| Referências | Lista final | 24 entradas próprias | Relatório distingue metadados verificados e parciais |
| Glossário | Presente na referência | Ausente no candidato | Opcional; não bloquear por mera comparação |

## Pendências para a banca

- **P0:** nenhum defeito documental bloqueador confirmado na prova digital. Isto não substitui aprovação do orientador ou autorização de entrega.
- **P1:** obter dados reais da banca e aprovação; validar a formulação metodológica/hipótese com o orientador; confirmar as regras institucionais ainda abertas (dez participantes versus dez alunos, vínculos, horário no dia anterior, crédito excedente e retenção); resolver a identificação autoral parcial de Romário se possível. Esses pontos não foram preenchidos por inferência.
- **P1:** confirmar com os autores recrutamento, unicidade e autorização do levantamento caso pretendam ampliar conclusões sobre a escola. O candidato utiliza somente as contagens disponíveis, sem essa extrapolação.
- **P2:** glossário; eventual aperfeiçoamento de colunas/continuações dos quadros; prova física de impressão das menores cardinalidades; acabamento dos modelos Astah 05/06/09A somente se forem novamente selecionados como figuras finais. Esses modelos não compõem a representação final contraditória desta cópia.

## Reproduzir a prova

Ambiente: Windows, Google Docs, Python com PyMuPDF e Playwright já instalado no projeto para exportar SVG. Não há nova dependência de runtime Java.

1. Exportar a aba principal atual em PDF. Usar o inventário de títulos `indices-finais.json` como entrada para `paginar.py`.
2. Executar `python paginar.py origem.pdf indices-finais.json mapa.json candidato.pdf`. Conferir o mapa e atualizar o texto das listas no Docs antes da exportação definitiva.
3. Reexportar, verificar que os 103 títulos não mudaram de página e executar `python auditar_pdf.py candidato.pdf pasta-de-prova`.
4. Inspecionar todas as pranchas e ampliar páginas relevantes. A geração automática das imagens não equivale à revisão visual.
5. `extrair-figuras.py` reconcilia as 33 imagens incorporadas; `gerar-vistas.cjs` preserva as quatro fontes SVG editáveis. O gerador aceita `COURTTIME_PLAYWRIGHT_HOME` para reutilizar uma instalação local.

As verificações Java/Playwright já concluídas na missão anterior **não foram repetidas**. Os resultados citados no TCC mantêm commit, data e ambiente da execução de software correspondente. A evidência nova desta fase é a revisão acadêmica, a renderização integral e a reconciliação dos artefatos.
