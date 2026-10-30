# Simplificação final e guia de código para a banca

Revisão de 30/10/2026. [PR #89 em rascunho](https://github.com/arthur-amancio/CourtTime/pull/89), branch `refactor/simplificacao-final-courttime`, base `docs/modelagem-uml-profissional` em `20e376ab616f6f2f913a20ae49e645f6a8938691`. Sem merge ou mudança de regras institucionais. Este documento técnico não substitui o TCC congelado.

## Inventário e decisão de escopo

Medição por arquivos UTF-8 e linhas físicas de `src/main/java`, incluindo comentários/brancos, separados dos demais materiais. Não é uma contagem de comandos executáveis. Varredura de declarações, métodos, referências e consultas complementada por revisão dirigida dos nove serviços indicados, controllers, persistência e recursos. Não constitui prova de ausência de todo defeito ou auditoria exaustiva de segurança.

| Medida | Base | Refatoração |
|---|---:|---:|
| Arquivos/tipos Java de produção | 102 | 101 |
| Classes / records / enums / interfaces / anotação | 59 / 19 / 10 / 13 / 1 | 59 / 18 / 10 / 13 / 1 |
| Linhas físicas Java de produção | 9.234 | 9.196 |
| Linhas Java não vazias | 7.953 | 7.918 |
| Controllers / ControllerAdvice / Services / entidades / repositories | 12 / 3 / 15 / 12 / 12 | iguais |
| Arquivos Java de teste / linhas físicas | 50 / 13.075 | 50 / 13.129 |
| Templates HTML / linhas | 32 / 3.567 | iguais |
| CSS / linhas | 1 / 1.182 | iguais |
| JavaScript do produto / linhas | 3 / 223 | iguais |
| Dependências diretas Maven | 7 de execução + 2 de teste | iguais |

Pacotes finais, contando arquivos: aplicação 1, config 3, controller 23 (inclui sete forms), entity 21 (inclui enums), repository 12, security 5, service 34 (inclui DTOs), storage 2. Não somar testes, `qa/`, documentação, binários ou `target/` às linhas de produção. Os números não representam uma meta de redução percentual.

### Alterações efetivas

1. **Leitura da portaria:** `ConsultaSegurancaService.detalhar` carrega os participantes uma vez, utiliza o tamanho dessa lista e a entrega à sobrecarga interna de `PortariaService.resumir`. Antes havia duas leituras completas e um `count`; agora uma leitura completa, sem o `count` redundante. Permanecem o filtro de reserva autorizada, os dados mínimos do DTO e as verificações de perfil/data. Operações de escrita continuam obtendo os dados atuais sob lock.
2. **Crédito da remarcação:** `ContribuicaoApmService.registrar` busca a contribuição de origem uma vez e reutiliza o objeto no cálculo e no vínculo. Preservados crédito aplicado, diferença, excedente sem destino e histórico. A consulta duplicada foi demonstrada por teste que falhou antes e passou depois.
3. **Consulta JPA:** `ReservaRepository.listarAutorizadas` substitui duas consultas literalmente iguais por setor. Administração e Segurança continuam com autorização nos serviços e projeções distintas. Todos os consumidores Java e testes foram atualizados; nenhuma URL mudou.
4. **Formulários MVC:** cinco operações de `AdminQuadraController` e `ParticipanteReservaController` passam a preparar e devolver o formulário com erro em um único ponto. `BindingResult`, vínculos, identificadores, mensagens, sucesso/redirecionamento e recusa de edição permanecem. Não foi criada camada genérica de formulários.
5. **DTO duplicado:** removido `UsuarioEdicao.java`, idêntico a `UsuarioResumo`. `AdministracaoUsuarioService.obterParaEdicao` reutiliza `resumir`, eliminando o conversor duplicado; controller e testes recebem `UsuarioResumo`. São apenas dados de leitura, sem senha/hash. `EdicaoUsuarioForm` e `RedefinicaoSenhaForm` permanecem separados e as mutações continuam autorizadas no serviço.
6. **Impedimento da CI:** o passo da jornada não herdava `COURTTIME_DB_USERNAME` definido apenas na inicialização. Logs de #86/#87 comprovavam tentativa de conexão como `undefined`. O passo agora recebe o usuário sintético do runner, e esta branch integra a lista de branches testadas pelo workflow de navegador.
7. **Validação de perfil:** `ReservaService` reutiliza `Permissao.exigir` em vez de manter outra implementação. Um teste reproduziu que uma conta ativa sem ID era aceita na listagem pelo serviço antigo; agora é recusada antes de consultar o repository, conforme o contrato existente de conta persistida. Não se afirma exploração por HTTP: a resolução da identidade autenticada é uma proteção separada já existente.

Nenhuma entidade, coluna, constraint, associação, dependência, regra de preço ou arquivo institucional foi removido. A redução de 38 linhas é consequência; os ganhos principais são retirar leituras, consultas e conversões repetidas e manter o fluxo localizável.

### O que foi mantido e por quê

`ReservaService` mantém a coordenação transacional da aprovação/remarcação; `ParticipanteReservaService` mantém revisão documental e mínimo; `DocumentoReservaService` mantém validação/armazenamento/rollback; `AdministracaoQuadraService` mantém locks e proteção das reservas aprovadas. `AdministracaoUsuarioService` preserva auditoria, versão de acesso, último administrador e proibição de promoção indevida. Métodos de até cerca de 60 linhas foram tratados como pontos de leitura, não automaticamente como defeitos.

`FormularioReservaPdf` conserva coordenadas, continuação de participantes e tratamento da fonte do modelo institucional. `CalendarioDisponibilidadeService` conserva alinhamento à grade, sobreposição, curto-circuito e fuso. `DocumentoStorage` permite testar IO e centralizar a política privada. DTOs diferentes de Administração e Segurança protegem responsabilidades distintas; não foram fundidos só para reduzir classes.

Os três scripts web têm referências nos templates e funções de melhoria progressiva. Layout/ícones já usam fragments. Não se comprovou um arquivo CSS/JS inteiro morto; seletores dinâmicos e estados acessíveis impedem concluir desuso apenas por busca textual. Não houve remoção de estilos ou comportamento. `findByDocumento` e `countByReservaAndCategoria` são candidatos P2 sem consumidores textuais encontrados, inclusive nos testes; não foram priorizados sobre os fluxos afetados. Isso não torna seus repositories, usados pelo Spring/JPA, arquivos desnecessários. As nove dependências diretas têm finalidade na stack/testes; não houve atualização oportunista.

## Validação e evidências

Resultados completos e versão final: consultar o checkpoint no topo de [CONTINUIDADE-MISSAO.md](tcc/CONTINUIDADE-MISSAO.md). Comandos e logs locais em `C:\Users\arthur.francisco\Documents\CourtTime-Simplificacao-20261030`; os relatórios Java não devem ser somados entre execuções como se fossem testes distintos.

Rodadas pequenas: 40 testes de serviços; 36 testes de serviços/PostgreSQL; 30 MVC; 34 de usuários antes e 34 depois da retirada do DTO; 48 de permissão/reserva/PostgreSQL, todos sem erros/falhas/skips. A caracterização das consultas produziu quatro falhas esperadas antes da mudança (23 execuções) e passou depois. O teste de aluno sem ID também falhou antes/passou depois. Base completa: 477 executados, 476 aprovados e um erro ambiental de symlink no Windows. Revisão final `5eb8c82`: `mvnw.cmd clean verify`, **479 executados, 478 aprovados, zero falhas, um erro ambiental de symlink, zero skips**, 1 min 47 s; 63 integrações PostgreSQL e três jornadas HTTP aprovadas. O build local integral permanece reprovado por esse erro, sem mascaramento.

Revisão final de código, `5eb8c8252b51846c1d6226097f4265bad2e8c416`: [CI](https://github.com/arthur-amancio/CourtTime/actions/runs/41053267893), **479 aprovados em cada PostgreSQL 16/18**, zero falhas/erros/skips, logs dos dois jobs consultados. [Navegador](https://github.com/arthur-amancio/CourtTime/actions/runs/41053259435) aprovado: 367 jornada, 49 navegação, 285 interações, 330 layout e 86 finalização, total **1.117 verificações**, mais 44 varreduras axe sem violações. A suíte também exercita edição de usuário, redefinição da senha e encerramento da sessão antiga. Não confundir 1.117 verificações com testes Java nem 44 varreduras com certificação WCAG.

Execução local final no mesmo SHA: `npm run test:qa`, código de saída **0**, mesmos resultados e 113 PNGs. Conferidas imagens de Segurança em 390/1440 px, participantes e horários; sem alteração de HTML/CSS/JS. [Evidências persistentes da CI](https://github.com/arthur-amancio/CourtTime/tree/evidencias/ui-v2/refactor_simplificacao-final-courttime/5eb8c82): diretório consultado, 163 arquivos (130 JPGs, 26 PNGs, seis JSONs e índice). A aplicação e o cluster PostgreSQL locais exclusivos da missão foram encerrados; os dados sintéticos e as provas foram preservados. A revisão anterior `69833fc` também passou, mas os números acima pertencem à revisão posterior, verificada novamente.

Nenhum teste foi removido/desativado para ocultar falhas. O empacotamento com `-DskipTests` serve apenas para iniciar a instância de navegador depois da suíte executada; não é substituto de `clean verify`. O erro local de symlink é relatado separadamente da CI Linux aprovada. As fixtures de navegador ajustam a data apenas no banco E2E autorizado: isso é preparação sintética de teste, não operação normal de remarcação demonstrada à banca.

## Guia de leitura em vinte minutos

### Estrutura e classes principais

Em `src/main/java/br/com/courttime/`: `controller` recebe HTTP e prepara a tela; `controller/form` contém entradas validadas; `service` aplica regras e produz DTOs de consulta; `repository` consulta/persiste JPA; `entity` mapeia tabelas; `security` representa identidade/permissões e revalida sessões; `storage` protege PDFs; `config` conecta inicialização e segurança. Recursos HTML ficam em `src/main/resources/templates`; CSS/JS em `static`. A aplicação continua Spring MVC com Thymeleaf, sem frontend separado.

### Do formulário à reserva

Abrir `SolicitacaoReservaController.criar`, `ReservaService.criarSolicitacao`, `ReservaRepository` e `entity/Reserva.java`. O clique envia POST; MVC associa `SolicitacaoReservaForm`, valida entradas e disponibiliza `BindingResult`. O controller usa o usuário da sessão e chama o serviço; o serviço confere perfil, antecedência, período de uma hora, finalidade e disponibilidade. Salva `PENDENTE_DOCUMENTACAO` com `rascunho=true`, depois redireciona aos detalhes. Não é uma API REST nova.

Trecho real de `ReservaService.criarSolicitacao`:

```java
reserva.setFinalidade(finalidadeNormalizada);
reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
reserva.setRascunho(true);
return reservaRepository.save(reserva);
```

O encadeamento é Controller → Service → Repository → PostgreSQL. A entidade é o modelo de dados usado nesse caminho; Hibernate traduz o mapeamento para SQL. Exemplo real de `Reserva`:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "quadra_id", nullable = false)
private Quadra quadra;
```

Uma reserva referencia uma quadra e um solicitante; vários participantes/documentos referenciam a reserva. Chaves estrangeiras asseguram associações. `rascunho` não substitui a contagem real; `ParticipanteReservaService` formaliza a lista ao atingir dez ALUNO/OUTRO e protege revisão documental. Documentação, contribuição, aprovação e liberação continuam sendo etapas distintas.

### MVC e consultas depois da simplificação

No `ParticipanteReservaController.adicionar`, o serviço é chamado somente sem erro de binding. Exceção de entrada entra em `BindingResult`; erro de estado redireciona com mensagem; ao final há uma única preparação/retorno do formulário. `form`, `solicitacao`, `vinculos`, `participanteId` e `acaoFormulario` continuam com os nomes esperados pelo template. `th:object`, `th:field` e `th:action` conectam esses dados ao Spring MVC; o token CSRF continua no POST.

`ReservaRepository.listarAutorizadas(StatusReserva status, LocalDate dataAtual)` filtra por situação/data, faz `join fetch` de quadra/solicitante e ordena data, início e ID. Reutilizar consulta não significa reutilizar permissões: os serviços exigem seus perfis e a Segurança recebe DTO limitado. A aprovação continua travando reserva e quadra e revalidando conflito; uma consulta antiga do calendário não reserva o horário.

### Segurança e regras que todos devem explicar

`SecurityConfig` restringe rotas, usa sessão/form login e BCrypt; `Permissao` e consultas por titularidade protegem os serviços. Não transformar mutações em GET, não aceitar perfil enviado pelo cliente como autorização e não trocar `orElseThrow` por `get` sem tratar ausência. Versão de acesso encerra sessões antigas após mudanças relevantes na conta.

PDFs têm limites, validação técnica, chave aleatória e acesso privado autorizado. Prefixo PDF não autentica assinatura; conferência externa/humana continua necessária. `BigDecimal` representa R$ 2,00 por participante/hora. A Secretaria registra contribuição presencial; o Administrador aprova; a Segurança confere o uso no dia. **Ao acrescentar alguém na portaria, `ParticipanteReserva.marcarAdicionadoNaPortaria` já registra PRESENTE**; não confundir com o cadastro inicial do aluno. Essa precisão corrige uma descrição técnica anterior, sem alterar o comportamento existente ou o TCC congelado.

A paga → A fica AUSENTE → B entra: capacidade paga reutilizada, histórico de A e do pagamento preservado, sem estorno/cobrança duplicada. C excedente gera R$ 2,00. Liberação exige dez presentes e saldo devido zero; bloqueia alterações efetivas de presença, inclusões e novos complementos. Confirmar depois um repasse de recebimento anterior é operação separada da Secretaria. Remarcação usa origem elegível, crédito e novo documento/conferência/aprovação. Dez participantes totais continua divergindo de dez alunos no termo histórico; não foi inventada uma decisão institucional.

### Relação com as aulas e divisão do treino

Referências consultadas: `Revisao_SistemaWebII` em `4a9fe9ad3c12d471d27125180adbbbc7f1251a0d` (ProdutoController/Service/Repository); `CadastroAlunoNpN` em `ed249f612c7adc4ede23f376a3e341cf39f66439` (MatriculaService/DTO, formulário e ManyToOne); `EtecWeb` em `b708715c75f797ec10c2520ec1a00313489cc500` (AlunoController/Repository); `Hospital-ERP` em `b3ba9566b513524615717b1445cc420ab92dac82` (PacienteController e ConsultaService). Conservados conceitos de construtor, CRUD, JPA e templates; não copiados GET de exclusão, `Optional.get` sem validação ou ausência de autorização. Nenhum dado/configuração desses projetos foi copiado para o CourtTime.

Treino proposto: 0–2 min problema e quatro perfis; 2–5 criação/rascunho; 5–8 Controller/Form/Thymeleaf; 8–11 Repository/JPA/PostgreSQL; 11–14 permissões e locks; 14–17 contribuição/portaria; 17–20 abrir o diff e explicar lista reutilizada, consulta unificada, DTO removido e retorno de erro. Todos devem explicar o fluxo; isso não atribui autoria individual de módulos.

## Integração futura e congelamento

Ordem: main → #85 → #86 → #87 → #88 → #89. Neste snapshot todos continuam sem merge. Não apontar #89 para main enquanto isso incluir novamente a cadeia. Após cada merge autorizado, verificar se o GitHub retargetou o próximo PR; se necessário, atualizar apenas a base, sem force push. Conferir novamente diff, conflitos e CI antes de prosseguir. As falhas antigas de navegador em #86/#87 foram diagnosticadas; a correção está em #89 e não torna retroativamente verdes os checks antigos.

Comandos de leitura úteis: `git fetch origin`, `git log --oneline --graph --decorate`, `git merge-base --is-ancestor <base> <head>`, `git diff --stat <base>...<head>` e `git diff <base>...<head> -- src pom.xml .github`. Se a equipe utilizar GitHub CLI, conferir `gh pr view 89 --repo arthur-amancio/CourtTime --json baseRefName,headRefOid,mergeable,statusCheckRollup`, `gh pr diff 89 --repo arthur-amancio/CourtTime --name-only` e `gh pr checks 89 --repo arthur-amancio/CourtTime`; são instruções futuras, não ferramentas instaladas/executadas nesta missão. No teste, conferir o banco realmente conectado antes de habilitar `COURTTIME_POSTGRES_TEST`; usar nome dedicado contendo `test` e PostgreSQL descartável. Reexecutar Maven/integrações e `npm run test:qa` depois de mudança real de base/código, com resultados vinculados ao SHA. Nenhum comando de merge foi executado nesta missão.

Não houve mudança de entidades/relacionamentos/DER. Interfaces Java internas alteradas: consulta do repository, retorno `UsuarioResumo` em `obterParaEdicao`, conversor privado de contribuição e sobrecarga interna do resumo da portaria. Os identificadores antigos não aparecem nos fontes Draw.io pesquisáveis nem no mapa do código; não é necessário redesenhar o núcleo persistente. Materiais Astah nativos externos não foram reexportados. TCC/PDFs/modelos congelados preservados; eventual atualização de figura acadêmica pertence a revisão separada.

Condições objetivas para encerrar o MVP: CI obrigatória e testes dos fluxos afetados aprovados no SHA a integrar; nenhuma regressão crítica conhecida; diff final sem alterações inesperadas; cadeia integrada somente com autorização; ambiente sintético ensaiado e limitações institucionais explicitadas. **P0:** qualquer novo teste obrigatório falho/conflito/regressão bloqueia congelamento. **P1:** pareceres institucionais, homologação e validação após mudança de base; privilégio de symlink impede afirmar aprovação integral local Windows. **P2:** paginação/batching das listagens se volume real justificar, prova em projetor e sincronização futura do material didático. Não há meta de eliminar classes adequadas.
