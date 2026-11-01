# CourtTime — kit de ensaio e defesa

Proposta de 01/11/2026 para aprovação do grupo. Complementa o guia de 48 perguntas e os três simulados existentes;
não os substitui nem atribui autoria individual de módulos. Nenhum ensaio humano foi executado nesta missão.

## Preparar os três cenários

Usar o [guia cloud](https://github.com/arthur-amancio/CourtTime/blob/b842f35593c8624793003349aaffbb69c21926b4/docs/desenvolvimento-cloud.md) e o [roteiro ampliado](https://github.com/arthur-amancio/CourtTime/blob/b842f35593c8624793003349aaffbb69c21926b4/docs/tcc/preparacao-banca/DEMONSTRACAO.md).
Banco/diretório de PDFs exclusivos, nome real conferido, contas fictícias, senhas temporárias privadas e quatro contextos independentes.
Registrar SHA e IDs na ficha privada. Não usar a base E2E já consumida como se estivesse pronta para apresentação.

| Cenário | Preparação antes do ensaio | Demonstração e resultado |
| --- | --- | --- |
| A — aluno | Horário futuro disponível e rascunho com nove participantes. PDF sintético identificado, sem assinatura real. | Login/calendário; incluir décimo, observar formalização, gerar formulário e enviar PDF. Rascunho não oferece atalho documental. |
| B — portaria | **Até o dia anterior**, criar para a data real da apresentação, concluir participantes/PDF/conferência/contribuição e aprovação. Não liberar antes do ensaio. | Conferir histórico da Secretaria/Administração; Segurança marca presença. Com nove, liberação recusada; com dez e saldo zero, permitida. Na versão ampliada, A paga/sai, B substitui sem cobrar, C excedente paga R$ 2,00; repasse separado. |
| C — remarcação | Outra reserva aprovada e paga, elegível a cancelamento institucional. Preparar datas/horários alternativos futuros. | Cancelar com motivo; nova solicitação vinculada. Exemplo: crédito R$ 20,00, 12 participantes/R$ 24,00, cobrar R$ 4,00; novo PDF, conferência e aprovação. Sem regra inventada para excedente. |

A missão cloud já comprovou A/B/C automaticamente, incluindo crédito/diferença e recuperação. A data de B foi ajustada por fixture **só no banco E2E**,
explicitamente registrada; isso não serve como procedimento normal de banca. Sem B preparada corretamente, usar captura identificada como evidência anterior.
Não modificar a data no banco durante a apresentação nem prometer operação de portaria sobre uma solicitação futura recém-criada.

## Roteiro conferido de quatro minutos

Soma: **240 segundos**. A sequência respeita pré-requisitos; tempo é proposta para ensaio, não duração humana comprovada.
Sessões e nove participantes preparados. A geração/assinatura externa não pode ser concluída legitimamente em segundos: o upload é de PDF sintético de demonstração.

| Tempo | Tela/perfil e ação | Resultado e fala resumida | Alternativa se falhar |
| --- | --- | --- | --- |
| 0:00–0:35 | Aluno: login, calendário e A. | “O aluno consulta horários reais da grade e inicia uma solicitação futura.” Mostrar nove participantes. | Sessão do aluno já aberta; declarar login previamente realizado. |
| 0:35–1:10 | Aluno: incluir décimo e abrir documentos. | “O backend formaliza somente com dez pessoas. O formulário é gerado; assinatura é externa.” | Abrir A alternativa preparada com dez; não afirmar que o clique falho funcionou. |
| 1:10–2:00 | Aluno/Secretaria: upload sintético, conferência e recebimento. | “A assinatura é conferida por uma pessoa; R$ 20,00 são registrados como contribuição presencial.” | Usar solicitação alternativa já conferida e explicar preparação; não simular assinatura autêntica ou pagamento bancário. |
| 2:00–2:35 | Administrador: conferir e aprovar A. | “A aprovação revalida documentação, pagamento e conflitos sob transação.” | Abrir aprovação anterior identificada, sem editar banco ou forçar disponibilidade. |
| 2:35–3:40 | Segurança: anunciar troca para B de hoje; marcar dez e liberar. | “Esta é outra reserva, preparada no dia anterior. A liberação exige dez presentes e nenhum saldo.” | Captura real de portaria, explicitando que não é execução ao vivo. |
| 3:40–4:00 | Segurança: mostrar conferência encerrada. | “Depois da liberação, alterações ficam bloqueadas. Substituição, complemento e remarcação integram o roteiro ampliado.” | Mostrar estado encerrado de uma reserva alternativa e concluir dentro do tempo. |

Não tentar demonstrar assinatura externa, todos os pagamentos, substituições e remarcação inteira em quatro minutos.
Se o professor definir outro tempo, ajustar roteiro e falas antes de alterar slides.

## Contingência executável

| Problema | Ação do estudante | Limite para continuar |
| --- | --- | --- |
| Aplicação não inicia | Abrir terminal reservado, conferir Java 17 e último erro em `app.log`; executar o JAR conforme guia. Esperar `/login` responder. | Sem página de login após tentativa curta, usar PDF/capturas. Não projetar variáveis/senhas. |
| PostgreSQL indisponível | Conferir `pg_isready -h 127.0.0.1 -p <porta>`; iniciar somente o cluster privado preparado com `pg_ctl -D <diretório> start`. | Se não reconhecer diretório/banco, parar e usar evidências; não apontar para servidor real. |
| Login sem acesso | Conferir perfil/email fictícios e senha no registro privado; reautenticar no contexto correto. | Conta desativada/senha desconhecida: usar conta alternativa preparada; não editar hashes no banco. |
| Dados consumidos/liberados | Usar reserva alternativa preparada. Para recomeçar, restaurar backup sintético e PDFs em **novos** destinos conforme guia. | Não reabrir reserva liberada nem apagar histórico. |
| B incompatível com hoje | Conferir data, aprovação e sessão da Segurança. | Sem reserva válida de hoje, mostrar captura anterior identificada. Não mudar data artificialmente como ação do sistema. |
| PDF não encontrado | Conferir diretório privado correspondente ao backup; usar cópia sintética com hash conhecido ou reserva alternativa. | Não desabilitar autorização nem substituir bytes de documento já registrado. |
| Projeção falha | Testar cabo/entrada e resolução; usar PDF local dos slides ou outro equipamento autorizado. | Explicar a limitação e seguir a defesa oral; não depender de links públicos. |

Ficha de ensaio a preencher: data/fuso ____; SHA ____; banco confirmado ____; IDs A/B/C ____;
operadores ____; início/fim ____; tempo real ____; passos falhos ____; correções logísticas ____; alternativa testada ____.
Encerrar aplicação/cluster e fechar a sessão com credenciais após o uso. Não fazer deploy público.

## Divisão equilibrada proposta

Usar os 15 slides existentes. Versão de 12 minutos, **oito de fala e quatro de demonstração**, sujeita ao tempo oficial:

| Integrante | Dois minutos de fala | Um minuto na demonstração/apoio |
| --- | --- | --- |
| Arthur | Problema, público, objetivos, levantamento e limites da pesquisa. | 0:00–1:00: aluno/calendário e encaminhamento ao décimo. |
| Carlos | Requisitos, atores, casos de uso, AOO e DER. | 1:00–2:00: documentos/Secretaria; auxiliar troca de contexto. |
| Gustavo | MVC, Thymeleaf, JPA, transações, autenticação e autorização. | 2:00–3:00: Administração e início da portaria. |
| Heitor | Testes, resultados, limitações, decisões institucionais e conclusão. | 3:00–4:00: portaria/encerramento; controle de tempo. |

As transições de perfil do roteiro prevalecem sobre a troca exata de operador: ensaiar sem interromper a explicação.
Para a versão sem demonstração, manter três minutos por integrante conforme o roteiro anterior. Todos devem conseguir explicar o fluxo completo.
Não afirmar que essa distribuição corresponde a quem implementou cada módulo.

## Treino rigoroso — responder e sustentar a resposta

Complemento oral: cada integrante responde sem ler por até 45 segundos; outro faz a réplica e pede a evidência.
Marcar “explicou / precisou consultar / precisa revisar”, sem notas ou desempenhos fictícios.

1. **Qual é o fluxo e onde a solicitação vira autorização?** Rascunho → dez participantes → documento → conferência → contribuição → análise → aprovação. Só APROVADA autoriza uso; liberação é conferência posterior. Réplica: “Pagar basta?” Não; demonstrar requisito administrativo em `ReservaService.aprovar`.
2. **Dois administradores clicam Aprovar ao mesmo tempo: quem garante exclusão?** A transação bloqueia reserva e quadra e reconsulta conflitos; o segundo observa o estado atualizado. Réplica: “É só o calendário da tela?” Não; citar `pessimisticCourtLockAllowsOnlyOneConcurrentApprovalForSamePeriod`.
3. **Se uma gravação intermediária falhar, o banco fica pela metade?** A transação reverte as alterações de banco. Arquivo físico exige tratamento próprio de armazenamento/rollback; não pertence automaticamente à transação SQL. Réplica: “Tudo no disco é transacional?” Não; consultar `DocumentoReservaService` e `DocumentoStorage`.
4. **Como chaves estrangeiras e locks se complementam?** FKs impedem referências inválidas; unicidades evitam duplicações específicas. Locks serializam decisões que dependem de consulta e escrita. Réplica: “FK impede duas reservas no mesmo horário?” Sozinha, não; é necessária a regra transacional de conflito.
5. **Autenticação e autorização são a mesma coisa?** Login identifica a conta; autorização decide perfil e propriedade do recurso. Réplica: “Aluno autenticado pode trocar o ID do PDF?” Não; consulta autorizada verifica reserva/documento do próprio aluno. Citar testes IDOR.
6. **E uma sessão antiga após desativação ou mudança de senha/perfil?** A conta tem versão de acesso; o filtro revalida conta, perfil, email e versão, invalidando sessão divergente. Réplica: “Foi implementado JWT?” Não; sessão Spring Security, BCrypt e CSRF.
7. **Por que Thymeleaf e não uma SPA?** O escopo usa formulários e páginas por perfil; renderização MVC mantém uma aplicação simples, sem API/frontend separado desnecessários. Réplica: “Thymeleaf faz segurança sozinho?” Não; escape de texto ajuda, mas autorização e validação são do backend.
8. **O CourtTime valida assinatura digital?** Não. PDFBox preenche o formulário; upload preserva bytes/metadados e a Secretaria registra conferência externa/humana. Réplica: “Prefixo PDF comprova assinatura?” Não; é apenas validação técnica do upload, nunca autenticidade.
9. **O sistema recebe dinheiro?** Registra recebimento presencial calculado, ajustes justificados e histórico. Não processa Pix/cartão. Réplica: “Por que um substituto não paga novamente?” A capacidade já paga pode ser reutilizada antes da liberação; excedente gera R$ 2,00 por pessoa.
10. **Crédito pode dispensar documento ou criar saldo livre?** Não: nova solicitação, documento/conferência/aprovação. Diferença pode ser zero; excedente fica sem destino institucional definido. Réplica: “Pode devolver?” Não demonstrar recurso/política inexistente.
11. **Dez pessoas significam dez alunos oficialmente?** O software soma ALUNO/OUTRO por decisão do grupo; termo histórico diz dez alunos. Réplica: “Quem resolve?” ETEC, com regra e termo aprovados; teste verde não resolve divergência institucional.
12. **28 registros comprovam que a comunidade inteira aprova o sistema ou que o tempo caiu?** Não. São dados exploratórios disponíveis, sem prova de representatividade, unicidade ou experimento temporal. Réplica: “E questionário impresso/logo?” Permanecem sem comprovação; não inventar aplicação nem votação.
13. **Essas imagens são protótipos ou telas reais?** A seção exigida se chama protótipos; as figuras selecionadas são capturas da aplicação implementada, identificadas no texto. Réplica: “Provam uma prototipação anterior?” Não; não há essa evidência.
14. **Os testes aprovam a ETEC e autorizam implantação?** Não. 479 casos Java repetidos em dois bancos não viram 958 casos distintos; 1.128 são verificações de navegador; 44 axe não certificam toda acessibilidade. Réplica: “Por que não está implantado?” Faltam autorização, operação institucional e decisões de dados/regras. Ambiente cloud de desenvolvimento não é hospedagem definitiva; integração também ainda não ocorreu.

Objetivo do treino: explicar uma decisão com exemplo e limite, localizar a evidência e admitir o que depende de confirmação.
As 48 perguntas e os três simulados anteriores continuam como base de estudo; este complemento não cria novos resultados de pesquisa.
