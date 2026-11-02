-- V2__alinha_schema_ao_readme.sql
-- Fase 2 (decisões 3, 6, 7 e 14 de docs/fase2_implementar.md)
--   * renomeia as colunas para inglês, seguindo o README §8
--   * datas passam para TIMESTAMPTZ (valores existentes interpretados em America/Sao_Paulo)
--   * loans.data_devolucao_prevista passa a ser DATE
--   * colunas de bloqueio de conta em users
--   * created_at em reserves e índices parciais do README

-- ---------- users ----------
ALTER TABLE users RENAME COLUMN nome      TO name;
ALTER TABLE users RENAME COLUMN senha     TO password;
ALTER TABLE users RENAME COLUMN matricula TO registration;
ALTER TABLE users RENAME COLUMN tipo      TO type;
ALTER TABLE users RENAME COLUMN ativo     TO active;

ALTER TABLE users ADD COLUMN failed_attempts INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until    TIMESTAMPTZ NULL;

-- ---------- laboratories ----------
ALTER TABLE laboratories RENAME COLUMN nome        TO name;
ALTER TABLE laboratories RENAME COLUMN localizacao TO localization;
ALTER TABLE laboratories RENAME COLUMN capacidade  TO capacity;
ALTER TABLE laboratories RENAME COLUMN descricao   TO description;
ALTER TABLE laboratories RENAME COLUMN ativo       TO active;

-- ---------- equipments ----------
ALTER TABLE equipments RENAME COLUMN nome       TO name;
ALTER TABLE equipments RENAME COLUMN patrimonio TO heritage;
ALTER TABLE equipments RENAME COLUMN descricao  TO description;

-- ---------- reserves ----------
ALTER TABLE reserves
    ALTER COLUMN data_hora_inicio TYPE TIMESTAMPTZ USING data_hora_inicio AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN data_hora_fim    TYPE TIMESTAMPTZ USING data_hora_fim    AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN data_confirmacao TYPE TIMESTAMPTZ USING data_confirmacao AT TIME ZONE 'America/Sao_Paulo';

ALTER TABLE reserves ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- ---------- checkins ----------
ALTER TABLE checkins
    ALTER COLUMN horario_chegada TYPE TIMESTAMPTZ USING horario_chegada AT TIME ZONE 'America/Sao_Paulo';

-- ---------- loans ----------
UPDATE loans SET status = 'ATIVO' WHERE status = 'EM_ANDAMENTO';
ALTER TABLE loans ALTER COLUMN status SET DEFAULT 'ATIVO';

ALTER TABLE loans
    ALTER COLUMN data_retirada           TYPE TIMESTAMPTZ USING data_retirada       AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN data_devolucao_real     TYPE TIMESTAMPTZ USING data_devolucao_real AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN data_devolucao_prevista TYPE DATE        USING data_devolucao_prevista::date;

-- ---------- índices parciais (README §8) ----------
-- acelera a query de conflito de horário
CREATE INDEX idx_reserves_laboratorio_horario
    ON reserves (laboratory_id, data_hora_inicio, data_hora_fim)
    WHERE status NOT IN ('CANCELADA', 'EXPIRADA');

-- acelera o job de e-mail de confirmação
CREATE INDEX idx_reserves_email_pendente
    ON reserves (data_hora_inicio)
    WHERE email_confirmacao_enviado = FALSE AND status = 'PENDENTE';

-- acelera o job de empréstimos atrasados
CREATE INDEX idx_loans_atrasados
    ON loans (data_devolucao_prevista)
    WHERE status = 'ATIVO' AND data_devolucao_real IS NULL;
