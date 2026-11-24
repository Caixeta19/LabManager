package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.LaboratoryRequestDTO;
import com.example.lab_manager.dto.response.LaboratoryResponseDTO;
import com.example.lab_manager.enums.LaboratoryStatus;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.LaboratoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.EnumSet;
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
        aplicarJanelaDeReserva(lab, dto);
        lab.applyStatus(dto.status() != null ? dto.status() : LaboratoryStatus.ATIVO);

        Laboratory saved = laboratoryRepository.save(lab);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LaboratoryResponseDTO> findAllActive() {
        return laboratoryRepository.findByActiveTrue().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LaboratoryResponseDTO> findAll() {
        return laboratoryRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
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
        aplicarJanelaDeReserva(lab, dto);
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

    /** Janela de reserva: as duas horas ou nenhuma, e a abertura antes do fechamento. */
    private static void aplicarJanelaDeReserva(Laboratory lab, LaboratoryRequestDTO dto) {
        boolean temAbertura = dto.bookingOpenTime() != null;
        boolean temFechamento = dto.bookingCloseTime() != null;
        if (temAbertura != temFechamento) {
            throw new RegraDeNegocioException(
                    "Informe o horário de abertura e o de fechamento das reservas, ou nenhum dos dois.", "ERR_LAB_003");
        }
        if (temAbertura && !dto.bookingOpenTime().isBefore(dto.bookingCloseTime())) {
            throw new RegraDeNegocioException(
                    "O horário de abertura das reservas deve ser antes do horário de fechamento.", "ERR_LAB_003");
        }
        lab.setBookingOpenTime(dto.bookingOpenTime());
        lab.setBookingCloseTime(dto.bookingCloseTime());

        EnumSet<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        if (dto.bookingDays() != null) {
            dto.bookingDays().stream().filter(Objects::nonNull).forEach(dias::add);
        }
        lab.setBookingDays(dias);
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
                lab.getResources() != null ? lab.getResources() : List.of(),
                lab.getBookingOpenTime(),
                lab.getBookingCloseTime(),
                lab.getBookingDays() == null ? List.of() : lab.getBookingDays().stream().sorted().toList()
        );
    }
}