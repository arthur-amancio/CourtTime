# Entrega verificável — 29/10/2026

## PRs e código
PR85 preservado em fe21d89; PR86 preservado em4fb7a06; PR87 atualizado por merge e mapa em24d99fe; PR88 recebeu a base por merges02efd15/609036a e modelagem emc456d6f. Nenhuma alteração de runtime foi introduzida nesta missão: src/, qa/ e pom.xml continuam iguais à árvore funcional4fb7a06. O diff87 contém apenas mapa; o diff88 contém documentação/modelagem e diagnóstico SQL somente leitura.

## Validação atual
Maven Wrapper clean verify em Windows/Java17/PostgreSQL18 isolado:477 testes,476 aprovados,0 falhas,1 erro de privilégio de symlink,0 skips,1min43s. O conjunto local não está integralmente aprovado.
CI314 do PR87, HEAD24d99fe:477 aprovados,0 falhas/erros/skips em cada matriz PostgreSQL16 e18. Logs dos dois jobs foram conferidos: https://github.com/arthur-amancio/CourtTime/actions/runs/40810403653
Nova jornada real local em29/10, SHA02efd1599b2c33f1e45e145219b01a185c5425fa, banco novo courttime_e2e_p0p1_run3 e porta18081:5 verificações da guarda,367 jornada,49 navegação,285 interações,330 telas e86 finalização aprovadas. Total1.122, nenhuma reprovação.44 varreduras axe sem violações.113 PNGs,130 JPEGs,6 JSONs e2 PDFs sintéticos em qa/playwright/evidencias, ignorados pelo Git. O Spring Boot iniciou e os quatro perfis atuaram pelo Chromium. Desktop e390px incluídos. App de testes encerrada após execução.
Os commits posteriores alteram apenas documentação. A comparação de src/qa/pom com a árvore testada não apresentou diferenças.

## Materiais acadêmicos
Cópia nativa preservando57 imagens entre as duas abas: https://docs.google.com/document/d/1NwFW5kRauj1vNHkWRuP1Kxbgm4eg82ez7Uwos67mGNU/edit
Revisões efetivas: nomeCOURTTIME; presença implementada; RF04/RF05; regra de rascunho no capítuloV; limites e resultados noVII e considerações; suspensão de duas afirmações quantitativas sem dados primários conferidos; margens2,5cm;31 figuras existentes numeradas. A aba histórica e suas26 imagens foram preservadas. Figuras sem imagem não receberam número fictício.
A cópia é revisão parcial, não TCC final. Os sete capítulos existentes foram preservados; não foram integralmente reescritos. Resumo/abstract, sumário/listas paginadas, bibliografia integral e revisão visual completa ainda precisam ser concluídos.

## Entregas
- MATRIZ-ACADEMICA-E-FONTES.md: comparação DrawCode/CourtTime, regras do manual e fontes verificadas/incompletas.
- INVENTARIO-FIGURAS.md: correspondência de31 legendas com objetos reais da cópia.
- RASTREABILIDADE-E-BANCA.md: implementação/testes e roteiro prático/técnico.
- DECISOES-MODELAGEM-E-BANCO.md: fonte por figura, campo rascunho e plano de diagnóstico de legado.
- scripts/diagnostico-rascunhos.sql: somente leitura, sem dados pessoais; não executado em banco real.
- docs/tcc/modelagem-profissional: nove conjuntos editáveis, SVG, PNG3508×2480 e PDF A4; validador estrutural aprovado. Classes núcleo e DER inspecionados visualmente; DER geral ainda denso para impressão e pode exigir vistas temáticas.

## Pendências humanas e riscos
P0: nenhuma falha de programação confirmada nesta execução; não se declara homologação institucional.
P1: texto do termo dez alunos, vínculos admitidos, horário-limite no dia anterior, destino do crédito excedente e política de retenção dependem da ETEC. Nenhuma regra foi inventada.
P1: conferir exportação primária do questionário; completar referências e fonte da epígrafe; validar resumo/abstract e elementos exigidos pelo manual com orientador.
P1: revisar A4 de todas as figuras e atualizar nativo Astah05/06/09A; conciliar versões sem duas modelagens finais contraditórias.
P1: diagnosticar registros anteriores ao campo rascunho em cópia antes de migração autorizada. Não foi alterado banco real.
P2: privilégio symlink local; refinamentos editoriais e glossário se pertinente.

## Verificação posterior
CI315 em8701b9d:477 testes aprovados por matriz PostgreSQL16/18,0 falhas/erros/skips; logs conferidos. https://github.com/arthur-amancio/CourtTime/actions/runs/40810912204
A cópia nativa foi exportada e páginas selecionadas foram inspecionadas. Corrigidos recuos nas tabelas, fonte11 nas células, parágrafo de rascunho em corpo12, margem2,5cm e espaço da capa para manter2026 na mesma página. Linhas de tabelas foram protegidas contra divisão entre páginas. Não equivale a aprovação visual das cem páginas nem do formato final. Provas privadas em data/fontes-academicas.

## Retomada
Começar por docs/tcc/CONTINUIDADE-MISSAO.md. Conferir HEADs e CI antes de novos edits. Fontes integrais e PDF exportado ficam privados em data/fontes-academicas; não adicionar data/ ao Git. Revisar a cópia, nunca o consolidado original. Sem merge na main, force push, deploy ou mudança de compartilhamento.
