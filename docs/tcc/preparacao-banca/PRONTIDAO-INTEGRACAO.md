# CourtTime — prontidão para integração

**Consulta:** 29/10/2026. **Modalidade:** somente leitura dos PRs, histórico, arquivos e logs de CI; publicação restrita a estes materiais de preparação na branch documental. Nenhum merge, retarget, rebase ou alteração de código foi executado nesta missão.

## Estado consultado antes deste pacote documental

| PR | Base → branch | HEAD consultado | Escopo do diff contra a base | Estado GitHub |
|---|---|---|---|---|
| [#85](https://github.com/arthur-amancio/CourtTime/pull/85) | `main` → `feat/refatoracao-permissoes` | `fe21d898f9082acb30c9f71700fc7d7872a2ef5a` | 17 arquivos em `src/`; permissões e tratamento de acesso | Aberto, rascunho; `mergeable=true` |
| [#86](https://github.com/arthur-amancio/CourtTime/pull/86) | `feat/refatoracao-permissoes` → `feat/portaria-liberacao-acrescimos` | `4fb7a06c8343234487bd15260c67e31a3b765aaa` | 61 arquivos: 47 em `src/`, cinco em `qa/`, sete em `docs/`, README e um workflow | Aberto, rascunho; `mergeable=true` |
| [#87](https://github.com/arthur-amancio/CourtTime/pull/87) | `feat/portaria-liberacao-acrescimos` → `chore/ui-v2-verificacao-portaria` | `24d99feda3cd9b070af9f4ddb5e327a81d1aed27` | Um arquivo em `docs/`, mapa do código | Aberto, rascunho; `mergeable=true` |
| [#88](https://github.com/arthur-amancio/CourtTime/pull/88) | `chore/ui-v2-verificacao-portaria` → `docs/modelagem-uml-profissional` | `47b0a5333fc98a5e600aa79335896e4334972cc7` | 113 arquivos: 112 em `docs/` e um em `scripts/`; documentação/modelagem e SQL diagnóstico | Aberto, rascunho; `mergeable=true` |

`main` consultada: `4df490729734d9f45830e15be241cdb66c3f112f`. A relação ancestral foi conferida com `git merge-base --is-ancestor` para main→85→86→87→88: quatro resultados positivos. A cadeia está íntegra. `mergeable=true` indica ausência de conflito reportado naquele momento; não equivale a aprovação, cumprimento de proteção da branch ou autorização para merge.

**Atualização deste pacote:** o commit de preparação acrescenta os quatro arquivos deste diretório e atualiza `CONTINUIDADE-MISSAO.md`. Por isso o HEAD e a quantidade total de arquivos de #88 avançam após o snapshot acima. Não há mudança correspondente nos HEADs de #85–#87. O SHA de publicação é obtido por `git log -1` ou pela página do PR; os resultados de CI abaixo pertencem especificamente ao SHA consultado.

## CI: evidência e alcance

| PR | CI consultada | Proteção QA consultada | Evidência |
|---|---|---|---|
| #85 | #286, concluída com sucesso | #84, sucesso | [CI](https://github.com/arthur-amancio/CourtTime/actions/runs/39782510401), [proteção QA](https://github.com/arthur-amancio/CourtTime/actions/runs/39782510404) |
| #86 | #313, concluída com sucesso | #103, sucesso | [CI](https://github.com/arthur-amancio/CourtTime/actions/runs/40708650204), [proteção QA](https://github.com/arthur-amancio/CourtTime/actions/runs/40708650293) |
| #87 | #314, concluída com sucesso | #104, sucesso | [CI](https://github.com/arthur-amancio/CourtTime/actions/runs/40810403653), [proteção QA](https://github.com/arthur-amancio/CourtTime/actions/runs/40810402372) |
| #88 | #319, concluída com sucesso, SHA `47b0a53` | #109, sucesso | [CI](https://github.com/arthur-amancio/CourtTime/actions/runs/40868642896), [proteção QA](https://github.com/arthur-amancio/CourtTime/actions/runs/40868642885) |

Os logs dos jobs PostgreSQL 16 (`123429717933`) e 18 (`123429718334`) da CI #319 foram efetivamente consultados nesta missão: **477 testes, zero falhas, zero erros e zero skips em cada job; BUILD SUCCESS**. O passo condicional de resumo de falhas aparece ignorado porque não houve falha; isso não representa testes ignorados. A execução repete a mesma suíte em duas versões do banco.

As verificações de proteção QA conferem travas da automação. Seu sucesso **não** significa execução da jornada completa de navegador na CI. A jornada de quatro perfis, as 1.122 verificações, as 44 varreduras axe e os 113 PNGs pertencem à execução anterior no SHA `02efd1599b2c33f1e45e145219b01a185c5425fa`, documentada no [relatório anterior](../revisao-20261029/ENTREGA-E-VALIDACAO.md).

Nenhuma suíte foi reexecutada nesta missão. O resultado do novo commit exclusivamente documental deve ser consultado separadamente; não se transfere o status verde do SHA anterior para ele por antecipação.

## Diffs, regressão e triagem de conteúdo público

As comparações abaixo foram executadas e não retornaram arquivos:

```powershell
git diff --name-only 4fb7a06 47b0a53 -- src qa pom.xml .github
```

Assim, #87 e #88 preservam a árvore funcional e a automação da base `4fb7a06`; seus acréscimos não são uma nova versão de domínio. O escopo observado de #85 corresponde à refatoração de permissões; #86 concentra os comportamentos de portaria/rascunho e respectivos testes. Não foram identificados arquivos fora desses escopos na classificação dos diffs. Isso é uma revisão de prontidão, não uma nova auditoria integral de cada regra de negócio.

Nas adições textuais de main até `47b0a53`, a busca por marcadores de chaves privadas, tokens GitHub conhecidos, chaves AWS no formato AKIA e URLs PostgreSQL com credenciais embutidas retornou **zero ocorrências**. O inventário de caminhos não mostrou novos `.env`, arquivos de configuração `application*.properties`, CSVs primários, chaves `.pem/.key` ou certificados privados `.p12/.pfx`. Os PDFs adicionados são o candidato acadêmico e exportações de modelagem. Dados individuais do questionário e diretórios privados de evidência não foram adicionados por esta missão.

**Limite:** triagem textual e inventário de caminhos não garantem ausência absoluta de informação sensível, não cobrem todo o histórico Git e não constituem OCR ou perícia de todos os binários. Os nomes dos autores e a identificação da instituição são conteúdo acadêmico intencional. Os materiais novos contêm identificadores de contas sintéticas e links com controle de acesso, sem senhas. O acesso aos arquivos privados do Drive continua sujeito às permissões existentes; não foi alterado.

## Plano para integração futura — não executado

**Condição inicial:** aprovação explícita futura para integrar e atendimento das proteções/revisões exigidas. A ordem é **#85 → #86 → #87 → #88**, sempre reavaliando o próximo diff contra a `main` já atualizada. Preferir merge com preservação dos commits da cadeia; squash ou rebase podem desfazer a relação ancestral usada pelos descendentes e exigem outra análise.

### 1. Capturar o estado e conferir o primeiro PR

Os comandos deste bloco são para a sessão futura autorizada; não foram usados para integrar nesta missão:

```powershell
git fetch origin
git status --short
gh pr view 85 --repo arthur-amancio/CourtTime --json state,isDraft,baseRefName,headRefName,headRefOid,mergeable,reviewDecision
gh pr diff 85 --repo arthur-amancio/CourtTime --name-only
gh pr checks 85 --repo arthur-amancio/CourtTime
```

Trabalhar em worktree limpa própria. Não modificar ou limpar o checkout original. Conferir HEAD, revisão humana e CI aplicável à base atual, sem assumir que os checks antigos representam a futura combinação com main. Se houver novo código, conflito ou mudança de base relevante, executar a validação proporcional em ambiente isolado antes da autorização efetiva de merge.

### 2. Integrar somente após a autorização específica

Exemplo restrito a #85, se seu HEAD continuar igual e houver aprovação:

```powershell
# SOMENTE APÓS AUTORIZAÇÃO FUTURA; NÃO EXECUTADO NESTA MISSÃO.
gh pr ready 85 --repo arthur-amancio/CourtTime
gh pr merge 85 --repo arthur-amancio/CourtTime --merge --match-head-commit fe21d898f9082acb30c9f71700fc7d7872a2ef5a
```

Não usar `--admin`, `--auto`, `--delete-branch`, `--squash`, rebase ou force push. Se o HEAD tiver mudado, interromper a integração daquele PR e revisar o novo SHA; não substituir a trava mecanicamente. A execução do comando depende também de configurações e proteções reais do repositório.

### 3. Reavaliar o próximo PR depois de cada merge

Depois de #85, conferir se GitHub já ajustou a base de #86. Caso não tenha ajustado, a sessão de integração autorizada poderá executar:

```powershell
# APENAS NA INTEGRAÇÃO FUTURA, DEPOIS DO MERGE DE #85.
gh pr edit 86 --repo arthur-amancio/CourtTime --base main
gh pr diff 86 --repo arthur-amancio/CourtTime --name-only
gh pr checks 86 --repo arthur-amancio/CourtTime
```

Repetir a conferência de HEAD, diff, revisão, conflito e CI para #86; somente então integrá-lo com merge preservando histórico e trava do HEAD efetivamente revisado. Depois fazer o mesmo para #87 e, por último, #88. Não retargetar todos de uma vez e não excluir as branches das bases antes de revisar os descendentes.

**Atenção à CI:** se não surgir execução para a nova base, ou se os checks listados ainda forem anteriores à alteração relevante, não considerá-los comprovação da combinação nova. Verificar o SHA/ref de merge testado; quando necessário, validar o candidato de integração em nova worktree e PostgreSQL descartável. Se houver falha Windows de symlink, registrá-la separadamente e usar evidência da CI adequada, sem afirmar aprovação local integral. Maven, integração PostgreSQL e navegador só devem ser repetidos quando a mudança/combinação justificar; esta missão não os repetiu.

### 4. Verificação final após a futura integração

Confirmar os quatro PRs efetivamente integrados, ancestralidade dos SHAs funcionais na main, CI do estado final, ausência de duplicação de código e preservação dos arquivos acadêmicos. Gerar registro de SHAs, responsáveis e autorizações. Não executar deploy ou migração de banco real como consequência implícita dos merges. Registros legados anteriores ao campo `rascunho` devem ser avaliados em cópia, pelo [diagnóstico somente leitura](../../../scripts/diagnostico-rascunhos.sql), antes de qualquer plano de migração autorizado.

## Avaliação e pendências

| Prioridade | Item | Encaminhamento |
|---|---|---|
| P0 condicional | CI/conflito/diff inesperado que venha a surgir após mudança de base ou HEAD | Bloqueia o merge afetado até revisão e validação. Não há conflito confirmado no snapshot consultado. |
| P1 | Revisões e autorização de integração; PRs ainda em rascunho | Solicitar na etapa humana apropriada, sem interpretar este relatório como aprovação. |
| P1 | Regras institucionais e parecer acadêmico | Resolver a [tabela do orientador](REVISAO-ORIENTADOR.md) antes de declarar homologação. |
| P1 | Legado anterior a `rascunho` se houver banco existente a implantar | Diagnosticar cópia; preservar dados e preparar migração específica se necessária. Nenhum banco real foi consultado. |
| P1 | Ensaio da demonstração | Preparar os três cenários em ambiente sintético e cronometrar. O roteiro não é execução comprovada. |
| P2 | Prova física das figuras e material de slides em formato nativo | Realizar conforme recursos e orientação da banca; o roteiro em Markdown está disponível. |

**Conclusão técnica:** cadeia preservada, CI consultada com sucesso e nenhum conflito reportado no momento da leitura; materiais preparados para revisão humana e posterior integração autorizada. Não se declara merge pronto por aprovação automática, homologação institucional ou deploy autorizado.
