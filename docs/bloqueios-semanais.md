# Bloqueios semanais — manutenção e publicação futura

Implementação candidata baseada no PR #94. **Não publicada no Render e não aplicada ao Neon.**
As revisões acadêmicas #93/#95 e a monografia principal de 98 páginas permanecem preservadas.

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

1. Após revisão/autorização, atualizar refs e conferir os HEADs de #93/#94/#95 e deste PR.
   Integrar preservando commits na missão autorizada, interrompendo em conflito/check falho.
   Não ligar deploy automático ou trocar plano do Render/Neon.
2. Produzir backup privado do banco e dos PDFs referenciados, com hashes, e testar recuperação
   em destinos novos. Não publicar dump, chaves S3 ou credenciais na apresentação/repositório.
3. Restaurar a versão anterior em banco **isolado**, conferir o nome conectado e aplicar o SQL
   abaixo primeiro nesse banco. Conferir contagens/hashes das 12 tabelas e subir o novo JAR
   com `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`. Testar cadastro, conflito, desativação e PDF.
4. Somente após a autorização para o Neon, conferir explicitamente `current_database()`,
   endpoint/conta e backup. Confirmar o destino na sessão privada e executar uma vez:

   ```bash
   # NOME_CONFERIDO é o banco realmente verificado; conexão/credenciais ficam privadas.
   PGOPTIONS='-c courttime.migration_database=NOME_CONFERIDO' \
     psql -v ON_ERROR_STOP=1 -f scripts/deploy/20261105-bloqueios-semanais.sql
   ```

   Sem confirmação ou com banco diferente, o script falha antes de criar a tabela.
   `lock_timeout=5s` e transação limitam a aplicação; em falha, diagnosticar antes de repetir.
5. A tabela nova começa vazia. Não cadastrar regras antes de o runtime novo estar ativo:
   o código antigo desconhece a recorrência. Fazer deploy **manual** do SHA aprovado,
   preservando variáveis, bucket privado, Free e `/healthz`.
6. Aguardar o despertar, conferir saúde/login, quatro perfis, dados/PDFs existentes e criar
   uma regra sintética pela interface para uma data sem aprovada; conferir bloqueio e
   desativação. Só depois registrar a funcionalidade como publicada e validada no Render.

**Retorno ao runtime #94:** a tabela aditiva pode permanecer, sem DROP. Entretanto, o #94
ignora regras semanais: com regras ativas, voltar ao código antigo reabriria períodos
indevidamente. Suspender novas operações e combinar tratamento manual autorizado dos
bloqueios antes de qualquer retorno. Não apagar histórico nem converter regras em massa.

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
