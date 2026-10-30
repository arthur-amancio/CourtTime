# Registro das decisões de modelagem

## Base e escopo

1. A base escolhida foi o commit `6fc1aee626c7478a35a93b5b70d7f70b4de1c042`, do PR #87, por conter os PRs #85 e #86 e representar a implementação mais recente disponível no início da tarefa.
2. A entrega foi limitada a nove figuras: duas de casos de uso, duas de classes, um DER, três de atividades e uma de arquitetura. A divisão preserva contexto suficiente e evita uma coleção fragmentada ou páginas sobrecarregadas.
3. O formulário oficial foi representado como artefato preservado. A divergência de redação conhecida não foi corrigida, pois não há autorização para modificar o documento institucional.

## Ferramenta e editabilidade

4. O Astah Community local foi inspecionado. A edição instalada é antiga, não oferece DER e sua API não autoriza criação de modelos nessa edição. Ela também não dispõe do MCP oficial introduzido nas versões atuais do Astah. Produzir ou converter um `.asta` sem abri-lo e validá-lo no aplicativo criaria uma afirmação falsa de compatibilidade.
5. O formato nativo adotado foi `.drawio`. Os arquivos contêm formas, textos e conectores separados em `mxCell`, além de uma versão multipágina. O mesmo fonte atende UML, DER e arquitetura e pode ser continuado no diagrams.net sem plugin.
6. SVG e PDF foram mantidos como vetores. O PNG usa 3508 × 2480 pixels, equivalente a A4 paisagem em alta resolução. Todas as figuras possuem título, número, legenda e fonte.

## Notação UML e apresentação

7. Os casos de uso mostram objetivos observáveis dos atores. Relações `include` indicam comportamento sempre reutilizado; `extend` aparece apenas quando o comportamento é condicional. Detalhes de interface e nomes de endpoints foram deixados no relatório de correspondência.
8. Os diagramas de classes usam associações navegáveis somente quando ajudam a explicar a estrutura. Dependências tracejadas ligam serviços às entidades que manipulam. Enums foram resumidos em notas para impedir compartimentos extensos.
9. Nenhuma composição foi desenhada. O código não declara `cascade = REMOVE` nem `orphanRemoval = true`; portanto, não existe evidência suficiente de ciclo de vida exclusivo no mapeamento JPA.
10. A multiplicidade representa o estado persistente permitido. A regra de ao menos dez participantes aparece como restrição de serviço e como nota, pois uma reserva em elaboração pode existir temporariamente com menos registros.
11. Os diagramas de atividades usam raias por responsável, decisões com saídas rotuladas e nós finais distintos quando o processo é aprovado, devolvido ou bloqueado. Ações externas de assinatura e conferência foram explicitadas sem sugerir integrações inexistentes.
12. A arquitetura representa dependências lógicas da aplicação monolítica. Gov.br, ITI e WhatsApp estão fora da fronteira da aplicação e são usados por pessoas.

## DER e cardinalidades

13. O DER é lógico e deriva das doze entidades JPA, não de engenharia reversa de uma instância PostgreSQL. O projeto usa `spring.jpa.hibernate.ddl-auto=update` e não possui migrações versionadas; por isso, o desenho registra o schema pretendido pelo código analisado.
14. `PK`, `FK`, `UQ` e `NN` foram incluídos para permitir conferência das restrições. Campos opcionais não recebem `NN`.
15. A remarcação é uma autorreferência 0..1–0..1 devido à coluna `remarcacao_de_id` única. O vínculo de crédito entre contribuições permanece 0..1–0..*, pois `contribuicao_origem_id` é `ManyToOne` e não possui unicidade no banco, mesmo que o fluxo limite a remarcação.
16. Papéis distintos do mesmo `Usuario` foram rotulados nas classes. No DER, ligações operacionais secundárias foram agrupadas quando desenhá-las individualmente tornaria a página ilegível; todas as FKs permanecem listadas nas entidades.

## Regras atualizadas incorporadas

17. A solicitação exige dez pessoas cadastradas no total, sem distribuição obrigatória por categoria.
18. A contribuição principal e os pagamentos adicionais usam R$ 2,00 por participante.
19. A liberação exige dez pessoas presentes e nenhum valor pendente.
20. A Segurança pode cadastrar pessoa adicional, com identificação, categoria e vínculo, somente antes da liberação.
21. O cálculo usa vagas pagas, não identidades pagantes. Se uma pessoa paga sair, outra pode ocupar aquela vaga sem cobrança duplicada; os dois registros pessoais permanecem no histórico.
22. Pagamentos recebidos na portaria ficam identificados por local e aguardam confirmação posterior de repasse pela Secretaria.
23. A liberação grava responsável e instante e impede novas alterações de participantes e presenças.

## Limitações conhecidas

24. O manual integral da ETEC e o trabalho Draw Code não estavam no repositório nem nos diretórios locais pesquisados. O pacote segue as informações de apresentação já documentadas no projeto e não declara aprovação editorial por esses materiais.
25. O diagrams.net Desktop não está instalado. A editabilidade foi validada estruturalmente pela leitura XML e pela presença de elementos independentes; os resultados SVG e PNG foram abertos e revisados visualmente. Para uma verificação manual adicional, abra o arquivo multipágina no diagrams.net listado no README.
