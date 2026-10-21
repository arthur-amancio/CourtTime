-- CourtTime — dados ficticios para DESENVOLVIMENTO LOCAL / DEMONSTRACAO.
-- Execute pelo Query Tool do pgAdmin 4 conectado EXCLUSIVAMENTE a courttime_demo.
-- PRIMEIRO: abra o CourtTime uma vez para o Hibernate criar as tabelas.
-- SEGUNDO: troque o valor TROQUE_A_SENHA_DEMO_AQUI por uma senha ficticia
-- de 8 a 72 caracteres. NAO salve nem publique sua senha no Git.
-- Usuarios criados nao tem a senha redefinida se este script for repetido.
-- Este script NAO limpa banco, NAO apaga tabelas nem cria PDFs falsos.
BEGIN;
SET LOCAL TIME ZONE 'America/Sao_Paulo';
SET LOCAL courttime.demo_password = 'TROQUE_A_SENHA_DEMO_AQUI';

-- Rejeitar outro banco antes de criar extensoes ou inserir qualquer dado.
DO $$
DECLARE
    senha text := current_setting('courttime.demo_password', true);
BEGIN
    IF current_database() <> 'courttime_demo' THEN
        RAISE EXCEPTION 'Seed recusado: banco atual (%) nao e courttime_demo.', current_database();
    END IF;
    IF senha IS NULL OR senha = 'TROQUE_A_SENHA_DEMO_AQUI'
       OR length(senha) < 8 OR length(senha) > 72 THEN
        RAISE EXCEPTION 'Troque TROQUE_A_SENHA_DEMO_AQUI por uma senha ficticia de 8 a 72 caracteres.';
    END IF;
    IF to_regclass('public.usuarios') IS NULL
       OR to_regclass('public.quadras') IS NULL
       OR to_regclass('public.reservas') IS NULL
       OR to_regclass('public.disponibilidades_quadra') IS NULL
       OR to_regclass('public.bloqueios_quadra') IS NULL
       OR to_regclass('public.participantes_reserva') IS NULL THEN
        RAISE EXCEPTION 'Tabelas CourtTime ausentes: execute a aplicacao uma vez antes do SQL.';
    END IF;
    -- Colunas das regras da ETEC (R09): exigem o CourtTime atualizado.
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'participantes_reserva'
          AND column_name = 'vinculo'
    ) THEN
        RAISE EXCEPTION 'Coluna participantes_reserva.vinculo ausente: atualize o CourtTime e abra a aplicacao uma vez.';
    END IF;
    IF (SELECT count(*) FROM public.quadras) <> 1 THEN
        RAISE EXCEPTION 'O demo exige uma unica quadra cadastrada. Confira o banco antes.';
    END IF;
END $$;

-- pgcrypto fornece BCrypt compativel com o PasswordEncoder Spring Security.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 17 contas ficticias: 1 administrador, 2 secretarias, 2 segurancas e 12 alunos.
-- Senha nao e publicada: substitua apenas no editor do pgAdmin.
WITH pessoas AS (
    SELECT nome, email, perfil FROM (VALUES
        ('Administrador Demonstração', 'admin.demo@exemplo.invalid', 'ADMINISTRADOR'),
        ('Secretaria Demo 1', 'secretaria1.demo@exemplo.invalid', 'SECRETARIA'),
        ('Secretaria Demo 2', 'secretaria2.demo@exemplo.invalid', 'SECRETARIA'),
        ('Segurança Demo 1', 'seguranca1.demo@exemplo.invalid', 'SEGURANCA'),
        ('Segurança Demo 2', 'seguranca2.demo@exemplo.invalid', 'SEGURANCA')
    ) AS p(nome, email, perfil)
    UNION ALL
    SELECT 'Aluno Demo ' || lpad(n::text, 2, '0'),
           'aluno' || lpad(n::text, 2, '0') || '.demo@exemplo.invalid',
           'ALUNO'
    FROM generate_series(1, 12) n
)
INSERT INTO usuarios (nome, email, senha, perfil, ativo, versao_acesso)
SELECT p.nome, p.email,
       crypt(current_setting('courttime.demo_password'), gen_salt('bf', 10)),
       p.perfil, true, 0
FROM pessoas p
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios u WHERE lower(u.email) = lower(p.email)
);

-- Grade semanal ficticia (NAO representa horario oficial da ETEC).
-- Segunda a quinta, manha e noite. Sexta/domingo permanecem como estao.
WITH horarios(dia, inicio, fim) AS (
    VALUES
      ('SEGUNDA', time '08:00', time '12:00'),
      ('SEGUNDA', time '18:00', time '22:00'),
      ('TERCA',   time '09:00', time '12:00'),
      ('TERCA',   time '18:00', time '22:00'),
      ('QUARTA',  time '08:00', time '12:00'),
      ('QUARTA',  time '18:00', time '22:00'),
      ('QUINTA',  time '08:00', time '12:00'),
      ('QUINTA',  time '18:00', time '21:00')
)
INSERT INTO disponibilidades_quadra
    (dia_semana, horario_inicio, horario_fim, ativa, quadra_id)
SELECT h.dia, h.inicio, h.fim, true, q.id
FROM horarios h CROSS JOIN quadras q
WHERE NOT EXISTS (
    SELECT 1 FROM disponibilidades_quadra d
    WHERE d.quadra_id = q.id AND d.dia_semana = h.dia
      AND d.horario_inicio < h.fim AND d.horario_fim > h.inicio
);

-- Bloqueios distribuídos pelos proximos 8 meses.
-- Dois ativos e um inativo por mes (o inativo demonstra desbloqueio).
WITH meses AS (
    SELECT (date_trunc('month', current_date)::date
             + (n * interval '1 month'))::date AS primeiro
    FROM generate_series(0, 7) n
),
segundas AS (
    SELECT (primeiro
            + ((8 - extract(isodow FROM primeiro)::int) % 7))::date AS segunda
    FROM meses
),
bloqueios AS (
    SELECT (segunda + 7)::date AS dia,
           time '18:00' AS inicio, time '20:00' AS fim,
           'DEMO - manutenção preventiva fictícia'::text AS motivo, true AS ativo
    FROM segundas
    UNION ALL
    SELECT (segunda + 9)::date, time '18:00', time '19:00',
           'DEMO - evento institucional fictício', true
    FROM segundas
    UNION ALL
    SELECT (segunda + 15)::date, time '18:00', time '19:00',
           'DEMO - bloqueio desativado para teste', false
    FROM segundas
)
INSERT INTO bloqueios_quadra
    (data, horario_inicio, horario_fim, motivo, ativo, quadra_id)
SELECT b.dia, b.inicio, b.fim, b.motivo, b.ativo, q.id
FROM bloqueios b CROSS JOIN quadras q
WHERE NOT EXISTS (
    SELECT 1 FROM bloqueios_quadra antigo
    WHERE antigo.quadra_id = q.id AND antigo.data = b.dia
      AND antigo.horario_inicio = b.inicio AND antigo.horario_fim = b.fim
      AND antigo.motivo = b.motivo
)
AND NOT EXISTS (
    SELECT 1 FROM reservas r
    WHERE b.ativo AND r.quadra_id = q.id
      AND r.data_utilizacao = b.dia AND r.status = 'APROVADA'
      AND r.horario_inicio < b.fim AND r.horario_fim > b.inicio
);

-- Solicitacoes preliminares ficticias por cerca de 9 meses (passado e futuro).
-- NAO seedar APROVADA: esse estado exige documento, conferencia e APM reais.
WITH dias AS (
    SELECT g.dia::date AS data
    FROM generate_series(
        (current_date - interval '56 days')::date,
        (current_date + interval '210 days')::date,
        interval '1 day'
    ) AS g(dia)
    WHERE extract(isodow FROM g.dia) BETWEEN 1 AND 4
      AND mod(extract(doy FROM g.dia)::int, 2) = 0
),
periodos AS (
    SELECT d.data,
           CASE extract(isodow FROM d.data)::int
               WHEN 1 THEN time '09:00'
               WHEN 2 THEN time '19:00'
               WHEN 3 THEN time '10:00'
               ELSE time '20:00'
           END AS inicio
    FROM dias d
),
solicitacoes AS (
    SELECT p.data, p.inicio, (p.inicio + interval '1 hour')::time AS fim,
           ('[DEMO] Treino fictício de ' || to_char(p.data, 'DD/MM/YYYY')) AS finalidade,
           'aluno' ||
             lpad(((extract(doy FROM p.data)::int % 12) + 1)::text, 2, '0')
             || '.demo@exemplo.invalid' AS email
    FROM periodos p
)
INSERT INTO reservas
    (quadra_id, solicitante_id, data_utilizacao, horario_inicio,
     horario_fim, finalidade, status, revisao_participantes,
     criada_em, atualizada_em)
SELECT q.id, u.id, s.data, s.inicio, s.fim,
       s.finalidade, 'PENDENTE_DOCUMENTACAO', 0,
       CASE WHEN s.data < current_date
            THEN ((s.data - 15)::timestamp AT TIME ZONE 'America/Sao_Paulo')
            ELSE now() END,
       CASE WHEN s.data < current_date
            THEN ((s.data - 15)::timestamp AT TIME ZONE 'America/Sao_Paulo')
            ELSE now() END
FROM solicitacoes s
JOIN usuarios u ON u.email = s.email
CROSS JOIN quadras q
WHERE EXISTS (
    SELECT 1 FROM disponibilidades_quadra d
    WHERE d.quadra_id = q.id AND d.ativa = true
      AND d.dia_semana = (
          CASE extract(isodow FROM s.data)::int
              WHEN 1 THEN 'SEGUNDA' WHEN 2 THEN 'TERCA'
              WHEN 3 THEN 'QUARTA' ELSE 'QUINTA'
          END)
      AND d.horario_inicio <= s.inicio AND d.horario_fim >= s.fim
)
AND NOT EXISTS (
    SELECT 1 FROM bloqueios_quadra b
    WHERE b.quadra_id = q.id AND b.ativo = true
      AND b.data = s.data
      AND b.horario_inicio < s.fim AND b.horario_fim > s.inicio
)
AND NOT EXISTS (
    SELECT 1 FROM reservas r
    WHERE r.quadra_id = q.id
      AND r.data_utilizacao = s.data
      AND r.horario_inicio = s.inicio
);

-- Coloque dez participantes ficticios nas primeiras 18 solicitacoes futuras.
-- Reexecucao nao duplica os participantes existentes.
WITH alvos AS (
    SELECT id FROM reservas
    WHERE finalidade LIKE '[DEMO]%'
      AND status = 'PENDENTE_DOCUMENTACAO'
      AND data_utilizacao >= current_date + 1
    ORDER BY data_utilizacao, id
    LIMIT 18
),
novos AS (
    SELECT a.id AS reserva_id, n,
           ('Participante Fictício ' || n) AS nome,
           ('RM-DEMO-' || a.id || '-' || n) AS rm
    FROM alvos a CROSS JOIN generate_series(1,10) n
)
INSERT INTO participantes_reserva
    (reserva_id, nome_completo, rm_ou_rg, categoria)
SELECT p.reserva_id, p.nome, p.rm, 'ALUNO'
FROM novos p
WHERE NOT EXISTS (
    SELECT 1 FROM participantes_reserva atual
    WHERE atual.reserva_id = p.reserva_id AND atual.rm_ou_rg = p.rm
);

-- R09: nas tres primeiras, dois participantes da categoria Outro com vinculo.
-- Assim a secretaria ve "12 participantes x R$ 2,00 = R$ 24,00" (R03).
WITH alvos AS (
    SELECT id FROM reservas
    WHERE finalidade LIKE '[DEMO]%'
      AND status = 'PENDENTE_DOCUMENTACAO'
      AND data_utilizacao >= current_date + 1
    ORDER BY data_utilizacao, id
    LIMIT 3
),
outros(ordem, nome, vinculo) AS (
    VALUES (1, 'Responsável Fictício', 'PAI_OU_MAE'),
           (2, 'Ex-aluno Fictício', 'EX_ALUNO')
),
novos AS (
    SELECT a.id AS reserva_id, o.nome, o.vinculo,
           ('RG-DEMO-' || a.id || '-' || o.ordem) AS rg
    FROM alvos a CROSS JOIN outros o
)
INSERT INTO participantes_reserva
    (reserva_id, nome_completo, rm_ou_rg, categoria, vinculo)
SELECT n.reserva_id, n.nome, n.rg, 'OUTRO', n.vinculo
FROM novos n
WHERE NOT EXISTS (
    SELECT 1 FROM participantes_reserva atual
    WHERE atual.reserva_id = n.reserva_id AND atual.rm_ou_rg = n.rg
)
AND NOT EXISTS (
    SELECT 1 FROM documentos_reserva d WHERE d.reserva_id = n.reserva_id
);

-- Mantem o contador consistente nas reservas geradas que ainda nao tem PDF.
UPDATE reservas r
SET revisao_participantes = (
        SELECT count(*)::integer FROM participantes_reserva p WHERE p.reserva_id = r.id
    ),
    atualizada_em = now()
WHERE r.finalidade LIKE '[DEMO]%'
  AND r.status = 'PENDENTE_DOCUMENTACAO'
  AND NOT EXISTS (SELECT 1 FROM documentos_reserva d WHERE d.reserva_id = r.id)
  AND r.revisao_participantes <> (
        SELECT count(*)::integer FROM participantes_reserva p WHERE p.reserva_id = r.id)
  AND (SELECT count(*) FROM participantes_reserva p WHERE p.reserva_id = r.id) >= 10;

-- Resumo conferivel; todos os registros so existem em courttime_demo.
SELECT 'Contas ficticias' AS item, count(*) AS total
  FROM usuarios WHERE email LIKE '%.demo@exemplo.invalid'
UNION ALL
SELECT 'Disponibilidades de segunda a quinta', count(*)
  FROM disponibilidades_quadra WHERE dia_semana IN ('SEGUNDA','TERCA','QUARTA','QUINTA')
UNION ALL
SELECT 'Bloqueios DEMO ativos', count(*)
  FROM bloqueios_quadra WHERE motivo LIKE 'DEMO -%' AND ativo
UNION ALL
SELECT 'Bloqueios DEMO inativos', count(*)
  FROM bloqueios_quadra WHERE motivo LIKE 'DEMO -%' AND NOT ativo
UNION ALL
SELECT 'Solicitacoes DEMO', count(*)
  FROM reservas WHERE finalidade LIKE '[DEMO]%'
UNION ALL
SELECT 'Participantes de solicitacoes DEMO', count(*)
  FROM participantes_reserva p JOIN reservas r ON r.id=p.reserva_id
  WHERE r.finalidade LIKE '[DEMO]%'
UNION ALL
SELECT 'Participantes Outro com vinculo (R09)', count(*)
  FROM participantes_reserva p JOIN reservas r ON r.id=p.reserva_id
  WHERE r.finalidade LIKE '[DEMO]%' AND p.categoria = 'OUTRO' AND p.vinculo IS NOT NULL;

COMMIT;
