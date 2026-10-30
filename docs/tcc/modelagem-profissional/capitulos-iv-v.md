# Textos explicativos para os capítulos IV e V

Os textos abaixo podem ser incorporados ao trabalho após o ajuste da numeração das figuras à lista geral de ilustrações. As expressões “Figura 1” a “Figura 9” correspondem à numeração interna deste pacote.

## Capítulo IV — Análise e modelagem do sistema

### Visão funcional

A Figura 1 apresenta os casos de uso gerais do CourtTime e delimita quatro perfis autenticados. O aluno consulta a disponibilidade, cria a solicitação, mantém a relação de participantes enquanto o processo permite alterações, gera e envia o formulário oficial e acompanha o resultado. A Secretaria confere documentos e a assinatura eletrônica, registra a contribuição presencial e aprova ou devolve a solicitação. O administrador mantém quadras, disponibilidade e bloqueios, administra usuários e pode efetuar cancelamento institucional. A Segurança consulta as reservas aprovadas do dia, registra presença, inclui pessoas adicionais antes da liberação e libera a quadra. As associações foram mantidas apenas quando o perfil executa diretamente o caso de uso, reduzindo repetição e cruzamento de linhas.

**Figura 1 – Diagrama geral de casos de uso do CourtTime.**
**Fonte:** Elaborado pelos autores (2026).

A Figura 2 detalha a cooperação entre Segurança, Secretaria e sistema no dia da reserva. A Segurança identifica presentes e ausentes e pode cadastrar pessoa adicional com identificação, categoria e vínculo. O cálculo do valor considera as vagas já pagas e evita nova cobrança quando outra pessoa ocupa uma vaga cujo pagamento foi preservado. Um pagamento adicional pode ser registrado na Secretaria ou, por acordo, na portaria. Neste último caso, o registro permanece pendente de confirmação de repasse pela Secretaria. A liberação exige dez presentes e valor pendente igual a zero; depois dela, participantes e presenças ficam congelados.

**Figura 2 – Casos de uso da portaria e da Secretaria.**
**Fonte:** Elaborado pelos autores (2026).

### Modelo estrutural

A Figura 3 descreve as classes do núcleo de reservas. `Reserva` centraliza data, horário, finalidade, estado e revisão da lista de participantes. Cada reserva pertence a uma quadra e a um solicitante, enquanto uma quadra pode possuir diversas disponibilidades, bloqueios e reservas ao longo do tempo. A classe `ParticipanteReserva` preserva identificação, categoria e vínculo e também registra presença e eventual inclusão pela portaria. A autorreferência opcional de `Reserva` representa a origem de uma remarcação; a unicidade desse vínculo impede que o mesmo cancelamento gere mais de uma nova solicitação. Os estados foram resumidos em nota para conservar a legibilidade em A4.

**Figura 3 – Diagrama de classes do núcleo de reservas.**
**Fonte:** Elaborado pelos autores (2026).

A Figura 4 separa o subsistema de documentação, contribuição e portaria. Cada envio produz uma versão imutável de `DocumentoReserva`, que pode receber uma única `ConferenciaDocumento`. A contribuição presencial associa a reserva e a conferência que lhe dá suporte, registra o valor devido, eventual crédito de remarcação e o usuário responsável. Ajustes posteriores formam um histórico próprio. Os pagamentos adicionais registram quantidade de pessoas, valor, local e responsáveis pelo recebimento e pelo repasse. A ausência de composição UML foi intencional: as entidades JPA não declaram remoção em cascata nem `orphanRemoval`, logo o ciclo de vida exclusivo não poderia ser afirmado somente a partir do código.

**Figura 4 – Diagrama de classes de documentação, contribuição e portaria.**
**Fonte:** Elaborado pelos autores (2026).

### Modelo de dados

A Figura 5 apresenta o DER lógico derivado das doze entidades JPA. As marcações `PK`, `FK`, `UQ` e `NN` representam, respectivamente, chave primária, chave estrangeira, unicidade e obrigatoriedade. As cardinalidades decorrem de `ManyToOne`, `OneToOne`, `optional`, `nullable` e restrições únicas do mapeamento. Uma reserva admite várias versões de documento, participantes e pagamentos adicionais, mas no máximo uma contribuição. Uma versão de documento admite no máximo uma conferência. O DER também preserva os papéis de usuário responsáveis por análise, cancelamento, liberação, presença, conferência, pagamentos e auditoria. Trata-se de um modelo lógico do schema pretendido pelo Hibernate; o projeto usa `ddl-auto=update` e não contém migrações versionadas que comprovem o estado de uma base já existente.

**Figura 5 – Diagrama entidade-relacionamento lógico do CourtTime.**
**Fonte:** Elaborado pelos autores (2026).

### Processos principais

A Figura 6 organiza a reserva completa em raias de Aluno, CourtTime e Secretaria. O fluxo inicia na escolha de um horário disponível, cria a solicitação e exige ao menos dez pessoas cadastradas no total, independentemente da categoria. Depois da geração, assinatura externa e envio do formulário, a Secretaria confere o documento. Uma devolução reabre a correção; um documento válido permite registrar a contribuição de R$ 2,00 por participante e decidir pela aprovação. O diagrama separa ações humanas de validações do sistema e evidencia os pontos que impedem o avanço.

**Figura 6 – Diagrama de atividades da reserva completa.**
**Fonte:** Elaborado pelos autores (2026).

A Figura 7 mostra o cancelamento institucional e a remarcação. O administrador registra o cancelamento e seu motivo. O aluno pode então iniciar uma nova solicitação vinculada à anterior; o sistema bloqueia a reserva cancelada durante a operação para impedir duas remarcações concorrentes. A lista de participantes é copiada como ponto de partida e o crédito da contribuição anterior é aplicado ao novo valor devido. Apesar do crédito, a nova solicitação repete geração, assinatura, envio, conferência documental e aprovação, mantendo evidências específicas do novo horário.

**Figura 7 – Diagrama de atividades do cancelamento e da remarcação.**
**Fonte:** Elaborado pelos autores (2026).

A Figura 8 representa a conferência no dia de uso. A Segurança consulta uma reserva aprovada, registra presença e pode incluir pessoa adicional antes da liberação. O CourtTime compara o número de presentes às vagas já pagas; se houver diferença, o pagamento é registrado na Secretaria ou na portaria, conforme o acordo operacional. A quadra somente é liberada quando existem ao menos dez presentes e nenhum valor pendente. O pagamento permanece no histórico mesmo que uma pessoa deixe o local, permitindo a substituição sem cobrança duplicada. Pagamentos recebidos na portaria geram uma confirmação posterior de repasse pela Secretaria. A liberação grava responsável e instante e encerra novas alterações de participantes e presença.

**Figura 8 – Diagrama de atividades da liberação da quadra.**
**Fonte:** Elaborado pelos autores (2026).

## Capítulo V — Implementação e arquitetura

A Figura 9 sintetiza a arquitetura implementada. O CourtTime é uma aplicação web monolítica em Java 17 e Spring Boot, com páginas Thymeleaf renderizadas no servidor. O Spring Security mantém sessão, autenticação e autorização por perfil, e os controladores Spring MVC encaminham as operações aos serviços de aplicação. Esses serviços concentram regras transacionais e bloqueios concorrentes, acessam o PostgreSQL por Spring Data JPA e Hibernate e usam Apache PDFBox para preencher o formulário institucional preservado. PDFs enviados ficam em diretório privado e são acessados por serviço, não como conteúdo público direto. Gov.br, o validador do ITI e o WhatsApp aparecem como serviços usados manualmente pelas pessoas; o código analisado não implementa integração automática com eles.

**Figura 9 – Arquitetura lógica e tecnológica do CourtTime.**
**Fonte:** Elaborado pelos autores (2026).

O formulário oficial apresenta uma divergência de redação conhecida. A modelagem registra o comportamento implementado e conserva o documento sem alteração, pois a decisão de modificar o formulário depende de autorização institucional do grupo. Essa escolha mantém a rastreabilidade entre a documentação técnica e a versão efetivamente utilizada pelo sistema.
