package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.LaboratoryRequestDTO;
import com.example.lab_manager.dto.response.LaboratoryResponseDTO;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.LaboratoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        lab.setLocatization(dto.localization());
        lab.setCapacity(dto.capacity());
        lab.setDescription(dto.description());
        lab.setActive(true);

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
        lab.setLocatization(dto.localization());
        lab.setCapacity(dto.capacity());
        lab.setDescription(dto.description());

        return toDTO(laboratoryRepository.save(lab));
    }

    @Transactional
    public void deactivate(UUID id) {
        Laboratory lab = laboratoryRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório não encontrado."));
        lab.setActive(false);
        laboratoryRepository.save(lab);
    }

    private LaboratoryResponseDTO toDTO(Laboratory lab) {
        return new LaboratoryResponseDTO(
                lab.getId(),
                lab.getName(),
                lab.getLocatization(),
                lab.getCapacity(),
                lab.getDescription(),
                lab.getActive()
        );
    }
}