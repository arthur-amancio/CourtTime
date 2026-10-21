# Calendário no celular — opções A e B

No celular (abaixo de 860 px), o calendário mensal é trocado por uma faixa de dias com rolagem horizontal. O protótipo traz duas variantes dessa faixa. Para alternar entre elas, use **Controles do protótipo → Calendário no celular** em `quadra.html` ou em `aluno-nova-solicitacao.html`, ou abra a página com `?calendario=a` ou `?calendario=b`.

| | Opção A — simplificada | Opção B — com disponibilidade |
| --- | --- | --- |
| Captura | `capturas/v2/calendario-opcao-a-390.png` | `capturas/v2/calendario-opcao-b-390.png` |
| Mostra | dia da semana, número do dia, ponto quando há funcionamento, “fechado” quando não há, data selecionada | dia da semana, número do dia, “6 livres”, “2 livres” (em âmbar), “sem vagas”, “bloqueado” (listrado), “fechado”, data selecionada |
| De onde vêm os dados | Somente da **grade semanal** (`DisponibilidadeQuadra`), que já é carregada | Do cálculo de disponibilidade **de cada dia** exibido (grade, bloqueios e reservas aprovadas) |
| Já existe na aplicação? | **Sim.** A grade semanal existe e a consulta por data (`GET /quadra?data=…`) também | **Não.** Hoje o `CalendarioDisponibilidadeService` calcula um dia por vez. Não existe resumo para vários dias |
| Custo por página | Nenhuma consulta extra | Mostrar 21 dias exigiria repetir o cálculo 21 vezes ou criar uma consulta agregada nova |
| Funciona sem JavaScript | Sim: cada dia vira um link `?data=` | Sim, desde que o servidor envie as contagens já prontas |
| Risco | Baixo: o aluno só descobre se há vaga ao abrir o dia | Médio: número desatualizado se outra solicitação for aprovada entre a carga da página e o envio; mais código para testar |
| Testes novos no backend | Nenhum | Serviço de resumo por período, testes de bloqueio parcial, dia inteiro bloqueado, quadra inativa e desempenho |

## Comportamento comum às duas opções

- Datas anteriores ficam desabilitadas. Na nova solicitação, **hoje** também fica desabilitado por causa da antecedência mínima de 1 dia.
- Dias sem grade semanal continuam selecionáveis e mostram “Sem funcionamento nesta data”.
- Os períodos usam os três estados reais de `SituacaoHorario`: **Disponível**, **Reservado** (cinza, cadeado) e **Indisponível** (listras vermelhas, para bloqueios).
- Com a quadra inativa, nenhum período pode ser escolhido e aparece a mensagem do sistema atual.
- Teclado: Tab chega a cada dia; as setas ← e → percorrem a faixa; Enter ou Espaço escolhem. No calendário mensal do computador, as setas também andam por semana (↑ ↓).
- Nada sugere aprovação automática: a escolha só preenche o formulário, e o aviso “O envio não autoriza o uso” fica junto do botão.

## Recomendação

**Adotar a opção A na migração.** Ela usa só dados que já existem, não acrescenta consultas, funciona sem JavaScript e é fácil de explicar à banca. O aluno vê os horários reais assim que abre o dia, com um toque.

A opção B só deve ser considerada se o grupo decidir que vale criar uma consulta de resumo. Nesse caso, o caminho mais simples seria:

1. limitar o resumo aos próximos 14 dias;
2. calcular o resumo no servidor, em uma única passada por grade, bloqueios e reservas aprovadas do período;
3. tratar o número apenas como orientação, revalidando tudo no envio (o backend já revalida na criação e na aprovação);
4. cobrir o resumo com testes antes de exibi-lo.

Até lá, o número de vagas por dia deve ser tratado como **proposta**, não como funcionalidade existente.
