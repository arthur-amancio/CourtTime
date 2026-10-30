# Reconciliação da modelagem e compatibilidade

Fonte técnica principal para classes, DER e fluxos alterados: conjunto editável Draw.io do PR88, atualizado para a árvore funcional4fb7a06. Fontes nativas Astah e suas exportações permanecem preservadas como conjunto histórico e complementar. Não inserir simultaneamente duas versões do mesmo diagrama no TCC final.

O inventário Astah de27/10 classifica05,06 e09A como pendentes visuais. Não foi realizada edição do .asta. Para reconciliar: incluir Reserva.rascunho:boolean em04; separar rascunho/formalização em07; explicar capacidade paga em09A; conferir imutabilidade após liberação em09B; alinhar e reexportar05/06/09A, verificando fontes em prova A4. Até essa revisão, os diagramas Draw.io correspondentes são a referência técnica desta missão. A equipe deve confirmar se há exigência de fonte Astah pela disciplina.

Novo atributo: Reserva.rascunho boolean, coluna reservas.rascunho NOT NULL DEFAULT false. Novos pedidos começam true. Ao atingir10 participantes documentais, false. Remoção permitida para menos de10 retorna true. RASCUNHO não é novo valor de StatusReserva. Geração/envio de PDF são bloqueados no backend; painel exclui rascunhos dos indicadores formais.

Registros antigos recebem false por padrão e não devem ser reclassificados cegamente. O SQL somente leitura identifica IDs suspeitos sem expor nomes ou documentos. Plano: backup restaurável; cópia isolada; diagnóstico; separar casos com documento/contribuição; revisão humana dos IDs; script transacional com filtro dos IDs aprovados; comparação de contagens/histórico; homologação em cópia. Nenhuma migração em banco real foi executada.

Teste desta missão: Maven clean verify com PostgreSQL isolado,477 executados,476 aprovados,0 falhas,1 erro ambiental de symlink,0 skips,1min43s. Log missao-java.log. Evidências Playwright de28/10 pertencem a4fb7a06 e não são automaticamente atribuídas aos merges desta missão.
