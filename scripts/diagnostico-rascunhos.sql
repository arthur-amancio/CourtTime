-- Somente leitura; não retorna nomes, identificadores pessoais ou PDFs.
BEGIN TRANSACTION READ ONLY;
SELECT current_database(), current_setting('server_version');
SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='reservas' AND column_name='rascunho') AS coluna_existe;
-- Executar abaixo somente se coluna_existe=true.
SELECT r.id, r.status, r.rascunho,
 count(p.id) FILTER (WHERE coalesce(p.adicionado_na_portaria,false)=false) AS participantes_documentais,
 EXISTS(SELECT 1 FROM documentos_reserva d WHERE d.reserva_id=r.id) AS possui_documento
FROM reservas r LEFT JOIN participantes_reserva p ON p.reserva_id=r.id
WHERE r.status='PENDENTE_DOCUMENTACAO'
GROUP BY r.id,r.status,r.rascunho
HAVING (NOT r.rascunho AND count(p.id) FILTER (WHERE coalesce(p.adicionado_na_portaria,false)=false)<10)
 OR (r.rascunho AND count(p.id) FILTER (WHERE coalesce(p.adicionado_na_portaria,false)=false)>=10)
ORDER BY r.id;
ROLLBACK;
