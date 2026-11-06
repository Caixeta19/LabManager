package com.example.lab_manager.service;

import com.example.lab_manager.dto.response.PublicSummaryDTO;
import com.example.lab_manager.enums.PublicLaboratoryStatus;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.EquipmentRepository;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.repository.ReserveRepository;
import com.example.lab_manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Números e status exibidos na tela de login, antes de qualquer autenticação. */
@Service
@RequiredArgsConstructor
public class PublicSummaryService {

    private final LaboratoryRepository laboratoryRepository;
    private final ReserveRepository reserveRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PublicSummaryDTO resumo() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime inicioDoDia = agora.toLocalDate().atStartOfDay();

        Set<UUID> emUso = reserveRepository.findLaboratoriosEmUso(ReserveStatus.CONFIRMADA, agora);
        List<PublicSummaryDTO.LaboratorioDTO> laboratorios = laboratoryRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Laboratory::getName))
                .map(lab -> new PublicSummaryDTO.LaboratorioDTO(lab.getName(), statusDe(lab, emUso)))
                .toList();

        return new PublicSummaryDTO(
                laboratoryRepository.countByActiveTrueAndMaintenanceFalse(),
                reserveRepository.countComInicioEntre(inicioDoDia, inicioDoDia.plusDays(1),
                        ReserveRepository.STATUS_IGNORADOS_NO_CONFLITO),
                equipmentRepository.count(),
                userRepository.countByActiveTrue(),
                laboratorios
        );
    }

    private static PublicLaboratoryStatus statusDe(Laboratory lab, Set<UUID> emUso) {
        if (lab.isMaintenance()) {
            return PublicLaboratoryStatus.MANUTENCAO;
        }
        return emUso.contains(lab.getId()) ? PublicLaboratoryStatus.EM_USO_AGORA : PublicLaboratoryStatus.DISPONIVEL;
    }
}
