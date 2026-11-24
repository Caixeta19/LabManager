-- V4__janela_de_reserva.sql
-- Janela diária em que ALUNO e PROFESSOR podem reservar o laboratório.
-- As duas colunas nulas = sem restrição de horário. ADMIN ignora a janela.

ALTER TABLE laboratories ADD COLUMN booking_open_time  TIME NULL;
ALTER TABLE laboratories ADD COLUMN booking_close_time TIME NULL;

ALTER TABLE laboratories ADD CONSTRAINT ck_laboratories_booking_window
    CHECK (
        (booking_open_time IS NULL AND booking_close_time IS NULL)
        OR (booking_open_time IS NOT NULL AND booking_close_time IS NOT NULL
            AND booking_open_time < booking_close_time)
    );
