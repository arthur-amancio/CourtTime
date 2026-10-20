# Roteiro de demonstração para a banca

Este roteiro apresenta o fluxo integrado do CourtTime com dados integralmente
fictícios. Ele não substitui procedimentos da ETEC e não deve ser executado em
um banco que contenha dados reais.

## Limites que devem ser informados à banca

- O modelo oficial do termo ainda depende do arquivo solicitado ao Leandro.
- O PDF usado na demonstração é sintético, não é o termo oficial e não possui
  assinatura válida.
- O upload preserva o arquivo, mas não comprova nem valida uma assinatura
  digital.
- O valor de R$ 2,00 por participante/hora é apenas uma referência pendente de
  confirmação institucional. O CourtTime não calcula o valor automaticamente.
- Reagendamento, registro de contato, ocorrências da segurança e geração do PDF
  oficial não fazem parte do fluxo demonstrado.

## Preparação isolada

1. Use Java 17 e PostgreSQL 16.
2. Crie um banco exclusivo, por exemplo `courttime_banca_demo`. Não use o banco
   normal de desenvolvimento.
3. Configure `COURTTIME_DB_URL`, `COURTTIME_DB_USERNAME` e
   `COURTTIME_DB_PASSWORD` apenas no ambiente local.
4. Configure `COURTTIME_DOCUMENT_STORAGE_PATH` para uma pasta privada e
   descartável criada para a demonstração.
5. Siga o bootstrap controlado descrito no README para criar somente o primeiro
   administrador. Escolha a senha no momento da preparação e não a registre no
   repositório ou nos slides.
6. Inicie a aplicação com `./mvnw spring-boot:run` e confirme o acesso em
   `http://localhost:8080`.

Pelo menu **Administração → Usuários**, crie as contas fictícias abaixo. A senha
temporária deve ser definida e entregue localmente, sem ser documentada.

| Perfil | Nome fictício | Email fictício |
| --- | --- | --- |
| Aluno | Aluna Demonstração | `aluna.demo@exemplo.local` |
| Secretaria | Secretaria Demonstração | `secretaria.demo@exemplo.local` |
| Segurança | Segurança Demonstração | `seguranca.demo@exemplo.local` |

Em **Administração → Horários**, cadastre um intervalo semanal futuro com pelo
menos duas horas completas. Escolha uma data de uso com mais de um dia de
antecedência e sem bloqueio.

Prepare também um PDF tecnicamente válido de uma página com o texto
“Documento sintético de demonstração — não é o termo oficial e não representa
assinatura”. Salve-o como `documento-sintetico-nao-oficial.pdf`. Não copie
logotipos, cláusulas, autorizações ou assinaturas institucionais.

## Dados da solicitação

- Finalidade: `Atividade esportiva fictícia para demonstração do TCC`.
- Data e período: escolher na grade preparada, sem fixar uma data no roteiro.
- Participantes: dez pessoas fictícias na categoria **Aluno**.
- Nomes: `Participante Demonstração 01` até `Participante Demonstração 10`.
- RMs: `RM-DEMO-001` até `RM-DEMO-010`.
- Contribuição: `R$ 20,00`, informada manualmente apenas como dado sintético.
- Observação: `Recebimento fictício em ambiente isolado de demonstração`.

## Sequência da apresentação

### 1. Aluno

1. Entre com a conta de aluno e abra **Consultar horários**.
2. Mostre a disponibilidade da data preparada.
3. Em **Nova solicitação**, escolha o período e registre a finalidade.
4. Abra **Participantes** e cadastre os dez alunos fictícios.
5. Mostre o resumo separado entre alunos, demais participantes e total.
6. Revise a lista e abra **Documentos**.
7. Reforce o aviso de que a assinatura ocorre fora do sistema e envie o PDF
   sintético.
8. Mostre a situação principal **Documento aguardando conferência** e o
   histórico da versão enviada.

Resultado esperado: a solicitação ainda não autoriza o uso da quadra; a lista e
um novo envio ficam bloqueados enquanto a versão aguarda a secretaria.

### 2. Secretaria

1. Entre com a conta da secretaria e abra **Conferência documental**.
2. Localize a solicitação e abra seus detalhes.
3. Baixe o original e explique que o arquivo não é modificado pelo CourtTime.
4. Clique em **Validar documento** somente para prosseguir com a demonstração.
5. Abra **Contribuições presenciais**, informe o valor e a observação
   sintéticos e registre o recebimento.
6. Mostre o histórico do registro.

Resultado esperado: a solicitação passa a estar pronta para análise. A
secretaria não possui ação nem permissão para aprová-la.

### 3. Administração

1. Entre com o administrador e abra **Solicitações**.
2. Localize a solicitação na fila de análise.
3. Mostre data, período, documento mais recente, contribuição e participantes.
4. Clique em **Aprovar e autorizar reserva**.
5. Consulte novamente o calendário e mostre que o período aprovado não está
   mais disponível.

Resultado esperado: a solicitação aparece como aprovada somente após a
revalidação transacional das condições.

### 4. Segurança

1. Entre com a conta da segurança e abra **Reservas autorizadas**.
2. Abra a reserva demonstrada.
3. Mostre data, período, solicitante e a identificação necessária dos
   participantes.
4. Observe que documentos e dados financeiros não aparecem neste perfil.

Resultado esperado: somente reservas efetivamente aprovadas são exibidas, em
modo de consulta.

### 5. Cancelamento institucional

1. Volte à administração, abra a reserva aprovada e informe um motivo fictício
   claramente identificado como parte da demonstração.
2. Registre o cancelamento institucional.
3. Volte ao perfil da segurança e atualize a lista.

Resultado esperado: a reserva cancelada deixa imediatamente de autorizar a
entrada e não aparece mais para a segurança. O PDF original e o motivo ficam
preservados.

## Alternativa curta: devolução para correção

Se houver tempo, prepare uma segunda solicitação. Na conferência documental,
use **Devolver para correção** com uma justificativa fictícia. Mostre ao aluno a
situação e envie uma segunda versão sintética. O histórico deve manter a versão
anterior e seus bytes originais.

## Verificações antes de apresentar

- Executar `./mvnw clean verify` contra PostgreSQL 16 isolado.
- Confirmar login dos quatro perfis em uma janela anônima.
- Confirmar data futura, grade ativa e ausência de bloqueio.
- Confirmar que o PDF tem no máximo 10 MB e está claramente marcado como
  sintético.
- Ensaiar em uma largura de celular e em desktop.
- Não usar pessoas, documentos, identificadores ou recebimentos reais.
- Não apresentar o valor fictício como orientação oficial da APM.
- Não afirmar que upload equivale a assinatura verificada.

## Riscos de falha e recuperação

| Sintoma | Verificação segura |
| --- | --- |
| Horário não aparece | Conferir dia da semana, antecedência, grade ativa, bloqueio e reserva aprovada. |
| PDF é recusado | Confirmar extensão, conteúdo PDF válido e limite de 10 MB. |
| Documento não pode ser validado | Confirmar que é a versão mais recente e que existem ao menos dez alunos. |
| Contribuição não aparece | Confirmar que o documento mais recente foi validado. |
| Solicitação não está na fila administrativa | Confirmar documento, participantes e contribuição. |
| Reserva não aparece para a segurança | Confirmar que está aprovada, futura e não foi cancelada. |

Não altere diretamente o banco para contornar uma falha durante a apresentação.
Registre o sintoma e retome a partir de um banco limpo.

## Reinicialização do ambiente

1. Pare a aplicação.
2. Exclua somente o banco exclusivo `courttime_banca_demo` e a pasta privada
   criada especificamente para a demonstração.
3. Recrie o banco vazio.
4. Repita o bootstrap do administrador e a preparação das contas e da grade.

Antes de excluir, confirme o nome completo do banco e da pasta. Nunca execute a
reinicialização contra o banco `courttime` habitual, outro ambiente compartilhado
ou uma pasta ampla do sistema.
