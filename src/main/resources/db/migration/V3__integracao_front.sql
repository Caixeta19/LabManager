-- V3__integracao_front.sql
-- Integração com o front (docs/promp_integracao.md). Só acrescenta colunas e tabelas.

-- AdminUsers: "último acesso"
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMPTZ NULL;

-- Labs / AdminLabs: horário, recursos (um por linha) e status "Manutenção"
ALTER TABLE laboratories ADD COLUMN opening_hours VARCHAR(120) NULL;
ALTER TABLE laboratories ADD COLUMN resources     TEXT         NULL;
ALTER TABLE laboratories ADD COLUMN maintenance   BOOLEAN      NOT NULL DEFAULT FALSE;

-- AdminEquipment: responsável
ALTER TABLE equipments ADD COLUMN responsible VARCHAR(255) NULL;

-- NewReservation: motivo da solicitação
ALTER TABLE reserves ADD COLUMN reason VARCHAR(500) NULL;

-- "Esqueci minha senha": guarda só o hash SHA-256 do token
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ  NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
);

CREATE INDEX idx_password_reset_tokens_user
    ON password_reset_tokens (user_id);
