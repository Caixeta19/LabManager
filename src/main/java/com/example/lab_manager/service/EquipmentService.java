package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.EquipmentRequestDTO;
import com.example.lab_manager.dto.response.EquipmentResponseDTO;
import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.Equipment;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final LaboratoryService laboratoryService;

    @Transactional
    public EquipmentResponseDTO criar(EquipmentRequestDTO dto) {
        Laboratory laboratory = laboratoryService.buscarEntidadePorId(dto.laboratoryId());

        Equipment equipment = Equipment.builder()
                .name(dto.name())
                .heritage(dto.heritage())
                .description(dto.description())
                .status(EquipmentStatus.DISPONIVEL)
                .laboratory(laboratory)
                .build();

        return EquipmentResponseDTO.fromEntity(equipmentRepository.save(equipment));
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponseDTO> listarPorLaboratorio(UUID laboratoryId) {
        return equipmentRepository.findByLaboratoryId(laboratoryId).stream()
                .map(EquipmentResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponseDTO> listarDisponiveis() {
        return equipmentRepository.findByStatus(EquipmentStatus.DISPONIVEL).stream()
                .map(EquipmentResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    void alterarStatus(UUID equipmentId, EquipmentStatus novoStatus) {
        Equipment equipment = buscarEntidadePorId(equipmentId);

        if (novoStatus == EquipmentStatus.EMPRESTADO && equipment.getStatus() != EquipmentStatus.DISPONIVEL) {
            throw new RegraDeNegocioException("Equipamento não está disponível para empréstimo.");
        }

        equipment.setStatus(novoStatus);
        equipmentRepository.save(equipment);
    }

    Equipment buscarEntidadePorId(UUID id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado: " + id));
    }
}
