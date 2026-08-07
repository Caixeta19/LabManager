-- V1__create_schema.sql

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       nome VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       senha VARCHAR(255) NOT NULL,
                       matricula VARCHAR(100) NOT NULL UNIQUE,
                       tipo VARCHAR(20) NOT NULL,
                       ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE laboratories (
                              id BIGSERIAL PRIMARY KEY,
                              nome VARCHAR(255) NOT NULL,
                              localizacao VARCHAR(255) NOT NULL,
                              capacidade INTEGER NOT NULL,
                              descricao TEXT,
                              ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE equipments (
                            id BIGSERIAL PRIMARY KEY,
                            nome VARCHAR(255) NOT NULL,
                            patrimonio VARCHAR(100) NOT NULL UNIQUE,
                            descricao TEXT,
                            status VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
                            laboratory_id BIGINT NOT NULL,
                            CONSTRAINT fk_equipment_laboratory
                                FOREIGN KEY (laboratory_id) REFERENCES laboratories (id)
);

CREATE TABLE reserves (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          laboratory_id BIGINT NOT NULL,
                          data_hora_inicio TIMESTAMP NOT NULL,
                          data_hora_fim TIMESTAMP NOT NULL,
                          status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
                          token_confirmacao VARCHAR(255) NOT NULL UNIQUE,
                          codigo_qr VARCHAR(255) NOT NULL UNIQUE,
                          email_confirmacao_enviado BOOLEAN NOT NULL DEFAULT FALSE,
                          data_confirmacao TIMESTAMP,
                          CONSTRAINT fk_reserve_user
                              FOREIGN KEY (user_id) REFERENCES users (id),
                          CONSTRAINT fk_reserve_laboratory
                              FOREIGN KEY (laboratory_id) REFERENCES laboratories (id)
);

CREATE TABLE checkins (
                          id BIGSERIAL PRIMARY KEY,
                          reserve_id BIGINT NOT NULL UNIQUE,
                          horario_chegada TIMESTAMP NOT NULL DEFAULT now(),
                          CONSTRAINT fk_checkin_reserve
                              FOREIGN KEY (reserve_id) REFERENCES reserves (id)
);

CREATE TABLE loans (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       equipment_id BIGINT NOT NULL,
                       data_retirada TIMESTAMP NOT NULL DEFAULT now(),
                       data_devolucao_prevista TIMESTAMP NOT NULL,
                       data_devolucao_real TIMESTAMP,
                       status VARCHAR(20) NOT NULL DEFAULT 'EM_ANDAMENTO',
                       CONSTRAINT fk_loan_user
                           FOREIGN KEY (user_id) REFERENCES users (id),
                       CONSTRAINT fk_loan_equipment
                           FOREIGN KEY (equipment_id) REFERENCES equipments (id)
);

-- Índices para as buscas mais comuns (conflito de horário, filtros por status)
CREATE INDEX idx_reserves_laboratory_periodo ON reserves (laboratory_id, data_hora_inicio, data_hora_fim);
CREATE INDEX idx_reserves_user ON reserves (user_id);
CREATE INDEX idx_loans_status ON loans (status);
CREATE INDEX idx_equipments_laboratory ON equipments (laboratory_id);