package com.example.lab_manager.repository;

import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.model.Reserve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReserveRepository extends JpaRepository<Reserve, UUID> {

    Optional<Reserve> findByConfirmationToken(String token);

    Optional<Reserve> findByQRCode(String qrCode);

    List<Reserve> findByUserId (UUID userId);

    List<Reserve> findByLaboratoryIdAndStatus(UUID laboratoryId, ReserveStatus status);

   // Usada pra checar conflito de horário no mesmo laboratório
    List<Reserve>  findByLaboratoryIdAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(UUID laboratoryId, LocalDateTime dataHora, LocalDateTime dataFim);

    List<Reserve> findByEmailConfirmacaoEnviadoFalseAndDataHoraInicioBetween(LocalDateTime dataHora, LocalDateTime dataFim);

}
