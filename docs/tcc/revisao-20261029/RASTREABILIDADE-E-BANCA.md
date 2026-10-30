# Rastreabilidade e preparação da banca

| Fluxo/regra | Implementação | Teste existente | TCC/figura | Situação |
|---|---|---|---|---|
| Login/perfis/CSRF | SecurityConfig, Permissao | Testes de controllers e jornada | IV/VI | Implementado |
| Disponibilidade/conflitos | CalendarioDisponibilidadeService, ReservaService | ReservaPostgreSqlIntegrationTest | IV/VI | Implementado |
| Rascunho/dez pessoas | ReservaService, ParticipanteReservaService | rascunhoSoAvancaComDezParticipantesDeQualquerCategoria | Classes/DER; V | Implementado |
| Participantes/vínculo | ParticipanteReservaService | ParticipanteReservaPostgreSqlIntegrationTest | V/VI | ALUNO+OUTRO |
| PDF/assinatura externa | FormularioOficialService, DocumentoReservaService | Testes de formulário e documentos | VI | Implementado |
| Conferência | ConferenciaDocumentoService | ConferenciaDocumentoPostgreSqlIntegrationTest | IV/VI | Humana |
| Contribuição | ContribuicaoApmService | ContribuicaoApmPostgreSqlIntegrationTest | V/VI | Presencial |
| Aprovação/cancelamento | ReservaService | ReservaPostgreSqlIntegrationTest | IV | Implementado |
| Remarcação/crédito | ReservaService.criarRemarcacao | ReservaPostgreSqlIntegrationTest | Atividade07 | Novo PDF/aprovação |
| Presença/adicionais | ConsultaSegurancaService, PortariaService | PortariaPostgreSqlIntegrationTest | Atividade08 | Dia da reserva |
| Vaga paga | PortariaService.resumir | vagaPagaPorAdicionalPodeSerReutilizadaSemEstornoOuCobrancaDuplicada | IV/VI | Sem estorno |
| Repasse concorrente | PortariaService | duasConfirmacoesSimultaneasDoMesmoRepasseRegistramUmaSo | IV/VI | Secretaria confirma |
| Liberação | PortariaService | segurancaNaoPodeAlterarParticipantesDepoisDaLiberacao | IV/VI | Dez presentes; sem devido |

## Demonstração — 12 minutos
1. Problema institucional e quatro perfis (1min).
2. Aluno consulta agenda, salva rascunho e mostra PDF bloqueado (2min).
3. Lista fictícia preparada com nove pessoas; cadastrar a décima OUTRO e mostrar formalização (1min).
4. Gerar e enviar PDF sintético. Explicar assinatura externa; não alegar assinatura válida no teste (1min).
5. Secretaria confere e registra contribuição; administrador aprova (3min).
6. Segurança em390px registra presença. A paga/sai; B ocupa vaga; C geraR$2. Secretaria confirma repasse (2min).
7. Liberar e mostrar bloqueio. Demonstrar cancelamento/remarcação em cenário separado preparado (2min).

Usar banco dedicado, nunca dados reais. Credenciais fora dos slides. Ensaiar o dia da reserva em ambiente descartável. O PDF sintético demonstra fluxo, não autenticidade.

## Respostas técnicas
- Java/Spring: classes, validações, MVC, transações e autorização usam bibliotecas da stack. A motivação histórica da escolha deve ser confirmada pelos autores.
- Thymeleaf: renderiza páginas no servidor e permite manter uma aplicação única.
- PostgreSQL: chaves e transações preservam relações; lock da quadra serializa aprovações com reconferência de conflito.
- Dez pessoas: backend soma categorias, atualiza rascunho e bloqueia PDF antes da formalização.
- A paga e sai: AUSENTE preserva pessoa/pagamento; B ocupa capacidade já paga; só excedente gera complemento.
- Acesso indevido: perfis, titularidade e CSRF são conferidos no servidor; Segurança não baixa PDFs.
- Limites: sem pagamento online, sem validador criptográfico próprio e sem homologação institucional comprovada.
