# Matriz de validação do redesign V2

Base funcional inicial: `8c00b358de36fb03d113e8e2a8de6497c4cc1dfb`.
Referência visual: PR #49, commit
`766c430fde14ece94c3f524e2a8b31b768ae5581` — **não mesclar**.

Revisões reais registradas nesta matriz:

- Fase B, PR #52: `8b11183c24903598b809c31c2c545607e70232fb`;
- Fase C, PR #53 (baseada na Fase B):
  `394e9177fc9f5935d96b16ede55c8eba13b31cf4`;
- Fase D, PR #54 (baseada na Fase C):
  `3618e16bd2a7a5a66a6325b4bfb8949cef14e2aa`;
- Fase E, PR #55 (baseada na Fase D):
  `216b67fcc67e052e5759296a4d6adc8df78d45f9`;
- Fase F, PR #56 (baseada na Fase E):
  `2d284e6fd823c878752cc3a6fc5e9576afcaac54`.

As capturas em `design/prototipo-visual/docs/capturas/v2/` pertencem ao
protótipo estático. Elas orientam a comparação, mas não são evidência da
aplicação Spring Boot. Na base inicial, somente a Fase A (tokens e fundação do
design system) está integrada; diferenças visuais esperadas não são defeitos.

| Tela | Referência V2 no PR #49 | Implementação revisada | Divergência encontrada | Prioridade | Critério de aceite |
| --- | --- | --- | --- | --- | --- |
| Login | `login--390/430/1440.png` | Fase C no PR #53; formulário real preservado | Nenhuma divergência funcional ou de reflow nas larguras auditadas | P2 | Formulário mantém `email`, `password`, CSRF, erros reais, foco visível e envio sem JavaScript. |
| Página pública | `index--390/1440.png` | Fase C no PR #53 | Nenhuma divergência bloqueadora; texto informa que o valor da APM ainda depende de confirmação | P2 | Um H1, conteúdo institucional verdadeiro, sem ação fictícia, sem overflow e navegação por teclado. |
| Página inicial autenticada | `inicio-administrador--1440.png` | Conteúdo migrado na Fase D, PR #54 | Nenhuma divergência funcional; o defeito do link de salto vem da Fase B | P1 | Mostra somente ações autorizadas, situação real do aluno e um H1; nenhuma ação fictícia. |
| Navegação compartilhada | `menu-movel-aberto-390.png` | Fase B no PR #52 | O link de salto focado fica atrás de `header.topo-app` em 390 px (`z-index: 10` contra `30`) | P1 | Menu abre pelo teclado, usa `aria-expanded`, torna fundo `inert`, contém foco, fecha por Escape e restaura foco; link de salto permanece visível; links e logout POST funcionam sem JS. |
| Calendário | `quadra--1440.png` e `calendario-opcao-a-390.png` | Opção A implementada na Fase D, PR #54 | Nenhuma divergência funcional na grade controlada; sem contadores por dia | P1 | Usa grade/bloqueios reais, recusa reserva no passado e antecedência inválida, mantém `horarioInicio=HH:mm`, teclado e alternativa sem JS. |
| Nova solicitação | `nova-solicitacao--390/430/768/1440.png` | Migrada na Fase D, PR #54 | Nenhuma divergência funcional; radios preservam `horarioInicio=HH:mm` | P1 | Preserva nomes, rota, CSRF e duração calculada no backend; validação legível e nenhum envio automático. |
| Participantes e documentos | capturas de detalhe do aluno no PR #49 | Migrados na Fase D, PR #54 | Nenhuma divergência bloqueadora nas jornadas de dez alunos, upload, substituição e download | P1 | Mantém ownership, campos confirmados, original privado, versões, limite e validação no servidor; funciona sem JavaScript. |
| Painel administrativo | `admin-painel--390/1440.png` | Migrado na Fase E, PR #55 | `style.css` contém um marcador `=======`; deve ser removido antes da integração | P1 | Solicitações, usuários, horários e bloqueios permanecem alcançáveis; nenhuma permissão da secretaria é ampliada e não há artefatos de conflito. |
| Secretaria | `secretaria-conferencia--1440.png` | Migrada na Fase F, PR #56 | O topo da pilha contém dois marcadores `=======` no CSS herdado/mesclado | P1 | Devolução exige justificativa com mensagem visível; download, validação e contribuição preservam CSRF, integridade e duplicidade. |
| Segurança | `seguranca-conferencia--390.png` e `seguranca-reservas--1440.png` | Migrada na Fase F, PR #56 | Nenhuma divergência própria nas telas; integração depende da limpeza do CSS | P1 | Exibe apenas reservas autorizadas e identificação necessária; omite documentos, finanças e edição; canceladas retornam 404. |

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
