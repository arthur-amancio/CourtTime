# RETOMADA DA MISSÃO — LEIA PRIMEIRO

Atualizado em 29/10/2026. Fase de finalização acadêmica concluída. **Próxima atividade: revisão humana do candidato pelo orientador e pelos autores, usando a checklist.** Não repetir auditoria inicial, testes Java/Playwright, geração de figuras ou revisão das provas já registradas sem alteração que justifique nova validação.

## Entregas atuais

- Cópia de trabalho atualizada: https://docs.google.com/document/d/1NwFW5kRauj1vNHkWRuP1Kxbgm4eg82ez7Uwos67mGNU/edit
- Candidato editável privado: https://docs.google.com/document/d/1h5l4LAgdLetneb-vtNf-KGEiELmNRACUobQzdhOHvC0/edit
- PDF candidato de 82 páginas: https://drive.google.com/file/d/17kUeBkY0cUHn0T6VEYYijNXZBmV95Lwz/view
- Pacote com fontes, figuras, relatórios e 21 pranchas da prova integral: https://drive.google.com/file/d/11e5LLDbStdgF4_GfVXqrIW3ypySIL2bz/view
- Pasta privada: https://drive.google.com/drive/folders/14oKiQNnPwLuvLQ4XwPkcfWcGDDlPX5Dq
- Arquivo recuperável anterior, incluindo a aba histórica: https://docs.google.com/document/d/1uOYDs8srmcJuJanmZwvMb68J7i1huWvpC48SyG7U5Kk/edit

Relatórios versionados em docs/tcc/finalizacao-academica:
ENTREGA-ACADEMICA.md (checklist e comparação Draw Code), BIBLIOGRAFIA-E-PESQUISA.md, INVENTARIO-FIGURAS.md e AUDITORIA-A4.md. Catalogação e integridade em catalogo-figuras.json e verificacao-final.json. PDF: CourtTime-candidato.pdf.

## Concluído

Sete capítulos revisados, pré-textuais completos para revisão, resumo de 180 palavras e abstract de 163, cinco palavras-chave por idioma, lista de 15 siglas, sumário com 63 títulos, 33 figuras e sete quadros: 103 títulos com páginas reais. A folha física 16 inicia a impressão do número 15; o último é 81. Bibliografia com 24 entradas e limites de metadados/acesso explicitados.

Pesquisa primária conferida: 28 registros de 6 a 24/10/2026. Consulta de horários 26/1/1; solicitação 25/3; dispositivos 24/4. Não publicar dados individuais nem interpretar os registros como 28 respondentes únicos ou amostra representativa. A antiga menção a 11 respostas não foi reutilizada.

Histórico retirado da cópia de revisão e preservado em arquivo: 31 imagens na aba principal anterior e 26 na histórica. O consolidado oficial não foi modificado. As permissões preexistentes não foram alteradas; PDF, candidato privado, arquivo privado e pacote novos foram conferidos com acesso somente do proprietário.

UML/DER: figuras 12–14 e 19 preservadas do acervo Astah; 15–18 usam SVG/PNG derivados de JPA, incluindo rascunho. As quatro fontes SVG são editáveis e têm gerador reproduzível. Tipos/campos selecionados conferidos nas entidades. Corrigido sha256 para varchar(64), inclusive no DER Draw.io completo, SVG/PNG/PDF e fonte multipágina. Nenhum arquivo .asta editado. As antigas variantes de classes/DER não permanecem como segunda representação final no corpo do TCC.

## Validação acadêmica nova

82 páginas A4 renderizadas e inspecionadas, com ampliações de figuras e tabelas durante as revisões. Zero páginas vazias e zero texto fora das margens no limiar geométrico auditado. 33 PNGs reconciliados, resolução de uso mínima 158 dpi; sete quadros e 40 fontes de ilustrações/quadros. Nenhum marcador Figura[a numerar], COURT-TIME ou líder 000 remanescente.

A última correção do DER alterou somente a folha física 59 na comparação raster das 82 páginas; essa folha foi reinspecionada. Os 103 títulos mantiveram as posições. validar.ps1 aprovou os nove conjuntos editáveis e exportações após a correção. Sintaxe Python/Node, links locais dos relatórios, 33 hashes do catálogo e integridade ZIP conferidos.

PDF local/Drive idênticos por SHA-256:
8475492fe64eb55aed671c57f62e8f78d0fc3dd753cc480b921890c4c9edd77b
Pacote ZIP local:
2d58dc413dd93ef71fca4cf78fba366841dd5737b413db05f5ae6e305baba821

**Limite importante:** sumário/listas são estáticos no Google Docs; os números de rodapé foram materializados no PDF. Qualquer edição posterior exige nova exportação, recálculo das listas e conferência completa. Não exportar diretamente do Docs e presumir identidade com o PDF paginado entregue.

## Commits e preservação

- cfae27555bba02f4a22dbd1db8f30fa032dcb53d — correção do tipo do hash nas fontes do DER.
- 6cb94ad008ee4e6c961d1a07e5c058bd4e47096e — candidato acadêmico e prova integral, 49 arquivos.
- Esta atualização do checkpoint fecha o estado de continuidade em commit documental separado.

Branch: docs/modelagem-uml-profissional, worktree CourtTime-Missao88, PR88. Base do PR preservada: chore/ui-v2-verificacao-portaria (PR87). Sem merge, rebase ou force push. Checkout original, application.properties e main preservados. src, qa e pom sem alteração nesta fase.

## Referência de software anterior — não repetir

Base funcional 4fb7a06; PR87 em 24d99fe; PR88 anterior à fase acadêmica em 2e7c07f. CI318 anterior: 477 aprovados por PostgreSQL16/18. Maven Windows anterior: 477 executados, 476 aprovados, um erro ambiental de symlink, zero skips. Playwright anterior no SHA02efd1599b2c33f1e45e145219b01a185c5425fa: 1.122 verificações, 44 varreduras axe e 113 PNGs. Nenhuma nova execução dessas suítes nesta fase acadêmica; as evidências mantêm seus commits e datas.

## Pendências e próxima tarefa exata

Classificação: **pronto para revisão do orientador**, sem aprovação definitiva da banca.

P0: nenhum defeito documental bloqueador confirmado na prova digital.
P1: dados reais de banca/aprovação; validar a formulação de hipótese e método com o orientador; confirmar regras institucionais ainda abertas (dez totais versus dez alunos, vínculos, horário anterior, crédito excedente e retenção); identificação autoral parcial de Romário. Recrutamento/unicidade do questionário e consulta integral de algumas fontes exigem confirmação caso se pretendam ampliar as conclusões.
P2: glossário; aperfeiçoamento de colunas/continuações; prova física das menores cardinalidades. Astah05/06/09A somente se forem novamente selecionados para a versão final.

A próxima sessão deve incorporar o retorno real do orientador e dos autores na cópia de trabalho, sem sobrescrever o consolidado oficial. Começar pela checklist de ENTREGA-ACADEMICA.md, não por repetir investigação de código. Se houver edição, recalcular os 103 títulos com indices-finais.json/paginar.py, renderizar novamente e reinspecionar as páginas. Preservar o candidato atual como versão recuperável.
