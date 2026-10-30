package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.EquipmentRequestDTO;
import com.example.lab_manager.dto.response.EquipmentResponseDTO;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.model.Equipment;
import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.EquipmentRepository;
import com.example.lab_manager.repository.LaboratoryRepository;
import lombok.RequiredArgsConstructor;
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
        Laboratory lab = (Laboratory) laboratoryRepository.findById(dto.laboratoryId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório vinculado não encontrado."));

        Equipment equipment = new Equipment();
        equipment.setName(dto.name());
        equipment.setHeritage(dto.heritage());
        equipment.setDescription(dto.description());
        equipment.setLaboratory(lab);
        equipment.setStatus(EquipmentStatus.DISPONIVEL);

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

        equipment.setName(dto.name());
        equipment.setHeritage(dto.heritage());
        equipment.setDescription(dto.description());

        return toDTO(equipmentRepository.save(equipment));
    }

    private EquipmentResponseDTO toDTO(Equipment eq) {
        return new EquipmentResponseDTO(
                eq.getId(),
                eq.getName(),
                eq.getHeritage(),
                eq.getDescription(),
                eq.getLaboratory().getId(),
                eq.getLaboratory().getName(),
                eq.getStatus()
        );
    }
}