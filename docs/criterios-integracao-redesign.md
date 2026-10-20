# Critérios técnicos para integrar o redesign

Este documento é um contrato de integração, não uma proposta visual. Ele foi
levantado sobre `main` no SHA `436b262` e deve ser usado ao transportar um
protótipo aprovado para os templates Thymeleaf reais.

## Limites da alteração visual

- Manter Spring MVC, Thymeleaf e formulários HTML renderizados no servidor.
- Não mover autorização, validação, estado ou cálculo para JavaScript.
- Não criar rota, controller ou formulário paralelo para uma ação já existente.
- Preservar a hierarquia de perfis configurada no `SecurityConfig`: `/admin/**`
  é do administrador, `/secretaria/**` da secretaria, `/seguranca/**` da
  segurança e `/solicitacoes/**` do aluno.
- Não esconder com CSS uma ação que o servidor deveria proibir. O backend
  continua sendo a autoridade; a tela mostra somente ações válidas para a etapa.
- Não adicionar dados de exemplo aos templates. Dados demonstrativos pertencem
  exclusivamente ao banco descartável.

## Contratos Thymeleaf e de segurança

1. Todo formulário mutável usa `method="post"`. O token CSRF gerado pelo Spring
   Security deve continuar dentro do formulário enviado.
2. Logout permanece `POST /logout`; não deve virar link `GET`.
3. Expressões de ação com identificadores (`th:action`) e URLs privadas
   (`th:href`) não podem ser substituídas por rotas fixas.
4. IDs vindos da URL nunca autorizam acesso por si mesmos. As páginas do aluno
   continuam tratando recurso alheio como 404; páginas dos demais perfis
   continuam dependendo da autorização do servidor.
5. Downloads de documento continuam usando os endpoints privados do aluno,
   secretaria ou administração. Nunca apontar diretamente para diretório ou
   chave de armazenamento.
6. Mensagens de sucesso e erro continuam vindo de `status`, `erro`,
   `BindingResult` ou exceções tratadas. Não renderizar sucesso antes da resposta
   do servidor.
7. O PDF enviado deve continuar sendo chamado de documento enviado ou original;
   a interface não pode afirmar que a assinatura foi validada automaticamente.

## Nomes de campos que não podem mudar isoladamente

Estes `name` são contratos de binding dos controllers. Uma mudança exige ajuste
coordenado do form Java, controller e testes — não apenas do HTML.

| Fluxo | Campos preservados |
|---|---|
| Login | `email`, `password` (o campo de senha tem `id="senha"`) |
| Solicitação | `data`, `horarioInicio`, `finalidade` |
| Participante | `nomeCompleto`, `rmOuRg`, `categoria` |
| Documento | `arquivo` |
| Devolução documental | `justificativa` |
| Contribuição | `valor`, `observacoes` |
| Ajuste da contribuição | `novoValor`, `novasObservacoes`, `motivo` |
| Decisão administrativa | `motivo` para rejeição; `motivoCancelamento` para cancelamento |
| Horário semanal | `diaSemana`, `horarioInicio`, `horarioFim` |
| Bloqueio | `data`, `horarioInicio`, `horarioFim`, `motivo` |
| Usuário | `nome`, `email`, `perfil`, `senha`, `confirmacaoSenha` |
| Redefinição de senha | `novaSenha`, `confirmacaoSenha` |

Os valores dos enums enviados também permanecem os do backend: perfis
`ALUNO`, `ADMINISTRADOR`, `SECRETARIA`, `SEGURANCA`; categorias `ALUNO`,
`OUTRO`; e dias `SEGUNDA` a `DOMINGO`.

## Validação e mensagens

- Cada controle precisa de `label` associado por `for`/`id`.
- Erro de campo deve ficar próximo do controle, manter o texto retornado pelo
  backend e definir `aria-invalid="true"` quando aplicável.
- Alertas de operação precisam ser anunciáveis (`role="alert"` ou região viva
  equivalente) sem depender apenas de cor.
- Campos obrigatórios no HTML ajudam o usuário, mas não substituem as regras do
  servidor. O teste deve conseguir remover `required` e ainda obter uma recusa
  compreensível.
- Limites atuais: finalidade 300 caracteres; nome de participante 150; RM/RG
  30; motivos de bloqueio 200; senhas de 8 a 72 caracteres; PDF até 10 MB.
- A regra documental é de dez **alunos**, não dez participantes no total.

## Calendário e seleção de horário

- A consulta continua em `GET /quadra?data=AAAA-MM-DD` e o formulário em
  `GET /solicitacoes/nova?data=AAAA-MM-DD`.
- O valor enviado de horário é `HH:mm` em `horarioInicio`; a duração de uma
  hora é definida no backend e não deve virar campo editável.
- A data mínima exibida é amanhã em `America/Sao_Paulo`. O servidor revalida a
  antecedência, a grade ativa, bloqueios, conflitos e horários vencidos.
- Horários indisponíveis não podem parecer selecionáveis. Uma mudança visual do
  calendário não pode pré-reservar nem prometer disponibilidade.
- Datas, horários e estados devem ser lidos dos atributos do modelo. Não
  duplicar regras de disponibilidade em JavaScript.

## JavaScript e formulários

O sistema atual conclui o fluxo sem JavaScript. Caso um comportamento aprovado
no protótipo exija script:

- manter submissão HTML como caminho funcional;
- usar JavaScript como melhoria progressiva, sem fabricar estado de negócio;
- não remover `name`, `value`, `action`, `method` nem token CSRF;
- não armazenar senha, identificação, PDF ou contribuição no navegador;
- impedir envio duplo apenas como auxílio visual; a idempotência continua no
  serviço e no banco;
- atualizar os testes por papel/label/texto, nunca por classe de aparência.

## Responsividade e acessibilidade

Critérios mínimos em 390, 430, 768 e 1440 px:

- sem rolagem horizontal global; tabelas extensas podem ter contêiner próprio
  ou representação móvel com os mesmos dados;
- um `h1` por página, ordem de títulos coerente e `lang="pt-BR"`;
- link “Pular para o conteúdo” como primeiro foco, foco visível e ordem de Tab
  compatível com a ordem visual;
- ações com nome acessível que descreva o efeito; alvos móveis com altura
  confortável; estado desabilitado distinguível sem depender só de cor;
- contraste WCAG AA para texto, controles, foco, erro e sucesso;
- cabeçalhos de tabela associados às células e rótulos preservados quando a
  tabela vira cartões;
- conteúdo crítico não pode existir somente em `title`, hover ou ícone.

## Verificação obrigatória antes de integrar

1. Executar testes direcionados dos controllers afetados.
2. Executar `./mvnw clean verify` com PostgreSQL 16 isolado.
3. Executar `qa/playwright/jornada-quatro-perfis.cjs` contra a aplicação real.
4. Executar `qa/playwright/auditoria-acessibilidade.cjs` e revisar toda violação,
   inclusive quando não for séria/crítica.
5. Comparar capturas nos quatro viewports, cobrindo ao menos login, consulta da
   quadra, participantes, documentos, filas da secretaria, análise
   administrativa e consulta da segurança.
6. Repetir os cenários negativos de CSRF, IDOR, upload, concorrência, operação
   duplicada, cancelamento e invalidação de sessão.
7. Confirmar no diff que entidades, serviços e regras institucionais não foram
   alterados inadvertidamente por uma entrega visual.
