-- V7__dias_de_funcionamento.sql
-- Dias da semana em que o laboratório funciona (e aceita reservas de ALUNO/PROFESSOR).
-- Números ISO separados por vírgula: 1 = segunda … 7 = domingo. Ex.: '1,2,3,4,5'.
-- NULL = sem restrição de dia (laboratórios já existentes continuam como estavam). ADMIN ignora.

ALTER TABLE laboratories ADD COLUMN booking_days VARCHAR(20) NULL;
