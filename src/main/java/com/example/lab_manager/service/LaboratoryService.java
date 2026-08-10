package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.LaboratoryRequestDTO;
import com.example.lab_manager.dto.response.LaboratoryResponseDTO;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.LaboratoryRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;

    @Transactional
    public LaboratoryResponseDTO criar(LaboratoryRequestDTO dto) {
        Laboratory lab = Laboratory.builder()
                .name(dto.name())
                .locatization(dto.localization())
                .capacity(dto.capacity())
                .description(dto.description())
                .active(true)
                .build();

        return LaboratoryResponseDTO.fromEntity(laboratoryRepository.save(lab));
    }

    @Transactional(readOnly = true)
    public LaboratoryResponseDTO buscarPorId(UUID id) {
        return LaboratoryResponseDTO.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public List<LaboratoryResponseDTO> listarAtivos() {
        return laboratoryRepository.findAByActiveTrue().stream()
                .map(LaboratoryResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public LaboratoryResponseDTO atualizar(UUID id, LaboratoryRequestDTO dto) {
        Laboratory lab = buscarEntidadePorId(id);
        lab.setName(dto.name());
        lab.setLocatization(dto.localization());
        lab.setCapacity(dto.capacity());
        lab.setDescription(dto.description());
        return LaboratoryResponseDTO.fromEntity(laboratoryRepository.save(lab));
    }

    @Transactional
    public void desativar(UUID id) {
        Laboratory lab = buscarEntidadePorId(id);
        lab.setActive(false);
        laboratoryRepository.save(lab);
    }

    Laboratory buscarEntidadePorId(UUID id) {
        return laboratoryRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório não encontrado: " + id));
    }
}

