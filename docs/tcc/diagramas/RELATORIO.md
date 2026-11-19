# CourtTime — Relatório técnico dos diagramas

Data da consulta: 21/10/2026, America/Sao_Paulo.

## Base e escopo

Commit da main: `7b40dcb1aeeb97b60cef751e0bbda7b7c138de73`. Projeto Java 17, Spring Boot 3.5.16, versão Maven 0.0.1-SNAPSHOT. O PR #68 já está integrado. Os PRs #70 (head 1d4bc3dcd69be8a7c81e1d4f7b3dfc8f2f9782ab) e #71 (head dc0dd5c962b0a4ea4f30dcc1acd7a9faad790efa) estavam abertos. #69 altera imagens e #49 é protótipo explícito, não base funcional. Branches foram consultadas; a existência de branch antiga não prova implementação na main.

Os sete tipos pedidos foram produzidos em 26 figuras complementares. Os arquivos DOT são a fonte editável; SVG é vetorial, PNG é raster em alta resolução e PDF individual preserva o vetor. Graphviz foi usado por estar disponível no ambiente; a preferência por PlantUML não pôde ser atendida com o renderizador local disponível. Nenhum desenho foi gerado por IA de imagens. A notação é UML nos casos de uso e classes; atividades usam ações, decisões e partições de responsabilidade. O DER é lógico-relacional com cardinalidades numéricas, não uma mistura de herança Java e tabelas.

## Fontes lidas e precedência

- Código Java, entidades, enums, repositories, controllers, segurança e armazenamento, pom.xml e application.properties no SHA fixado. Onze entidades e seis enums.
- README, guia-do-courttime, inventario-tecnico, regras-etec, plano-regras-etec e engineering-workflow.
- PRs #68, #70, #71, listagem de PRs abertos, issues #15 e #18 e comentários da issue #18; registros históricos são confrontados com código atual.
- Google Docs de trabalho, versão atual consultada em 21/10/2026. I e II já refletem regras recentes; III–V ainda contêm afirmações superadas.
- Manual de TCC e Metodologias 2024: PDF completo de 74 páginas disponível para consulta; regras de apresentação geral e ilustrações examinadas, incluindo a página PDF 45.
- Draw Code: PDF de 90 páginas; organização de atores, casos, classes e dicionário analisada. A página PDF 66 mostra a figura de classes e início do dicionário.
- Pesquisa: 11 respostas na leitura atual; 9 preferem celular e 2 ambos. Todos os 11 indicam que usariam consulta e solicitação. Amostra exploratória, sem generalização. Sugestões de integração WhatsApp não se tornam requisito aprovado.

O manual prevalece para formatação; o Draw Code orienta organização. Regras finais orientam o modelo pretendido; o código determina o que já existe. Não se inventa elegibilidade institucional para resolver uma divergência.

## Situação da versão final

Implementados: quatro perfis; sessão; consulta; solicitações de 1 h; mínimo de dez ALUNOS; participantes; formulário oficial com páginas adicionais; PDF privado e versionado; conferência humana; contribuição presencial manual; ajustes; aprovação / rejeição; cancelamento; remarcação sem crédito; consulta da segurança; auditoria de contas.

Em desenvolvimento: R01.2 no PR #70; valor devido e cálculo R03 no PR #71. Não foram integrados por esta tarefa.

Planejados: vínculo de OUTRO; confirmação gov.br na conferência; presença e ausência; adicionais; aproveitamento de crédito; registro de excedente; lembrete de contato manual. Campos sugeridos no plano são separados dos campos JPA reais.

## Inconsistências e ações técnicas separadas

1. **Sem Flyway.** A árvore e o pom.xml não contêm migrações ou dependência Flyway. O esquema usa Hibernate `ddl-auto=update`. Não apresentar DER como derivado de migrations inexistentes. A reprodução controlada de alterações de schema deve ser tratada em mudança específica, sem alterar produção nesta missão.
2. **Crédito com cobrança zero.** ContribuicaoApm.valor, AjusteContribuicaoApm e o serviço exigem valores positivos. R04 exige zero novo recebimento quando o crédito cobre o devido. Será necessário diferenciar dinheiro recebido de crédito aplicado, ajustar validações e testar zero. Não registrar recebimento fictício.
3. **Excedente ausente da lista de colunas.** O plano manda gravar excedente, mas a lista final não declara campo. `saldoExcedente` / `saldo_excedente` é proposta explícita deste modelo, sem destino até D2.
4. **Uso único financeiro.** A UQ em Reserva.remarcacaoDe impede duas sucessoras, mas não basta sozinha para todas as disputas de aplicação e ajustes. Proposta: bloquear origem e destino em ordem determinística; validar dono e cadeia; UQ em contribuicao_origem_id; revalidar antes de gravar; rollback integral. Nenhuma dessas novas garantias foi declarada implementada.
5. **Ajustes após uso do crédito.** Alterar dinheiro recebido na origem depois do uso pode distorcer a cadeia. A implantação precisa definir tratamento auditável; não propomos bloquear correção institucional sem decisão. Testar concorrência ajuste × aplicação. Resultado conservador: não criar novos direitos ou reembolsos automaticamente.
6. **Rejeição da remarcação.** Os registros não definem liberação do crédito ou nova tentativa após rejeição da sucessora. A relação atual admite uma sucessora por cancelada. O modelo não promete reutilização, estorno ou dispensa de aprovação.
7. **R09 divergente.** Relato: ex-aluno, pai/mãe, irmão/irmã, professor. Termo: pais, filhos e cônjuges de alunos, egressos, professores e seus filhos/cônjuges, funcionários e seus filhos/cônjuges. Não consolidar a lista como regra definitiva sem ETEC. O plano contém enum provisório, não homologação institucional.
8. **Documentação acadêmica antiga.** III/3.2 ainda diz aguardar respostas; I/II já têm 11. IV/4.5 repete pendências superadas. V termina afirmando que a geração do termo não foi comprovada, embora FormularioOficialService e PDFBox a implementem. Os novos textos corrigem essas afirmações no complemento, sem reescrever I e II.
9. **Responsabilidades e dados.** Segurança não acessa PDF, contribuição nem decisões. AuditoriaUsuario não representa todos os eventos do sistema. RM/RG não é conta de usuário. CPF e curso usados na geração não constam como atributos persistidos.
10. **Presença sem marcação.** Nulo não significa ausente. O plano registra responsável e último momento; não define histórico completo de correções. Caso a escola exija trilha imutável de alterações de presença, será necessário requisito adicional.

## Pendências institucionais comprovadas

D1: horário específico no dia anterior; até lá, não aceitar no dia de uso. D2: destino do excedente; somente registrar. D3: autoridade e procedimento para pessoa extra; somente registrar comparecimento. Também pendem janela de presença, conciliação de vínculos com o termo e política de retenção/descarte. Não foi criada regra nova de convidados, reembolso, reutilização após rejeição ou entrada automática.

## Apresentação acadêmica e integração

Texto de inserção preparado por figura: contexto, título numerado, imagem, fonte e explicação. Capítulo IV recebe comportamento; V recebe classes, DER e dicionário; arquitetura destina-se ao capítulo de tecnologias, seguindo a organização do Draw Code sem criar ou reorganizar capítulos existentes.

Margens A4 de 2,5 cm no atlas; título de figura 12 pt e fonte 10 pt. O pacote fornece textos editáveis para Arial 12, justificado, 1,5 e primeira linha de 2,5 cm; essas propriedades são aplicadas no Google Docs. O renderizador local não possui Arial licenciada: o PDF usa Helvetica/Nimbus Sans como substituta; os SVG declaram Arial e dependem da fonte do dispositivo. Essa diferença impede declarar o PDF local tipograficamente idêntico ao manual. Para a versão acadêmica final, exportar do Google Docs com Arial após revisão da paginação.

A aba original e suas 15 imagens foram preservadas. Uma cópia de segurança foi criada antes da edição. As figuras novas ficam em aba complementar, numeradas 22–47, para não substituir inadvertidamente as figuras 18–21. A consolidação dentro dos capítulos exige substituir a modelagem antiga e atualizar a lista de figuras; o complemento não pretende certificar o TCC inteiro como finalizado.

## Verificações e limites

Sintaxe DOT validada pelo Graphviz; 26 SVG renderizados. Imagens abertas e examinadas visualmente; atividades foram divididas e rótulos de auditoria ajustados após a primeira inspeção. Entidades, atributos, FKs e nulidade confrontados com JPA; regras e propostas confrontadas com docs. Dicionário e matriz acompanham todos os recortes.

Testes Java/PostgreSQL relacionados foram lidos, não executados nesta tarefa documental: não há checkout completo nem ambiente de banco preparado, e não houve alteração de código de produção. Os resultados históricos de CI não são apresentados como execução própria. O DER é derivado do mapeamento; não foi feita introspecção de banco em execução. Não houve merge, deploy ou mudança de dados da aplicação.

## Arquivos

`fontes/*.dot`, `svg/*.svg`, `png/*.png`, `pdf/*.pdf`, `figuras.json`, `textos-academicos.md`, `dicionario-atributos.md`, `inventario-jpa.json`, `matriz-rastreabilidade.md`, `rastreabilidade.json` e `validacao.json`. `renderizar.cjs` recompila os DOT usando @viz-js/viz, sem mudar a aplicação. O atlas e o pacote ZIP são entregas de leitura e edição.
