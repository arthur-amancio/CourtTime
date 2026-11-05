# Atualização vigente — 05/11/2026

**PRONTO COM RESSALVAS. INTEGRAÇÃO TECNICAMENTE PREPARADA — AGUARDANDO AUTORIZAÇÃO.**
Deploy #94 aceito como funcionalmente validado com as ressalvas documentadas. Nenhuma nova
auditoria genérica, suíte Java, implantação ou merge foi realizada nesta revisão acadêmica.

| PR | Base | HEAD remoto reconfirmado | Estado/checks |
| --- | --- | --- | --- |
| #92 | #91 | 07036b2244b448be89953d528e6dcdbe7a2fd4e6 | Aberto/draft/MERGEABLE; Java PG16/18 e guard aprovados nesse HEAD. |
| #93 | #92 | 273e83c5172921ee36292cfc9c94e48e151cb90b | Aberto/draft/MERGEABLE; HEAD documental, sem checks próprios. |
| #94 | #91 | fd5385687fdc49cc815725bc039656900b697f71 | Aberto/draft/MERGEABLE; HEAD documental, sem checks próprios. Runtime 3cdfccc: CI PG16/18 e Docker aprovada. |

Main: `4df490729734d9f45830e15be241cdb66c3f112f`. #91 é ancestral de #92 e #94;
#92 é ancestral de #93; #93 e #94 são ramificações, não uma cadeia linear.
#91→#93 altera apenas cinco arquivos documentais; nenhum `src/`, `qa/`, `pom.xml` ou workflow.
`git merge-tree --write-tree origin/docs/pesquisa-original-integrada-20261103 origin/chore/render-neon-demo-gratuita`
retornou `151e5968dab39aff7a34475a9c0b7974d2b13110`, sem conflito; árvore idêntica à conferência
anterior. Esse objeto é árvore local, **não commit de merge nem SHA de futura main**.
O diff da árvore resultante contra #94 contém somente a revisão acadêmica esperada.

**Integração futura:** após autorização específica, atualizar refs e conferir SHAs/proteções;
para cada PR #85→#86→#87→#88→#89→#90→#91→#92→#93→#94 e revisão documental final:
confirmar que a base anterior já está na main, ajustar a base do próximo PR à main somente
na missão autorizada e integrar por merge commit (sem squash/rebase/force push).
Após cada etapa, conferir ancestralidade, diff esperado e checks exigidos; após a última,
conferir código/árvore contra o candidato aprovado e realizar validação integrada proporcional
ao resultado. Parar antes de qualquer merge se houver conflito, checks obrigatórios falhos,
SHA novo sem revisão, arquivo ausente, regressão crítica ou proteção não atendida.
Não reaplicar patches acadêmicos por cherry-pick sobre o #94 nem duplicar funcionalidades.
Nenhuma base remota foi alterada nesta missão.

**Acadêmico:** monografia principal de 98 páginas preservada por hash; B completo;
slides/kit atualizados. Professor, anexo/C/D, regras institucionais e ensaio presencial
continuam humanos. Não confundir implementação validada com homologação, integração executada,
ensaio realizado, TCC aprovado ou submetido.

---

## Histórico de 01/11 — preservado

# CourtTime — decisão técnica de encerramento

01/11/2026. **PRONTO COM RESSALVAS. INTEGRAÇÃO TECNICAMENTE PREPARADA — AGUARDANDO AUTORIZAÇÃO.**
Versão funcional: PR #90, `b842f35593c8624793003349aaffbb69c21926b4`.
Main real: `4df490729734d9f45830e15be241cdb66c3f112f`. Nenhum merge executado.

## Diagnóstico dirigido

| Condição | Classificação | Evidência e alcance |
| --- | --- | --- |
| 1. Seis PRs e ancestralidade | APROVADO | Seis ligações conferidas por `git merge-base --is-ancestor`; HEADs e bases abaixo. |
| 2. Conflitos/arquivos inesperados | APROVADO | Seis PRs OPEN/draft e MERGEABLE; mesmas referências da missão cloud; sem diferença nova a simular. |
| 3. CI do último HEAD | APROVADO | CI Java e navegador identificam `b842f35`; resultados abaixo. Checks antigos falhos não foram convertidos em sucesso. |
| 4. Entidades/relacionamentos | APROVADO | 12 entidades; `entity/` idêntico a `4fb7a06`. Sem mudança JPA/schema desde a referência do DER. |
| 5. Disponibilidade/concorrência | APROVADO | Testes PostgreSQL existentes de aprovação incompatível, bloqueio e grade; lock da quadra/reserva mantido. |
| 6. Dez participantes/rascunho | APROVADO | Testes de nove/dez e documentos bloqueados; mínimo soma ALUNO/OUTRO. Homologação desse mínimo é separada. |
| 7. Documento/contribuição/aprovação | APROVADO | Revisão da lista, conferência da versão exata e contribuição revalidadas; aprovação não decorre de upload ou pagamento isolado. |
| 8. Vaga paga/adicionais | APROVADO | Jornada e integrações comprovam substituição sem nova cobrança e R$ 2,00 por excedente; histórico e repasse preservados. |
| 9. Liberação da portaria | APROVADO | Reserva aprovada no dia, dez presentes e saldo zero; bloqueio de alterações posteriores exercitado. |
| 10. Permissões/documentos | APROVADO | Autenticação por sessão, CSRF, titularidade, versão de acesso e downloads privados; sondas dirigidas, não perícia exaustiva. |
| 11. Recuperação | APROVADO | Restore em banco novo, sete PDFs por tamanho/SHA-256 e quatro perfis HTTP 200 na missão cloud. |
| 12. Compatibilidade documental | APROVADO | DER/entidades e fluxos preservados. Contagens antigas são históricas; números atuais estão neste suplemento. Regras institucionais divergentes exigem decisão humana. |
| Autorização de merge/homologação | PENDENTE DE DECISÃO HUMANA | Proprietário autoriza integração; ETEC decide uso institucional; orientador emite parecer acadêmico. |
| Submissão acadêmica integral | BLOQUEADO | Até resolver/dispensar formalmente C/D e demais itens da tabela de decisões. Não é defeito do software. |

Evidências já executadas no código inalterado: [CI Java 16/18](https://github.com/arthur-amancio/CourtTime/actions/runs/41275138403), **479 por versão**, zero falhas/erros/skips;
[navegador](https://github.com/arthur-amancio/CourtTime/actions/runs/41275142391), **1.128 verificações**, **44 varreduras axe/zero violações**;
[guarda](https://github.com/arthur-amancio/CourtTime/actions/runs/41275138366), aprovada.
[JSON cloud](https://github.com/arthur-amancio/CourtTime/blob/b842f35593c8624793003349aaffbb69c21926b4/docs/tcc/evidencias/cloud-20261101/resultados.json) identifica árvores, casos concorrentes, isolamento e recuperação.
Não repetir essas suítes nesta missão documental. Testes automatizados não equivalem a homologação, auditoria completa de segurança ou ensaio humano.

## Bases e plano autorizado somente em missão futura

| PR | HEAD completo esperado | Base atual | Conferência após integração autorizada |
| --- | --- | --- | --- |
| #85 | `fe21d898f9082acb30c9f71700fc7d7872a2ef5a` | main | Permissões, CI e ancestralidade na main; revisar base/diff de #86. |
| #86 | `4fb7a06c8343234487bd15260c67e31a3b765aaa` | feat/refatoracao-permissoes | Rascunho, portaria, financeiro; verificar obrigatoriedade do check antigo falho; revisar #87. |
| #87 | `24d99feda3cd9b070af9f4ddb5e327a81d1aed27` | feat/portaria-liberacao-acrescimos | Mapa documental e árvore funcional preservados; revisar #88. |
| #88 | `20e376ab616f6f2f913a20ae49e645f6a8938691` | chore/ui-v2-verificacao-portaria | Documentos e hashes preservados; revisar #89. |
| #89 | `abb240601d0e26616942f050e6aa72e6964e296f` | docs/modelagem-uml-profissional | Correção PostgreSQL do navegador e consultas/permissões; revisar #90. |
| #90 | `b842f35593c8624793003349aaffbb69c21926b4` | refactor/simplificacao-final-courttime | Confirmar escopo QA/docs, CI do resultado e smoke dos quatro perfis. |

Todos permanecem abertos/draft. #85/#88/#89/#90 têm Java/guarda aprovados;
#86/#87 mantêm falhas históricas de navegador. #89 e #90 incluem a correção e
possuem execução de navegador aprovada. Um check obrigatório antigo falho **não pode ser ignorado** por isso.

Simulação anterior preservada: `9261bd8` contém #85–#89 e árvore integral idêntica a #89;
`794a299` inclui QA Cloud e árvore idêntica a `ff094c8`. Comparação de `src`, `qa`, `pom.xml` e `.github`
com #90 não apresentou diferenças. A diferença posterior é documental; não houve nova simulação ou SHA fictício de main.

Na missão autorizada: buscar referências, conferir HEAD/revisões/proteções e usar merge preservando commits.
Após cada merge, conferir a main real, ajustar somente a base do próximo PR se necessário e revisar seu diff/CI.
Não antecipar retargets, usar squash/rebase/force push, apagar branches ou contornar proteções.
Interromper diante de HEAD divergente, conflito, exclusão inesperada, check obrigatório falho ou regressão crítica.
Se a combinação funcional mudar, validar a nova árvore em banco descartável antes de prosseguir.
Após o último merge, registrar o SHA real e executar smoke dos quatro perfis; a suíte completa só precisa ser repetida se a combinação mudou ou for check exigido.

**Ação exata que falta ao proprietário:** autorizar separadamente os merges sequenciais #85→#86→#87→#88→#89→#90,
com as revisões exigidas. Este documento não concede essa autorização. O PR documental deste pacote, se publicado,
é posterior a #90 e deve ser revisado separadamente. Nenhuma nova funcionalidade é necessária para a Sprint 4.
