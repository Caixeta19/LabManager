package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.EquipmentRequestDTO;
import com.example.lab_manager.dto.response.EquipmentResponseDTO;
import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.Equipment;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.EquipmentRepository;
import com.example.lab_manager.repository.LaboratoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final LaboratoryRepository laboratoryRepository;

    @Transactional
    public EquipmentResponseDTO create(EquipmentRequestDTO dto) {
        Laboratory lab = buscarLaboratorioAtivo(dto.laboratoryId());
        String heritage = dto.heritage().trim();

        if (equipmentRepository.existsByHeritage(heritage)) {
            throw patrimonioDuplicado();
        }

        Equipment equipment = new Equipment();
        equipment.setName(dto.name());
        equipment.setHeritage(heritage);
        equipment.setDescription(dto.description());
        equipment.setResponsible(dto.responsible());
        equipment.setLaboratory(lab);
        equipment.setStatus(EquipmentStatus.DISPONIVEL); // RN06

        return toDTO(equipmentRepository.save(equipment));
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponseDTO> findAll() {
        return equipmentRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponseDTO> findAllAvailable() {
        return equipmentRepository.findByStatus(EquipmentStatus.DISPONIVEL).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public EquipmentResponseDTO update(UUID id, EquipmentRequestDTO dto) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado."));
        String heritage = dto.heritage().trim();

        if (equipmentRepository.existsByHeritageAndIdNot(heritage, id)) {
            throw patrimonioDuplicado();
        }

        if (!equipment.getLaboratory().getId().equals(dto.laboratoryId())) {
            equipment.setLaboratory(buscarLaboratorioAtivo(dto.laboratoryId()));
        }

        if (dto.status() != null && dto.status() != equipment.getStatus()) {
            // EM_USO só entra e sai pelo fluxo de empréstimo (RN08/RN09)
            if (dto.status() == EquipmentStatus.EM_USO || equipment.getStatus() == EquipmentStatus.EM_USO) {
                throw new RegraDeNegocioException(
                        "O status EM_USO é controlado pelos empréstimos e não pode ser alterado manualmente.",
                        "ERR_EQUIP_003");
            }
            equipment.setStatus(dto.status());
        }

        equipment.setName(dto.name());
        equipment.setHeritage(heritage);
        equipment.setDescription(dto.description());
        equipment.setResponsible(dto.responsible());

        return toDTO(equipmentRepository.save(equipment));
    }

    /** Decisão 11: não se cadastra equipamento em laboratório inativo. */
    private Laboratory buscarLaboratorioAtivo(UUID laboratoryId) {
        Laboratory lab = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório vinculado não encontrado."));
        if (!Boolean.TRUE.equals(lab.getActive())) {
            throw new RegraDeNegocioException("Laboratório inativo.", "ERR_LAB_001");
        }
        return lab;
    }

    private RegraDeNegocioException patrimonioDuplicado() {
        return new RegraDeNegocioException("Já existe um equipamento com este número de patrimônio.",
                "ERR_EQUIP_002", HttpStatus.CONFLICT);
    }

    private EquipmentResponseDTO toDTO(Equipment eq) {
        return new EquipmentResponseDTO(
                eq.getId(),
                eq.getName(),
                eq.getHeritage(),
                eq.getDescription(),
                eq.getLaboratory().getId(),
                eq.getLaboratory().getName(),
                eq.getStatus(),
                eq.getResponsible()
        );
    }
}
