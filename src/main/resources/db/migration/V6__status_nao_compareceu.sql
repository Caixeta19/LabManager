-- V6__status_nao_compareceu.sql
-- Novo status de reserva NAO_COMPARECEU: reserva CONFIRMADA que terminou sem check-in.
-- A coluna reserves.status é VARCHAR(20) sem CHECK, então o valor novo (14 caracteres) não exige ALTER.
-- Índice parcial para o job NaoCompareceuScheduler, que roda a cada minuto.

CREATE INDEX idx_reserves_confirmada_fim
    ON reserves (data_hora_fim)
    WHERE status = 'CONFIRMADA';
