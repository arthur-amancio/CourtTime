# Bloqueios semanais — manutenção e publicação futura

Implementação candidata baseada no PR #94. **Não publicada no Render e não aplicada ao Neon.**
As revisões acadêmicas #92/#93/#95/#96 e a monografia principal de 98 páginas permanecem preservadas.

## Evidências da entrega

[PR #97](https://github.com/arthur-amancio/CourtTime/pull/97): Maven/CI com 550 casos Java
executados (553 declarados, três opcionais S3 ignorados); navegador com 1176 verificações
aprovadas e 48 varreduras axe sem violações encontradas; migração com dez verificações isoladas.
SHAs e escopos no [manifesto](bloqueios-semanais-evidencias.json).
Capturas sintéticas conferidas: [desktop](evidencias-bloqueios-semanais/lista-desktop.png) e
[celular](evidencias-bloqueios-semanais/lista-celular.png). O texto de script no motivo é
uma sonda de escape exibida como texto, sem execução; não é dado de aluno nem funcionalidade.

Revisão de estabilização de 05/11: HEAD `345340d` conferido sem defeito funcional confirmado.
Código, JavaDocs, testes e SQL preservados; evidências acima reutilizadas por identidade de fontes.
Somente mapa/checklist e registro da revisão atualizados. Nenhuma suíte foi reexecutada nesta revisão.

## Regra e operação

Administração → Bloqueios → **Novo bloqueio semanal**. Escolher dia, início/fim,
vigência e motivo. Exemplo: segunda, 08h–10h, 02/11–18/12/2026, Educação Física.
A grade de funcionamento continua separada; nenhum horário oficial é criado pelo sistema.

- Um registro em `bloqueios_semanais_quadra` representa a regra inteira. Não há cron,
  geração de bloqueios futuros ou registros de exceção por ocorrência.
- Início/fim da vigência são inclusivos. O horário final é exclusivo: 08h–10h
  não impede 10h–11h; 08h30–09h30 afeta as duas horas que se sobrepõem.
- O horário deve terminar no mesmo dia e depois do início. Datas, dia, motivo
  obrigatório/até 200 caracteres e perfil administrativo são validados no servidor.
- Uma vigência já iniciada é permitida se ainda contiver ocorrência atual/futura.
  Vigência invertida, totalmente vencida ou sem o dia escolhido é recusada.
- Uma regra nova não pode atingir reserva aprovada a partir de hoje, inclusive na
  própria data de hoje. Não cancela reservas nem solicitações pendentes automaticamente.
- Solicitação e remarcação consultam a disponibilidade; a aprovação a revalida sob lock.
  Uma solicitação anterior ao cadastro da regra poderá ser recusada na aprovação.
- A trava pessimista da **mesma quadra** serializa aprovação, grade e cadastro/desativação
  de regras. O cadastro lê as aprovadas depois de obter essa trava. A consulta da tela
  não reserva vaga nem substitui a verificação transacional.
- Regras sobrepostas são combinadas, sem prioridade especial. Desativar uma regra
  inteira preserva o registro e não desativa outras regras ou bloqueios pontuais.
  Não há edição, reativação, feriados automáticos ou exceções por data nesta entrega.

## Onde manter

| Necessidade | Arquivo/método | Testes relevantes |
| --- | --- | --- |
| Persistência e ocorrência semanal | `BloqueioSemanalQuadra.ocorreEm` | `BloqueioSemanalQuadraTest`, integração de Administração |
| Vigência e proteção de aprovadas | `AdministracaoQuadraService.cadastrarBloqueioSemanal` | `AdministracaoQuadraServiceTest`, `AdministracaoQuadraPostgreSqlIntegrationTest` |
| Dia consultado e sobreposição | `CalendarioDisponibilidadeService.obterBloqueios`, `periodosSobrepostos` | `CalendarioDisponibilidadeServiceTest` |
| Aprovação concorrente | `ReservaService.aprovar`, `QuadraRepository.findByIdForUpdate` | `ReservaPostgreSqlIntegrationTest.aprovacaoERegraSemanalConcorrentesNaoPodemPersistirJuntas` |
| Formulário e autorização HTTP | `AdminQuadraController`, `BloqueioSemanalQuadraForm`, templates `admin/bloqueios/` | `AdminQuadraControllerTest`, Playwright semanal |
| Consulta sem gerar ocorrências | `BloqueioSemanalQuadraRepository.listarAtivosNaData` | PostgreSQL e navegador; contagem persistida permanece uma por regra |
| SQL manual aditivo | `scripts/deploy/20261105-bloqueios-semanais.sql` | Ensaio de migração em schema anterior de 12 tabelas + inicialização com `validate` |

Os JavaDocs das camadas principais explicam autorização, propriedade, revisão documental,
contribuição/crédito, presença, liberação e locks. A alteração didática preserva os fluxos
estáveis; getters/setters não receberam comentários triviais. Veja também [mapa-do-codigo.md](mapa-do-codigo.md).

## Reproduzir a validação

Preparar JDK 17, PostgreSQL descartável e variáveis pelo [guia cloud](desenvolvimento-cloud.md).
Conferir `SELECT current_database(), current_user;` antes de qualquer preparação/limpeza.
Nunca usar a base online, mesmo que só contenha dados fictícios.

```bash
# Banco dedicado com "test" no nome, efetivamente conferido antes de habilitar a suíte.
COURTTIME_POSTGRES_TEST=true ./mvnw clean verify

# Em OUTRO banco E2E novo, com as proteções/marcador e perfis do guia cloud:
cd qa/playwright
npm ci
npm run test:qa
# O último passo é test:bloqueios-semanais; também pode ser repetido após a jornada,
# se os dados necessários e o mesmo candidato estiverem preservados.
```

O teste semanal usa somente formulários/rotas da aplicação para escrever e consultas
SQL de leitura para confirmar isolamento, persistência e reserva sintética aprovada.
Executa quatro varreduras axe nas telas novas (desktop/390 px), sem certificar toda acessibilidade.
Os resultados reais desta entrega ficam no checkpoint e em `bloqueios-semanais-evidencias.json`.
Relatórios XML completos, senhas e dumps de teste não devem ser publicados.

## Migração e futura implantação — requer autorização separada

O Render mantém `ddl-auto=validate`. Não ligar `update`, não substituir o schema inicial
de 12 entidades e não executar a migração no boot. O script novo cria **somente** tabela,
identity, FK, checks e índice; não altera ou apaga os dados anteriores. Reaplicação falha
em vez de ignorar silenciosamente uma tabela existente.

Os itens abaixo são **ações futuras**, todos pendentes de autorização. A revisão não os executou.

### Integração

- [ ] Registrar autorização do proprietário, `main` e HEADs atualizados. Conferir as bases:
  #92 parte de #91; #93 de #92; #95 de #93; #96 de #95; #94 parte de #91; #97 de #94.
- [ ] Na missão autorizada, preservar commits: #85→#86→#87→#88→#89→#90→#91→#92→#93→#94→#95→#96→#97.
  Antes de integrar cada PR, confirmar que sua base anterior já está na main e ajustar
  a base do PR à main apenas nessa missão. Sem squash, rebase, force push ou exclusão de branches.
- [ ] Após cada etapa, conferir ancestralidade e diff esperado. No candidato final, comparar
  `src/main`, `src/test`, `pom.xml`, Dockerfile, `render.yaml` e QA com os SHAs do manifesto.
  Cumprir os checks exigidos; código divergente exige revisão e validação proporcional.
  HEAD documental sem checks próprios não deve ser apresentado como um novo runtime testado.
- [ ] Interromper antes da próxima integração em conflito, SHA novo não revisado, arquivo
  ausente, mudança inesperada ou check obrigatório falho. Não resolver apagando material acadêmico.

### Migração autorizada

- [ ] Identificar o JAR/commit atual e o candidato, o hash do SQL, as variáveis e o destino
  privado dos PDFs. Conferir `SELECT current_database(), current_user;` e endpoint/conta.
  Não registrar senhas no checklist nem apontar testes destrutivos ao banco online.
- [ ] Ensaiar primeiro em banco isolado restaurado do schema anterior: aplicar o SQL,
  conferir contagens/hashes das 12 tabelas e iniciar o JAR novo com `ddl-auto=validate`.
  Verificar cadastro/desativação, conflito, solicitação, aprovação, remarcação e PDF.
  Se o retorno ao JAR antigo for necessário, ensaiá-lo nessa cópia, com a tabela adicional vazia.
- [ ] Na janela autorizada, pausar o serviço e aguardar as transações em andamento terminarem
  antes do backup final. Guardar dump e PDFs referenciados, com hashes, em destino privado;
  testar restauração em destinos novos. Isso evita uma cópia incoerente entre banco e objetos.
- [ ] Confirmar o banco na sessão e a ausência da tabela com
  `SELECT to_regclass('public.bloqueios_semanais_quadra');`. Resultado não nulo exige investigar
  uma aplicação anterior; não executar novamente por tentativa.
- [ ] Só então executar uma vez, com conexão privada e operador autorizado:

  ```bash
  # NOME_CONFERIDO é o banco realmente verificado; não usar literalmente o marcador.
  PGOPTIONS='-c courttime.migration_database=NOME_CONFERIDO' \
    psql -v ON_ERROR_STOP=1 -f scripts/deploy/20261105-bloqueios-semanais.sql
  ```

  Sem confirmação ou com banco diferente, o script recusa antes de criar a tabela.
  A transação, `lock_timeout=5s` e `statement_timeout=30s` limitam a aplicação.
- [ ] Conferir tabela vazia, identity, FK, checks/índice e acesso do usuário da aplicação à
  tabela/sequence. Preservar as 12 tabelas e seus dados. Não executar o schema inicial,
  habilitar `update`, remover constraints ou conceder permissões amplas para resolver erro.

### Deploy manual e aceite operacional

- [ ] Manter plano Free, deploy automático desligado, `/healthz`, `validate`, credenciais
  existentes e bucket privado. A migração não habilita a funcionalidade sozinha:
  o runtime #94 desconhece a tabela nova, que deve continuar vazia antes da troca.
- [ ] Fazer deploy manual do SHA aprovado e conferir o SHA realmente exibido nos Events
  do Render. Aguardar readiness antes de reabrir operações; um link respondendo não prova
  que o commit correto foi implantado.
- [ ] Conferir quatro logins, acesso privado/bytes dos PDFs e registros anteriores.
  Em cenário sintético separado, cadastrar regra sem aprovada conflitante, verificar
  dia/vigência e horário adjacente, recusar POST indevido, desativar a regra inteira e
  confirmar que um pontual continua bloqueando. Preparar datas futuras pela interface.
- [ ] Registrar SHA, data, IDs sintéticos, resultado e responsável. Só após esse aceite
  declarar a recorrência publicada; testes na nuvem não são homologação da ETEC.

### Recuperação sem ignorar regras ativas

| Situação | Ação segura | Condição para reabrir |
| --- | --- | --- |
| Migração recusada/erro antes do COMMIT | Manter o serviço pausado e diagnosticar banco, permissão ou lock. A conexão encerrada desfaz uma transação não confirmada. | Confirmar o estado real do schema antes de nova tentativa. Não alterar o SQL às pressas. |
| Conexão caiu e não se sabe se houve COMMIT | Consultar `to_regclass`, colunas, FK, checks e índice. A existência da tabela exige comparar com o SQL revisado. | Estado conhecido e validado; não repetir o script ou apagar a tabela por suposição. |
| Novo JAR falhou e a tabela está vazia/sem regra ativa | Manter a tabela aditiva. Pode-se considerar o último JAR #94 já ensaiado com esse schema, preservando banco/bucket. | Autorização de retorno, conferência de dados/documentos e saúde/login do JAR restaurado. Sem DROP. |
| Há regra semanal ativa | **Não voltar ao #94 nem desativar regras apenas para viabilizar o retorno.** Ele ignoraria os períodos. Manter operações suspensas e corrigir o candidato ou usar outro artefato validado que compreenda as 13 entidades. | Recorrências continuam respeitadas. Se a instituição decidir encerrar regras, a decisão é explícita, registrada e executada pela interface antes de reconsiderar o retorno. |
| Banco ou PDF perdeu consistência | Guardar também uma cópia do estado com falha. Restaurar banco e objetos em destinos novos, conferir hashes/referências e reconciliar operações posteriores ao backup. | Aceite humano e dados conferidos; não sobrescrever o online com dump antigo nem tornar o bucket público. |

Depois de confirmar que a tabela existe, esta consulta **de leitura** auxilia a decisão:

```sql
SELECT count(*) AS total,
       count(*) FILTER (WHERE ativo) AS regras_ativas
FROM public.bloqueios_semanais_quadra;
```

`regras_ativas > 0` bloqueia o retorno ao #94 neste procedimento, mesmo que alguma regra
esteja vencida; é uma condição conservadora. Zero não é aprovação automática de rollback:
schema, dados, PDFs, artefato e autorização também precisam estar conferidos.
Não existe script de DOWN destrutivo. A recuperação preserva tabela, regras e histórico;
não converte recorrências em massa nem falsifica datas para demonstrar funcionamento.

## Impacto acadêmico identificado

O sistema publicado e a monografia atual documentam **12 entidades**. Esta candidata
acrescenta a 13ª, `BloqueioSemanalQuadra`, com relação muitos-para-um para `Quadra`.
Os outros 12 modelos/relacionamentos persistentes permanecem iguais.

Se o grupo/orientador decidir incorporar esta funcionalidade à versão acadêmica, revisar
o Capítulo IV (requisito/caso de uso administrativo e disponibilidade), Capítulo V
(classe/dicionário de atributos), Apêndice A/DER (nova tabela/FK) e Capítulo VII
(programação, testes e publicação). O [mapa técnico](mapa-do-codigo.md) já distingue a candidata.
Não editar os DOCX/PDF de 98 páginas, diagramas congelados ou slides como se a funcionalidade
já estivesse publicada. O Apêndice B e os resultados da pesquisa não mudam por essa adição.
