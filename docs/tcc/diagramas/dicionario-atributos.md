# Dicionário de atributos e referências JPA

Base: `7b40dcb1aeeb97b60cef751e0bbda7b7c138de73`. Campos persistidos; acesso às propriedades via métodos públicos. Nulidade refere-se ao mapeamento, não à permissividade do fluxo. Identificadores implícitos em snake_case são derivados da convenção Hibernate/Spring Boot, sem introspecção de banco vivo.

## AjusteContribuicaoApm → ajustes_contribuicao_apm

Evidência: [AjusteContribuicaoApm.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/AjusteContribuicaoApm.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| contribuicao | ContribuicaoApm | contribuicao_id | não | FK → contribuicoes_apm(id) |
| valorAnterior | BigDecimal | valor_anterior | não | valor |
| valorNovo | BigDecimal | valor_novo | não | valor |
| observacoesAnteriores | String | observacoes_anteriores | sim | valor |
| observacoesNovas | String | observacoes_novas | sim | valor |
| motivo | String | motivo | não | valor |
| ajustadaPor | Usuario | ajustada_por_id | não | FK → usuarios(id) |
| ajustadaEm | Instant | ajustada_em | não | valor |

## AuditoriaUsuario → auditorias_usuario

Evidência: [AuditoriaUsuario.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/AuditoriaUsuario.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| operador | Usuario | operador_id | não | FK → usuarios(id) |
| usuarioAfetado | Usuario | usuario_afetado_id | não | FK → usuarios(id) |
| operacao | OperacaoAuditoriaUsuario | operacao | não | valor |
| perfilAnterior | Perfil | perfil_anterior | sim | valor |
| perfilNovo | Perfil | perfil_novo | sim | valor |
| ativoAnterior | Boolean | ativo_anterior | sim | valor |
| ativoNovo | Boolean | ativo_novo | sim | valor |
| realizadaEm | Instant | realizada_em | não | valor |

## BloqueioQuadra → bloqueios_quadra

Evidência: [BloqueioQuadra.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/BloqueioQuadra.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| data | LocalDate | data | não | valor |
| horarioInicio | LocalTime | horario_inicio | não | valor |
| horarioFim | LocalTime | horario_fim | não | valor |
| motivo | String | motivo | não | valor |
| ativo | boolean | ativo | não | valor |
| quadra | Quadra | quadra_id | não | FK → quadras(id) |

## ConferenciaDocumento → conferencias_documento

Evidência: [ConferenciaDocumento.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/ConferenciaDocumento.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| documento | DocumentoReserva | documento_id | não | FK → documentos_reserva(id); UQ |
| resultado | ResultadoConferenciaDocumento | resultado | não | valor |
| justificativa | String | justificativa | sim | valor |
| conferidoPor | Usuario | conferido_por_id | não | FK → usuarios(id) |
| conferidoEm | Instant | conferido_em | não | valor |

## ContribuicaoApm → contribuicoes_apm

Evidência: [ContribuicaoApm.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/ContribuicaoApm.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| reserva | Reserva | reserva_id | não | FK → reservas(id); UQ |
| conferenciaDocumento | ConferenciaDocumento | conferencia_documento_id | não | FK → conferencias_documento(id); UQ |
| valor | BigDecimal | valor | não | valor |
| observacoes | String | observacoes | sim | valor |
| registradaPor | Usuario | registrada_por_id | não | FK → usuarios(id) |
| registradaEm | Instant | registrada_em | não | valor |

## DisponibilidadeQuadra → disponibilidades_quadra

Evidência: [DisponibilidadeQuadra.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/DisponibilidadeQuadra.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| diaSemana | DiaSemana | dia_semana | não | valor |
| horarioInicio | LocalTime | horario_inicio | não | valor |
| horarioFim | LocalTime | horario_fim | não | valor |
| ativa | boolean | ativa | não | valor |
| quadra | Quadra | quadra_id | não | FK → quadras(id) |

## DocumentoReserva → documentos_reserva

Evidência: [DocumentoReserva.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/DocumentoReserva.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| reserva | Reserva | reserva_id | não | FK → reservas(id) |
| versao | int | versao | não | valor |
| revisaoParticipantes | int | revisao_participantes | não | valor |
| nomeOriginal | String | nome_original | não | valor |
| chaveArmazenamento | String | chave_armazenamento | não | valor; UQ |
| sha256 | String | sha256 | não | valor |
| tamanhoBytes | long | tamanho_bytes | não | valor |
| enviadoPor | Usuario | enviado_por_id | não | FK → usuarios(id) |
| enviadoEm | Instant | enviado_em | não | valor |

## ParticipanteReserva → participantes_reserva

Evidência: [ParticipanteReserva.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/ParticipanteReserva.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| reserva | Reserva | reserva_id | não | FK → reservas(id) |
| nomeCompleto | String | nome_completo | não | valor |
| rmOuRg | String | rm_ou_rg | não | valor |
| categoria | CategoriaParticipante | categoria | não | valor |

## Quadra → quadras

Evidência: [Quadra.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/Quadra.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| nome | String | nome | não | valor |
| descricao | String | descricao | não | valor |
| ativa | boolean | ativa | não | valor |

## Reserva → reservas

Evidência: [Reserva.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/Reserva.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| quadra | Quadra | quadra_id | não | FK → quadras(id) |
| solicitante | Usuario | solicitante_id | não | FK → usuarios(id) |
| dataUtilizacao | LocalDate | data_utilizacao | não | valor |
| horarioInicio | LocalTime | horario_inicio | não | valor |
| horarioFim | LocalTime | horario_fim | não | valor |
| finalidade | String | finalidade | não | valor |
| status | StatusReserva | status | não | valor |
| revisaoParticipantes | int | revisao_participantes | não | valor |
| analisadaPor | Usuario | analisada_por_id | sim | FK → usuarios(id) |
| motivoRejeicao | String | motivo_rejeicao | sim | valor |
| motivoCancelamentoInstitucional | String | motivo_cancelamento_institucional | sim | valor |
| canceladaPor | Usuario | cancelada_por_id | sim | FK → usuarios(id) |
| remarcacaoDe | Reserva | remarcacao_de_id | sim | FK → reservas(id); UQ |
| criadaEm | Instant | criada_em | não | valor |
| atualizadaEm | Instant | atualizada_em | não | valor |

## Usuario → usuarios

Evidência: [Usuario.java](https://github.com/arthur-amancio/CourtTime/blob/7b40dcb1aeeb97b60cef751e0bbda7b7c138de73/src/main/java/br/com/courttime/entity/Usuario.java)

| Atributo Java | Tipo | Coluna | Nulo | Papel / referência |
|---|---|---|---|---|
| id | Long | id | não | PK IDENTITY |
| nome | String | nome | não | valor |
| email | String | email | não | valor; UQ |
| senha | String | senha | não | valor |
| perfil | Perfil | perfil | não | valor |
| ativo | boolean | ativo | não | valor |
| versaoAcesso | long | versao_acesso | não | valor |

## Restrições adicionais

- documentos_reserva: UQ(reserva_id, versao); UQ(chave_armazenamento).
- conferencias_documento: UQ(documento_id).
- contribuicoes_apm: UQ(reserva_id), UQ(conferencia_documento_id).
- reservas: UQ(remarcacao_de_id), referência opcional à própria tabela.
- usuarios: UQ(email).
- participantes_reserva: não há UQ(reserva_id, rm_ou_rg) declarada no JPA; duplicação é impedida pelo serviço com bloqueio da reserva.
- Não há cascade REMOVE nem orphanRemoval modelados nas associações examinadas. Não se apresenta composição com semântica de exclusão.
- DocumentoReserva.conferencia é associação inversa mappedBy; não gera uma segunda coluna de FK em documentos_reserva.
- Índices declarados: reservas(quadra_id,data_utilizacao,status), reservas(solicitante_id), bloqueios_quadra(quadra_id,data), participantes_reserva(reserva_id), documentos_reserva(reserva_id,versao), conferencias_documento(conferido_em), contribuicoes_apm(registrada_em), ajustes_contribuicao_apm(contribuicao_id,ajustada_em), auditorias_usuario(usuario_afetado_id,realizada_em). Conferir nome e composição exata com a anotação de cada entidade.
- Períodos de uma hora, mínimo de alunos, compatibilidade documental e ausência de sobreposição são regras Java/transacionais, não deduzíveis apenas das FKs.
