-- V5__checkin_token_laboratorio.sql
-- QR fixo de check-in por laboratório: cada laboratório tem um token aleatório,
-- impresso no QR da porta. O ADMIN pode gerar um novo (o QR antigo deixa de valer).

ALTER TABLE laboratories ADD COLUMN checkin_token VARCHAR(64) NULL;

-- Laboratórios já existentes ganham um token (64 caracteres hex; pgcrypto vem da V1)
UPDATE laboratories SET checkin_token = encode(gen_random_bytes(32), 'hex') WHERE checkin_token IS NULL;

ALTER TABLE laboratories ALTER COLUMN checkin_token SET NOT NULL;
ALTER TABLE laboratories ADD CONSTRAINT uk_laboratories_checkin_token UNIQUE (checkin_token);
