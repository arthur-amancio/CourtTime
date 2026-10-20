# Matriz de validação do redesign V2

Base funcional inicial: `8c00b358de36fb03d113e8e2a8de6497c4cc1dfb`.
Referência visual: PR #49, commit
`766c430fde14ece94c3f524e2a8b31b768ae5581` — **não mesclar**.

As capturas em `design/prototipo-visual/docs/capturas/v2/` pertencem ao
protótipo estático. Elas orientam a comparação, mas não são evidência da
aplicação Spring Boot. Na base inicial, somente a Fase A (tokens e fundação do
design system) está integrada; diferenças visuais esperadas não são defeitos.

| Tela | Referência V2 no PR #49 | Implementação em `8c00b35` | Divergência inicial | Prioridade | Critério de aceite |
| --- | --- | --- | --- | --- | --- |
| Login | `login--390/430/1440.png` | Login Thymeleaf funcional, aparência anterior | Composição e espaçamento ainda não migrados | P2 | Formulário mantém `email`, `senha`, CSRF, erros reais, foco visível e envio sem JavaScript. |
| Página inicial | `inicio-administrador--1440.png` e `index--390/1440.png` | Atalhos condicionais por perfil | Cabeçalho e hierarquia ainda anteriores | P1 | Mostra somente ações autorizadas, situação real do aluno e um H1; nenhuma ação fictícia. |
| Navegação compartilhada | `menu-movel-aberto-390.png` | Links por página; menu móvel V2 ausente | Fase B pendente | P1 | Menu abre pelo teclado, usa `aria-expanded`, torna fundo `inert`, contém foco, fecha por Escape e restaura foco; links e logout POST funcionam sem JS. |
| Calendário | `quadra--1440.png` e `calendario-opcao-a-390.png` | Consulta por campo de data e tabela | Componente mensal/semana móvel ainda ausente | P1 | Implementa opção A sem contadores; usa grade/bloqueios reais, recusa passado e antecedência inválida, mantém `horarioInicio=HH:mm` e alternativa sem JS. |
| Nova solicitação | `nova-solicitacao--390/430/768/1440.png` | Data, horário e finalidade funcionais | Organização visual ainda anterior | P1 | Preserva nomes, rota, CSRF e duração calculada no backend; validação legível e nenhum envio automático. |
| Painel administrativo | `admin-painel--390/1440.png` | Métricas e links reais | Densidade e navegação ainda anteriores | P2 | Solicitações, usuários, horários e bloqueios permanecem alcançáveis; nenhuma permissão da secretaria é ampliada. |
| Secretaria | `secretaria-conferencia--1440.png` | Filas, conferência e contribuições reais | Layout V2 ainda não migrado | P1 | Devolução exige justificativa com mensagem visível; download, validação e contribuição preservam CSRF, integridade e duplicidade. |
| Segurança | `seguranca-conferencia--390.png` e `seguranca-reservas--1440.png` | Lista e detalhe somente leitura | Layout V2 ainda não migrado | P1 | Exibe apenas reservas autorizadas e identificação necessária; omite documentos, finanças e edição; canceladas retornam 404. |

## Procedimento por PR visual

1. Registrar SHA, branch-base e arquivos alterados.
2. Confirmar que não houve mudança de controller, entidade, serviço ou
   autorização sem justificativa funcional separada.
3. Executar Maven e a jornada Playwright na aplicação real.
4. Executar `test:navegacao`, `test:redesign` e `test:a11y`.
5. Comparar capturas de 390, 430, 768 e 1440 px com a linha correspondente.
6. Classificar diferenças por impacto no fluxo, não por igualdade de pixels.

Variações legítimas de texto, quebra de linha e quantidade de registros vindos
do servidor não são regressões. São regressões: ação inacessível, contrato de
formulário alterado, dado fictício, overflow que impede uso, estado incorreto,
perda de teclado, CSRF ou autorização.
