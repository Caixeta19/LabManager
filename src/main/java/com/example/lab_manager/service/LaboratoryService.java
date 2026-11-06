package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.LaboratoryRequestDTO;
import com.example.lab_manager.dto.response.LaboratoryResponseDTO;
import com.example.lab_manager.enums.LaboratoryStatus;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.LaboratoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;

    @Transactional
    public LaboratoryResponseDTO create(LaboratoryRequestDTO dto) {
        Laboratory lab = new Laboratory();
        lab.setName(dto.name());
        lab.setLocalization(dto.localization());
        lab.setCapacity(dto.capacity());
        lab.setDescription(dto.description());
        lab.setOpeningHours(dto.openingHours());
        lab.setResources(normalizarRecursos(dto.resources()));
        lab.applyStatus(dto.status() != null ? dto.status() : LaboratoryStatus.ATIVO);

        Laboratory saved = laboratoryRepository.save(lab);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LaboratoryResponseDTO> findAllActive() {
        return laboratoryRepository.findByActiveTrue().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LaboratoryResponseDTO findById(UUID id) {
        Laboratory lab = laboratoryRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório não encontrado."));
        return toDTO(lab);
    }

    @Transactional
    public LaboratoryResponseDTO update(UUID id, LaboratoryRequestDTO dto) {
        Laboratory lab = laboratoryRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório não encontrado."));

        lab.setName(dto.name());
        lab.setLocalization(dto.localization());
        lab.setCapacity(dto.capacity());
        lab.setDescription(dto.description());
        lab.setOpeningHours(dto.openingHours());
        lab.setResources(normalizarRecursos(dto.resources()));
        if (dto.status() != null) {
            lab.applyStatus(dto.status());
        }

        return toDTO(laboratoryRepository.save(lab));
    }

    @Transactional
    public void deactivate(UUID id) {
        Laboratory lab = laboratoryRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório não encontrado."));
        lab.setActive(false);
        laboratoryRepository.save(lab);
    }

    /** Cada recurso ocupa uma linha da coluna: quebras de linha e itens vazios ficam de fora. */
    private static List<String> normalizarRecursos(List<String> recursos) {
        if (recursos == null) {
            return List.of();
        }
        return recursos.stream()
                .filter(Objects::nonNull)
                .map(recurso -> recurso.replaceAll("\\s+", " ").trim())
                .filter(recurso -> !recurso.isEmpty())
                .toList();
    }

    private LaboratoryResponseDTO toDTO(Laboratory lab) {
        return new LaboratoryResponseDTO(
                lab.getId(),
                lab.getName(),
                lab.getLocalization(),
                lab.getCapacity(),
                lab.getDescription(),
                lab.getActive(),
                lab.status(),
                lab.getOpeningHours(),
                lab.getResources() != null ? lab.getResources() : List.of()
        );
    }
}