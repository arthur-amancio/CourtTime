# Modelagem profissional do CourtTime

Este diretório substitui o conjunto acadêmico anterior por nove diagramas concisos, editáveis e rastreáveis à implementação. A modelagem foi reconstruída sobre o commit `6fc1aee626c7478a35a93b5b70d7f70b4de1c042`, cabeça do PR #87 em 22 de outubro de 2026. Esse commit contém as alterações dos PRs #85 e #86; portanto, a análise não se limita ao commit antigo `4df4907`.

## Entregas

| Figura | Diagrama | Fonte editável | Uso principal |
|---:|---|---|---|
| 1 | Casos de uso gerais | `fontes/01-casos-de-uso-gerais.drawio` | Visão funcional de todos os perfis |
| 2 | Casos de uso da portaria e da Secretaria | `fontes/02-casos-de-uso-portaria-secretaria.drawio` | Presença, pessoa adicional, pagamento e repasse |
| 3 | Classes do núcleo de reservas | `fontes/03-classes-nucleo-reservas.drawio` | Estrutura de reserva, agenda, usuário e participante |
| 4 | Classes de documentação, contribuição e portaria | `fontes/04-classes-documentacao-contribuicao-portaria.drawio` | Evidências, conferência, crédito, ajustes e liberação |
| 5 | DER lógico PostgreSQL | `fontes/05-der-postgresql.drawio` | Doze tabelas persistidas e suas restrições |
| 6 | Atividades da reserva completa | `fontes/06-atividade-reserva-completa.drawio` | Da escolha do horário à aprovação |
| 7 | Atividades de cancelamento e remarcação | `fontes/07-atividade-cancelamento-remarcacao.drawio` | Cancelamento institucional e reaproveitamento de crédito |
| 8 | Atividades da liberação da quadra | `fontes/08-atividade-liberacao-quadra.drawio` | Conferência na portaria, pagamento e congelamento |
| 9 | Arquitetura do CourtTime | `fontes/09-arquitetura-courttime.drawio` | Aplicação monolítica, persistência e serviços humanos externos |

O arquivo `fontes/courttime-modelagem-profissional.drawio` reúne as nove páginas. Cada figura também possui SVG vetorial em `svg/`, PDF vetorial A4 paisagem em `pdf/` e PNG 3508 × 2480 em `png/`. Os arquivos em `impressao/` são páginas intermediárias usadas na exportação. `manifesto.json` registra a base, o formato e os nomes canônicos.

## Como editar e exportar

1. Abra um arquivo `.drawio` no diagrams.net Desktop ou em <https://app.diagrams.net/>. Cada forma, texto e conector é um elemento `mxCell` independente.
2. Preserve o tamanho de página A4 paisagem, a numeração, a legenda e a fonte acadêmica já incluídas.
3. Após uma alteração feita no gerador, execute `node docs/tcc/modelagem-profissional/gerar-diagramas.cjs`.
4. Execute `./docs/tcc/modelagem-profissional/exportar.ps1` no PowerShell para recriar PNG e PDF com Google Chrome ou Microsoft Edge instalado.
5. Execute `./docs/tcc/modelagem-profissional/validar.ps1` para verificar quantidade de páginas, XML, elementos editáveis, dimensões e presença de todas as entregas.

O gerador é a origem reproduzível do conjunto. Uma edição feita apenas pela interface do diagrams.net continua válida, mas será sobrescrita se o gerador for executado depois; nesse caso, incorpore também a mudança em `gerar-diagramas.cjs`.

## Ferramentas verificadas

- **Astah Community instalado:** edição antiga localizada em `C:\Program Files (x86)\astah-community`. A documentação local dessa edição restringe criação de modelos pela API às edições UML/Professional e não oferece DER. Ela também antecede o MCP oficial do Astah, disponível nas versões atuais 11 ou superiores. Por isso, nenhum arquivo `.asta` foi declarado ou produzido.
- **diagrams.net:** formato nativo `.drawio` adotado para UML, DER e arquitetura porque é editável, aberto e adequado às nove notações usadas.
- **Google Chrome/Microsoft Edge:** usados somente para renderização local, sem instalar dependências, plugins ou serviços.
- **Node.js e PowerShell:** necessários para regeneração e validação do pacote.

## Documentação de apoio

- [Texto para os capítulos IV e V](capitulos-iv-v.md)
- [Correspondência entre diagramas e código](correspondencia-codigo.md)
- [Decisões de modelagem](decisoes-modelagem.md)
- [Validação e entrega](validacao-e-entrega.md)
- [Fontes técnicas consultadas](fontes-consultadas.md)

Ao inserir as figuras no texto definitivo do TCC, atualize a numeração conforme a lista global de ilustrações. As legendas deste pacote usam a sequência interna de 1 a 9.
