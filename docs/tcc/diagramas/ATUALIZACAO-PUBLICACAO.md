# Atualização de publicação — 22/10/2026

As 26 figuras são uma modelagem versionada da base 7b40dcb (21/10/2026) e do plano final então disponível. Durante a interrupção, a main passou a 4df490729734d9f45830e15be241cdb66c3f112f. Os rótulos de implementação das figuras e os achados do relatório são relativos à base original, não uma certificação da main atual.

Na conferência de publicação, foram lidos o commit #82 e as versões atuais de ContribuicaoApm, ParticipanteReserva, ConferenciaDocumento, VinculoParticipante, PresencaParticipante, ContribuicaoApmService, ConsultaSegurancaService e DocumentoReservaService.

Mudanças confirmadas no código atual:
- O envio de PDF é recusado a partir do dia do uso.
- ContribuicaoApm possui valorDevido, creditoAplicado e contribuicaoOrigem. valor admite zero; o achado de incompatibilidade com crédito integral do relatório original foi superado.
- contribuicaoOrigem é ManyToOne, sem UQ nessa FK. A UQ desenhada no modelo proposto não é uma restrição implementada.
- ParticipanteReserva possui vinculo, presenca, presencaRegistradaPor e presencaRegistradaEm. PresencaParticipante contém PRESENTE e AUSENTE; null permanece não registrado.
- ConsultaSegurancaService registra presença em reserva aprovada no dia, sob bloqueio da reserva; não adiciona pessoas extras.
- ConferenciaDocumento possui assinaturaGovBrConferida; continua sendo registro de conferência humana.
- VinculoParticipante implementa EX_ALUNO, PAI_OU_MAE, IRMAO_OU_IRMA e PROFESSOR. Isso não resolve a divergência institucional com o termo.
- saldoExcedente e PessoaAdicionalReserva continuam propostas desta modelagem; não foram encontrados como campos/entidade nos arquivos atuais consultados.

A reconciliação integral das 26 figuras, do dicionário e da matriz com a nova base permanece pendente. O PR permanece draft e não está pronto para merge nem para substituição definitiva das figuras do TCC. A publicação usa a árvore atual da main para não carregar código antigo no diff.

Evidência atual: https://github.com/arthur-amancio/CourtTime/commit/4df490729734d9f45830e15be241cdb66c3f112f
