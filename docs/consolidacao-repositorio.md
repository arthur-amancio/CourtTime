# Consolidação do repositório — 20/10/2026

Registro da limpeza feita na missão de finalização. A `main` de referência é
`f73dd4e` (antes dos PRs #60–#62).

## PRs fechados sem merge

Nenhum deles tinha conteúdo que faltasse à `main`. O critério foi comparar,
arquivo a arquivo, as linhas que existem no PR e não na `main`; as que sobraram
eram versões anteriores de código depois substituído.

| PRs | Motivo |
| --- | --- |
| #13, #17, #19, #20, #21, #22, #23, #24, #25 | Cadeia funcional original. Integrada pelos PRs #36 e #37 (`83ef3e9`), que é ancestral da `main` |
| #27, #28, #29, #30, #31 | Integrados pelos PRs #36 e #37 |
| #32, #33, #35 | Idênticos à `main` nos arquivos que alteram |
| #34 | Branch de auditoria marcada "não mesclar"; superada pela `main` |
| #42 | Mesma correção do #40, já integrado |
| #43 | Roteiro da banca duplicado; o #39 foi integrado |
| #45 | Branch de validação "não mesclar"; os cinco PRs que ela testava (#38–#41, #44) foram integrados |

Um PR fechado pode ser reaberto, e sua branch restaurada pelo botão
"Restore branch" do próprio PR.

## Preservados

| Item | Por quê |
| --- | --- |
| PR #49 e `design/prototipo-visual-courttime` | Protótipo "NÃO MESCLAR", referência visual da V2 |
| `evidencias/ui-v2` | Capturas citadas no relatório da V2; o workflow `ui-navegador.yml` publica nela |

## Branches remotas a apagar

Não foram apagadas: o ambiente usado na consolidação não tem permissão para
excluir branches. A lista foi conferida antes; nenhuma tem trabalho exclusivo.

**42 branches ancestrais da `main`:** todos os commits delas já estão na
`main`, então apagar não perde nada.

| Branch | Último commit | PR |
| --- | --- | --- |
| `chore/github-actions-ci` | `4cf0754` | #4 (mesclado) |
| `chore/ui-v2-verificacao-navegador` | `8f777bb` | sem PR |
| `docs/inventario-tecnico` | `d2026e8` | #41 (mesclado) |
| `docs/roteiro-apresentacao` | `897b88d` | #39 (mesclado) |
| `feat/admin-real-management` | `f6f81d4` | #12 (mesclado) |
| `feat/admin-reservation-review` | `46e3fbc` | #23 (fechado) |
| `feat/admin-user-management` | `a004ed3` | #13 (fechado) |
| `feat/antecedencia-minima-solicitacao` | `549ed39` | #26 (mesclado) |
| `feat/demo-local-setup-20261020` | `4a931d2` | #46 (mesclado) |
| `feat/home-professional` | `1c6edc9` | #7 (mesclado) |
| `feat/institutional-cancellation` | `3745ff9` | #25 (fechado) |
| `feat/participants-request` | `7bb6340` | #19 (fechado) |
| `feat/secretary-contributions` | `633a715` | #22 (fechado) |
| `feat/secretary-document-review` | `95bf76b` | #21 (fechado) |
| `feat/security-authorized-reservations` | `eea7ca0` | #24 (fechado) |
| `feat/signed-document-upload` | `8882f46` | #20 (fechado) |
| `feat/ui-product-identity` | `133cac7` | #8 (mesclado) |
| `feat/ui-redesign-fase-a-design-system` | `de29905` | #50 (mesclado) |
| `feat/ui-refinamento-institucional` | `440b3b0` | #28 (fechado) |
| `feat/ui-v2-administracao` | `216b67f` | #55 (mesclado) |
| `feat/ui-v2-area-aluno` | `3618e16` | #54 (mesclado) |
| `feat/ui-v2-ensaio-escola` | `0965ef3` | #58 (mesclado) |
| `feat/ui-v2-layout-navegacao` | `8b11183` | #52 (mesclado) |
| `feat/ui-v2-publico-login` | `394e917` | #53 (mesclado) |
| `feat/ui-v2-secretaria-seguranca` | `2d284e6` | #56 (mesclado) |
| `feat/w1-interface-base` | `25541d8` | #2 (mesclado),#1 (mesclado) |
| `feat/w2-authentication` | `671af9f` | #3 (mesclado) |
| `feat/w3-2-calendar-availability` | `444ec33` | #9 (mesclado) |
| `feat/w3-quadra-disponibilidade-base` | `ad070ec` | #6 (mesclado) |
| `feat/w4-1-student-requests` | `6b5721c` | #11 (mesclado) |
| `feat/w4-reservation-foundation` | `f6c89d6` | #10 (mesclado) |
| `fix/aprovacao-respeita-antecedencia` | `be4bc4e` | #27 (fechado) |
| `fix/feedback-devolucao-secretaria` | `2c68372` | #40 (mesclado) |
| `fix/paginas-de-erro` | `26ee3bd` | #44 (mesclado) |
| `fix/ui-v2-css-skiplink` | `a9d751a` | #57 (mesclado) |
| `fix/windows-environment-verifier` | `7f98dee` | #47 (mesclado) |
| `integracao/courttime-core` | `7c86979` | #36 (mesclado) |
| `integracao/courttime-final` | `f9a5e8e` | #37 (mesclado) |
| `refactor/simplify-database-schema` | `26b2707` | #5 (mesclado) |
| `test/jornada-e2e` | `d52d615` | #38 (mesclado) |
| `test/qa-redesign-v2` | `5521a21` | #51 (mesclado) |
| `test/visual-baseline-redesign` | `2d161d7` | #48 (mesclado) |

**15 branches superadas:** o conteúdo foi integrado de outra forma
(squash, outra branch ou correção equivalente). As que têm PR podem ser
restauradas pelo PR. A única sem PR, `feat/ui-v2-release-smoke-20261020`, não
tem nenhuma linha que falte à `main`.

| Branch | Último commit | PR |
| --- | --- | --- |
| `audit/consolidacao-main` | `00051b1` | #34 (fechado) |
| `audit/final-quality-20261019` | `84a4069` | #35 (fechado) |
| `docs/agent-team-workflow-20261016` | `c4bb115` | #14 (mesclado) |
| `docs/guia-e-integracao-pdf` | `3ac412e` | #33 (fechado) |
| `docs/roteiro-demonstracao-banca` | `d700adb` | #43 (fechado) |
| `feat/acabamento-visual-acessibilidade` | `11c264e` | #32 (fechado) |
| `feat/aluno-painel-inicial` | `691d821` | #31 (fechado) |
| `feat/ui-v2-release-smoke-20261020` | `9bc35f2` | sem PR |
| `feat/ux-mobile-courttime-completo` | `2d8ecd9` | #30 (fechado) |
| `fix/calendar-slot-alignment-issue-15` | `5e69bc9` | #16 (mesclado) |
| `fix/feedback-devolucao-documental` | `f9aefb0` | #42 (fechado) |
| `fix/horarios-ordem-semanal` | `b3d7f59` | #59 (fechado) |
| `fix/pr12-stability-night-audit` | `b95ab46` | #17 (fechado) |
| `test/courttime-integracao-completa` | `bbb78ad` | #29 (fechado) |
| `validacao/rc-congelamento` | `4084072` | #45 (fechado) |

### Como apagar

Na sua máquina, na pasta do projeto, com o Git autenticado no GitHub:

```powershell
git fetch origin --prune
git push origin --delete audit/consolidacao-main audit/final-quality-20261019 chore/github-actions-ci chore/ui-v2-verificacao-navegador docs/agent-team-workflow-20261016 docs/guia-e-integracao-pdf docs/inventario-tecnico docs/roteiro-apresentacao docs/roteiro-demonstracao-banca feat/acabamento-visual-acessibilidade feat/admin-real-management feat/admin-reservation-review feat/admin-user-management feat/aluno-painel-inicial feat/antecedencia-minima-solicitacao feat/demo-local-setup-20261020 feat/home-professional feat/institutional-cancellation feat/participants-request feat/secretary-contributions feat/secretary-document-review feat/security-authorized-reservations feat/signed-document-upload feat/ui-product-identity feat/ui-redesign-fase-a-design-system feat/ui-refinamento-institucional feat/ui-v2-administracao feat/ui-v2-area-aluno feat/ui-v2-ensaio-escola feat/ui-v2-layout-navegacao feat/ui-v2-publico-login feat/ui-v2-release-smoke-20261020 feat/ui-v2-secretaria-seguranca feat/ux-mobile-courttime-completo feat/w1-interface-base feat/w2-authentication feat/w3-2-calendar-availability feat/w3-quadra-disponibilidade-base feat/w4-1-student-requests feat/w4-reservation-foundation fix/aprovacao-respeita-antecedencia fix/calendar-slot-alignment-issue-15 fix/feedback-devolucao-documental fix/feedback-devolucao-secretaria fix/horarios-ordem-semanal fix/paginas-de-erro fix/pr12-stability-night-audit fix/ui-v2-css-skiplink fix/windows-environment-verifier integracao/courttime-core integracao/courttime-final refactor/simplify-database-schema test/courttime-integracao-completa test/jornada-e2e test/qa-redesign-v2 test/visual-baseline-redesign validacao/rc-congelamento
```

Depois de integrar os PRs desta missão, apague também as branches deles:
`feat/remarcacao-apos-cancelamento`, `feat/formulario-oficial-pdf` e
`feat/guia-local-windows-pg18`.
