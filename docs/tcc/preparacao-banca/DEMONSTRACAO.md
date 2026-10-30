# CourtTime — demonstração funcional para a banca

**Preparado em:** 29/10/2026. **Estado:** roteiro e dados propostos, baseados nos fluxos já implementados e testados. Nesta missão não foram iniciados uma nova instância, um banco ou uma nova execução de navegador. O ensaio deve registrar versão, data, identificadores das reservas e resultado observado antes da apresentação.

## Ambiente e preparação reservada

Usar Java 17, PostgreSQL isolado e a versão do código identificada no [plano de integração](PRONTIDAO-INTEGRACAO.md). Manter uma worktree própria; não alterar `application.properties` do checkout principal. Registrar `git rev-parse HEAD` na ficha privada do operador.

Criar previamente um **banco novo, vazio e exclusivamente sintético**, com nome e usuário de desenvolvimento identificáveis. Nunca usar o banco principal ou restaurar dados reais. Antes de iniciar, conferir `SELECT current_database(), current_user;` na conexão de demonstração. A restauração descrita adiante deve ocorrer em outro banco novo.

Para iniciar a instância pelo Maven Wrapper, definir explicitamente na sessão PowerShell:

```powershell
# Executar na worktree de demonstração. O banco já deve ter sido criado e conferido.
$env:COURTTIME_DB_URL = Read-Host 'URL JDBC do NOVO banco isolado de demonstração'
$env:COURTTIME_DB_USERNAME = Read-Host 'Usuário desse banco isolado'
$senhaBancoBanca = Read-Host 'Senha do banco isolado' -AsSecureString
$env:COURTTIME_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaBancoBanca).Password
$env:COURTTIME_DOCUMENT_STORAGE_PATH = Read-Host 'Diretório privado exclusivo para PDFs sintéticos'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=18081'
```

Não aceitar o banco padrão por omissão. Conferir a URL e a conexão antes de iniciar; o comando acima não cria nem isola um banco por si só. O diretório de PDFs deve ficar fora da publicação web e separado do armazenamento real. Credenciais entram somente na sessão reservada do operador. Não projetar terminal, gerenciador de senhas ou telas de configuração. Encerrar a instância e limpar as variáveis da sessão ao terminar.

O bootstrap administrativo existente está documentado no [README](../../../README.md). Se necessário no banco novo, configurar `COURTTIME_DEV_USER_NAME`, `COURTTIME_DEV_USER_EMAIL`, `COURTTIME_DEV_USER_PASSWORD` e `COURTTIME_BOOTSTRAP_ADMIN_ENABLED=true` apenas na preparação privada; desabilitar o bootstrap após criar o administrador. Não adicionar um perfil `demo` inexistente.

### Contas propostas

São identificadores fictícios a cadastrar no ambiente isolado, não contas cuja existência foi verificada nesta missão. Definir senhas distintas e guardá-las somente no registro privado da equipe.

| Perfil | Nome exibido | Identificador sintético |
|---|---|---|
| ALUNO | Aluno de Demonstração | `aluno.banca@demo.invalid` |
| SECRETARIA | Secretaria de Demonstração | `secretaria.banca@demo.invalid` |
| ADMINISTRADOR | Administrador de Demonstração | `admin.banca@demo.invalid` |
| SEGURANÇA | Segurança de Demonstração | `seguranca.banca@demo.invalid` |

Usar quatro perfis de navegador ou contextos com sessões independentes. Abrir várias janelas anônimas do mesmo perfil não garante essa independência. Para Segurança, preparar uma janela de 390 px e manter uma alternativa desktop.

### Dados e estados de partida

| Cenário | Preparação | Finalidade |
|---|---|---|
| A — solicitação futura | Próxima data disponível **a partir de amanhã**, horário de uma hora, sem conflito. Deixar um rascunho com nove participantes para a versão curta. | Mostrar criação, mínimo de dez, PDF, Secretaria e Administração. |
| B — portaria de hoje | Reserva aprovada para o dia da apresentação, dez participantes documentais, PDF conferido e R$ 20,00 registrados, ainda sem liberação. | Mostrar presenças, substituição, complemento e liberação. |
| C — remarcação | Reserva futura aprovada e paga, em outro horário, disponível para cancelamento institucional com motivo. | Mostrar histórico, crédito e nova documentação/aprovação. |

Usar nomes como “Participante Demonstração 01” a “10”, oito ALUNO e dois OUTRO com vínculo permitido, por exemplo ex-aluno. Documentos identificadores devem ser sintéticos e compatíveis com as validações existentes; reutilizar o padrão das fixtures de QA, sem copiar dados pessoais. Para os adicionais, usar “Adicional A”, “Substituto B” e “Adicional C”. Uma reserva de dez pessoas em um período de uma hora tem contribuição-base de **R$ 20,00**.

**Restrição de data:** criar a reserva B pelo fluxo normal até o dia anterior, concluindo documento, contribuição e aprovação. A solicitação A, criada no dia da apresentação, não poderá ser mostrada honestamente como a mesma reserva atendida pela portaria naquele dia. Explicar a troca de cenário à banca.

Se a preparação antecipada for inviável, a QA já possui uma fixture que ajusta a data somente em banco E2E descartável, com travas explícitas: [jornada-quatro-perfis.cjs](../../../qa/playwright/jornada-quatro-perfis.cjs), função `definirDataDeDemonstracaoHoje`, e [pré-requisitos de QA](../../../qa/playwright/README.md). Isso é preparação sintética de teste, não remarcação pela interface. Não copiar seu SQL para banco real, retirar as travas ou alegar que o ajuste foi uma ação normal do aluno. A suíte não foi executada nesta missão. Sem B preparada, usar a evidência real anterior e declarar a limitação.

Preparar um PDF válido e identificado como **DEMONSTRAÇÃO — SEM VALIDADE INSTITUCIONAL**, sem assinatura real, para mostrar upload e conferência. A validação do formato e a decisão da Secretaria não comprovam assinatura autêntica. Não substituir o formulário oficial original.

## Sequência executável

Proposta de **oito minutos**, com nove participantes de A já cadastrados, sessões abertas e dados preparados. O tempo é estimado e precisa de ensaio. A criação integral e a digitação dos dez participantes podem ser mostradas em sessão ampliada.

| Tempo | Perfil e ação | Resultado a conferir e explicar |
|---|---|---|
| 0:00–0:45 | Aluno: consultar quadra/data/horário; abrir o rascunho A. | Data futura e ausência de conflito; com nove pessoas o registro continua rascunho e não libera PDF. |
| 0:45–1:30 | Aluno: cadastrar a décima pessoa, categoria OUTRO e vínculo permitido. | ALUNO + OUTRO totalizam dez; etapa inicial concluída. A regra é do backend. Se a lista editável cair abaixo de dez, volta a rascunho. |
| 1:30–2:20 | Aluno: gerar formulário e enviar o PDF sintético preparado. | PDF gerado a partir dos dados e envio registrado; explicar assinatura externa e prazo anterior ao uso. |
| 2:20–3:10 | Secretaria: abrir documento, registrar decisão documental e recebimento presencial. | Versão conferida; dez pessoas × R$ 2,00 × uma hora = R$ 20,00. Não há pagamento online. |
| 3:10–3:40 | Administrador: conferir requisitos e aprovar A. | Aprovação registrada. Explicar revalidação de conflitos e requisitos, sem alterar a data para hoje. |
| 3:40–4:15 | Segurança: anunciar troca para a reserva B, preparada para hoje; consultar participantes e marcar dez presentes. | Identificação e presenças disponíveis somente no contexto autorizado; não exibir os dados financeiros restritos da Secretaria. |
| 4:15–5:45 | Segurança: executar a substituição de vaga paga descrita abaixo. | Histórico preservado e cobrança calculada pela capacidade paga, sem estorno ou duplicidade. |
| 5:45–6:20 | Secretaria: confirmar repasse dos recebimentos registrados na portaria. | Receber na portaria e confirmar repasse são operações diferentes. Não representar repetição de clique como teste de concorrência novo. |
| 6:20–7:00 | Segurança: verificar ao menos dez presentes, nenhuma pendência impeditiva e liberar B. | Liberação registrada; presenças, novos participantes e complementos ficam protegidos contra alterações posteriores. Mostrar o bloqueio pela interface. |
| 7:00–8:00 | Administrador/Aluno: no cenário C, mostrar cancelamento institucional e início da remarcação. | Motivo e histórico preservados; crédito reaproveitado, nova reserva vinculada e nova documentação/aprovação necessárias. Na versão curta, explicar as etapas restantes em vez de afirmar conclusão. |

Para expor o bloqueio de presença, antes de completar os dez presentes em B tentar liberar com nove. Conferir a recusa, então marcar o décimo. Essa tentativa deve ocorrer **antes** dos adicionais; dez cadastrados não significam dez presentes.

### Diferencial: A paga, B substitui, C ultrapassa a capacidade

1. Começar com dez participantes originais presentes e contribuição-base regularizada.
2. Cadastrar A na portaria, marcar presença e registrar complemento de R$ 2,00 pelo fluxo permitido. A capacidade paga passa a onze.
3. Antes de liberar a quadra, A sai: marcar AUSENTE, sem excluir o registro ou desfazer o recebimento.
4. Cadastrar B e marcar presença. São onze presentes; B ocupa a capacidade já paga. **Nada adicional é devido por B.**
5. Cadastrar C e marcar presença. Agora são doze presentes; o excedente devido é **R$ 2,00**. Registrar esse complemento antes de liberar.
6. Conferir o total ilustrativo: R$ 20,00 de base + R$ 4,00 de complementos; dez originais + B + C presentes, A ausente, histórico preservado. Não há estorno nem crédito pela saída de A.

Se A sair sem substituto, seu recebimento continua no histórico e não há devolução automática. Não registrar complemento de valor zero para B nem apagar A para acomodá-lo. A eventual confirmação de repasse deve ser feita na Secretaria, pelo registro correspondente.

### Remarcação: exemplo sem saldo excedente ambíguo

No cenário C, cancelar institucionalmente com motivo fictício “Atividade escolar de demonstração”. Abrir a remarcação para outra data futura disponível, com dez participantes e uma hora. Com R$ 20,00 de crédito elegível e R$ 20,00 devidos, a diferença nova é zero. Conferir crédito aplicado e vínculo à origem. Gerar/enviar novo documento, obter conferência e nova aprovação no ensaio ampliado; o crédito não dispensa essas etapas. Não demonstrar um destino para saldo excedente ainda não decidido pela instituição.

## Recuperação e contingências

| Falha observada | Recuperação segura |
|---|---|
| Navegador perdeu sessão | Reautenticar no contexto daquele perfil, fora da projeção da senha. Confirmar o nome fictício antes de continuar. |
| Upload falhou ou arquivo incorreto | Usar a cópia sintética previamente validada; conferir mensagem, versão e prazo. Não desativar validações. |
| Horário já ocupado | Selecionar o horário alternativo previamente preparado. Não alterar conflito no banco durante a apresentação. |
| Reserva B não aparece | Conferir data de hoje e aprovação no ambiente isolado. Se não houver estado válido, mostrar a captura anterior identificada e explicar que não é execução ao vivo. |
| Erro HTTP ou queda da aplicação | Pausar o fluxo, guardar o erro sem dados sensíveis e usar o inventário de telas reais. Uma captura não deve ser narrada como clique bem-sucedido. |
| Estado já consumido/liberado | Trocar para uma segunda reserva preparada. Nunca editar registros liberados para repetir a cena. |
| Necessidade de restaurar | Antes do ensaio, produzir backup privado somente do banco sintético e copiar seu diretório de PDFs. Restaurar ambos em **outro banco/diretório novos**, conferir conexão e integridade e apontar a instância para eles. Não usar limpeza destrutiva do banco atual ou real. |
| Sem internet | Executar aplicação e banco localmente; ter o PDF candidato e as figuras reais em cópia local. Não mudar permissões do Drive para contornar acesso. |

## Ficha privada de ensaio

Preencher sem senhas: SHA; data/fuso; URL local; banco confirmado; diretório privado; IDs A/B/C; quem opera cada perfil; PDF de teste; resultados de cada transição; duração; erros; alternativa preparada. Conferir projeção, tela de 390 px, sessão correta e encerramento dos processos. **Nenhum campo deste ensaio foi preenchido como aprovado nesta missão.**
