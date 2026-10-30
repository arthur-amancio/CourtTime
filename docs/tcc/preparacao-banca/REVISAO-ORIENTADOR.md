# CourtTime — pacote de revisão para o orientador

**Data:** 29 de outubro de 2026. **Finalidade:** subsidiar a revisão acadêmica e as decisões institucionais anteriores à banca. Este documento não constitui homologação, ata de aprovação ou autorização de integração.

## Identificação

- **Projeto:** CourtTime — Sistema Web de Gerenciamento e Agendamento de Quadras Esportivas.
- **Instituição:** ETEC Prof. Armando José Farinazzo, Fernandópolis/SP.
- **Curso e ano:** Técnico em Informática para Internet, 2026.
- **Autores:** Arthur Amancio Francisco, Carlos Eduardo Ramos Alves, Gustavo Tagliaferro Sabadini e Heitor Silva Rodrigues.
- **Orientador:** Prof. André Luis Zampieri Mosca.

## Síntese do desenvolvimento

O CourtTime centraliza a solicitação e o acompanhamento de reservas de quadra, a documentação e a atuação de quatro perfis. A aplicação utiliza Java 17, Spring Boot, Spring MVC, Spring Security, Thymeleaf, PostgreSQL e Maven. O recebimento de contribuições é presencial; o sistema registra os valores e os responsáveis, sem processar pagamentos pela internet.

O aluno consulta a disponibilidade, inicia um rascunho e cadastra participantes. O backend soma ALUNO e OUTRO e libera a etapa documental a partir de dez pessoas. A Secretaria confere o PDF e registra a contribuição. A Administração decide sobre a solicitação, administra horários e bloqueios e registra cancelamentos. A Segurança consulta reservas autorizadas, registra comparecimento e adicionais e libera a quadra quando os requisitos são cumpridos.

Entre os comportamentos implementados estão a prevenção de conflitos de horário, a proteção de documentos por autorização, o histórico de decisões, a remarcação com nova documentação e aprovação e o aproveitamento do pagamento da reserva cancelada institucionalmente. Na portaria, uma vaga adicional já paga pode ser ocupada por um substituto antes da liberação, mantendo o histórico e sem cobrança duplicada. O recebimento pela Segurança e a confirmação de repasse pela Secretaria são registros distintos.

## Metodologia e limites das conclusões

O trabalho reúne levantamento de requisitos, desenvolvimento incremental, modelagem UML/DER e validação por testes automatizados e navegação real. As regras institucionais registradas no repositório são sínteses do relato de Arthur sobre conversa com o responsável pelo procedimento; não são uma entrevista formal transcrita.

A planilha primária contém **28 registros**, coletados de 6 a 24 de outubro de 2026. A revisão conferiu contagens agregadas: 26 respostas favoráveis à consulta de disponibilidade, uma contrária e uma intermediária; 25 favoráveis à solicitação pelo sistema e três indiferentes; 24 indicaram celular e quatro celular e computador. Esses resultados descrevem os registros disponíveis. Não comprovam unicidade dos respondentes, representatividade da comunidade escolar ou redução de tempo obtida com o software.

A classificação metodológica final e a adequação da pergunta/premissa à hipótese exigida no manual precisam do parecer do orientador. Não se declara uma pesquisa hipotético-dedutiva ou uma homologação da ETEC sem documentação correspondente.

## Evidências de validação

| Verificação | Versão e evidência | Resultado comprovado | Limite de interpretação |
|---|---|---|---|
| Suíte Java e integração PostgreSQL | [CI #319](https://github.com/arthur-amancio/CourtTime/actions/runs/40868642896), commit `47b0a5333fc98a5e600aa79335896e4334972cc7`; logs dos dois jobs consultados nesta missão | **477 testes aprovados por matriz**, PostgreSQL 16 e 18; zero falhas, erros ou skips em cada matriz; `BUILD SUCCESS` | São os mesmos 477 testes em dois ambientes, não 954 casos distintos. Não foram reexecutados localmente nesta missão. |
| Navegação dos quatro perfis | Execução anterior em 29/10, commit `02efd1599b2c33f1e45e145219b01a185c5425fa`; [registro de entrega](../revisao-20261029/ENTREGA-E-VALIDACAO.md) | 1.122 verificações aprovadas; 44 varreduras axe sem violações reportadas; 113 PNGs; desktop e 390 px | Contagem de verificações, não de testes Java ou de usuários. Não equivale a certificação integral de acessibilidade. |
| Maven no Windows | Execução anterior registrada no checkpoint | 477 executados: 476 aprovados, um erro ambiental de privilégio para symlink, zero skips | A suíte local **não** foi integralmente aprovada. A limitação não foi classificada como falha de domínio. |
| Prova acadêmica digital | [Auditoria A4 anterior](../finalizacao-academica/AUDITORIA-A4.md) | 82 páginas inspecionadas; 33 figuras, sete quadros, 24 referências; índices reconciliados | Não substitui parecer do orientador nem prova física. O PDF não foi alterado ou novamente auditado nesta missão. |

A comparação de `src/`, `qa/`, `pom.xml` e `.github/` entre a base funcional `4fb7a06` e o candidato `47b0a53` não apresentou diferenças. Os resultados de navegador continuam identificados pela execução original; a equivalência desses arquivos não transforma uma evidência antiga em execução nova.

## Situação do TCC

O candidato tem sete capítulos revisados, resumo e abstract, palavras-chave, sumário e listas paginadas. O material histórico foi preservado separadamente. O manual da ETEC orientou a formatação; Draw Code serviu de referência de organização e apresentação, sem reprodução de texto, resultados ou identidade visual.

**Classificação: pronto para revisão do orientador.** A autorização para entrega definitiva à banca depende do retorno acadêmico e das confirmações abaixo. A contagem de 24 referências não significa que todos os textos integrais tenham sido consultados; permanecem limites explicitados no [relatório bibliográfico](../finalizacao-academica/BIBLIOGRAFIA-E-PESQUISA.md), incluindo a identificação parcial de autoria de Romário.

## Decisões solicitadas

Todas as linhas abaixo são **P1 — importantes antes da banca**. O orientador pode encaminhar os itens operacionais ao responsável institucional; não se presume que sua aprovação acadêmica substitua uma decisão administrativa.

| Tema | Comportamento ou registro atual | Dúvida | Decisão necessária e responsável |
|---|---|---|---|
| Dez alunos ou dez participantes totais | O software formaliza com dez pessoas, somando ALUNO e OUTRO; abaixo disso mantém rascunho. O formulário institucional original continua dizendo “dez alunos”. | Qual redação é válida para uso institucional? | Responsável institucional confirmar o mínimo e aprovar/fornecer termo compatível. Até lá, apresentar a divergência sem alterar silenciosamente o PDF original. |
| Vínculos de OUTRO | São oferecidos ex-aluno, pai/mãe de aluno, irmão/irmã de aluno e professor da ETEC; o backend exige vínculo nessa categoria. | Essa lista corresponde às pessoas efetivamente autorizadas? | ETEC confirmar categorias, condições e eventual comprovação. Não interpretar OUTRO como acesso irrestrito a convidados. |
| Prazo documental | A solicitação é futura e o envio do PDF é bloqueado a partir do dia do uso. O limite atual é o dia anterior, sem um corte institucional mais cedo confirmado. | Há horário específico no dia anterior? Como tratar uma devolução documental no dia do uso? | ETEC definir horário, comunicação e tratamento da devolução; não presumir exceção automática. |
| Crédito excedente na remarcação | O crédito reduz a nova contribuição, sem cobrança negativa. O excedente é registrado, sem destino posterior definido. Nova documentação e aprovação continuam obrigatórias. | Pode ser reutilizado, restituído ou receber outro tratamento? | Responsável institucional definir destino e rastreabilidade. Não demonstrar estorno, carteira de saldo ou novo crédito como recurso existente. |
| Retenção de dados e documentos | Documentos e históricos são mantidos para consulta autorizada, sem expurgo automático nesta etapa. | Qual prazo, fundamento, responsabilidade e procedimento de descarte? | Instituição definir política de retenção, acesso e descarte; avaliar adequação jurídica com responsável competente. |
| Metodologia e hipótese | Levantamento documental/conversas relatadas, 28 registros de questionário e validação técnica; pergunta e premissa preservadas com limites explícitos. | A classificação e a formulação atendem ao manual e à orientação do curso? | Orientador validar a redação e a suficiência da hipótese, sem converter a amostra em pesquisa representativa ou inventar experimentação. |
| Banca e aprovação | Candidato sem examinadores, data ou aprovação inventados. | Quem comporá a banca e quais serão data, duração, local e elementos obrigatórios da apresentação? | Orientador/coordenação fornecer dados oficiais. Preencher aprovação somente conforme procedimento real. |

Não há P0 técnico/documental novo confirmado por esta revisão de preparação. A existência de decisões P1 impede declarar homologação institucional definitiva. Prova física das menores figuras e ajustes opcionais de apresentação são P2.

## Materiais para consulta

- [PDF candidato — 82 páginas](https://drive.google.com/file/d/17kUeBkY0cUHn0T6VEYYijNXZBmV95Lwz/view) e [PDF versionado](../finalizacao-academica/CourtTime-candidato.pdf). SHA-256 de referência: `8475492fe64eb55aed671c57f62e8f78d0fc3dd753cc480b921890c4c9edd77b`.
- [Candidato editável congelado](https://docs.google.com/document/d/1h5l4LAgdLetneb-vtNf-KGEiELmNRACUobQzdhOHvC0/edit), [cópia de trabalho](https://docs.google.com/document/d/1NwFW5kRauj1vNHkWRuP1Kxbgm4eg82ez7Uwos67mGNU/edit) e [pasta de entrega](https://drive.google.com/drive/folders/14oKiQNnPwLuvLQ4XwPkcfWcGDDlPX5Dq). Links preservam as permissões existentes; este pacote não concede acesso.
- [Checklist acadêmica](../finalizacao-academica/ENTREGA-ACADEMICA.md), [inventário das 33 figuras](../finalizacao-academica/INVENTARIO-FIGURAS.md) e [rastreabilidade técnica](../revisao-20261029/RASTREABILIDADE-E-BANCA.md).
- [PR #88 — documentação/modelagem](https://github.com/arthur-amancio/CourtTime/pull/88), [plano de integração](PRONTIDAO-INTEGRACAO.md), [apresentação](APRESENTACAO-E-ROTEIRO.md) e [demonstração](DEMONSTRACAO.md).

**Retorno esperado:** parecer acadêmico e registro das decisões, com responsável e data. Não é necessário editar o candidato congelado para emitir o parecer; qualquer revisão posterior deve preservar esta versão e gerar nova prova paginada.
