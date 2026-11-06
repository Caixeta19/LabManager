package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.LaboratoryRequestDTO;
import com.example.lab_manager.dto.response.LaboratoryResponseDTO;
import com.example.lab_manager.enums.LaboratoryStatus;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LaboratoryServiceTest {

    @Mock LaboratoryRepository laboratoryRepository;
    @InjectMocks LaboratoryService service;

    Laboratory lab;

    @BeforeEach
    void setUp() {
        lab = TestData.lab(true);
        lenient().when(laboratoryRepository.findById(lab.getId())).thenReturn(Optional.of(lab));
        lenient().when(laboratoryRepository.save(any(Laboratory.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private LaboratoryRequestDTO request(LaboratoryStatus status, List<String> recursos) {
        return new LaboratoryRequestDTO("Lab", "Bloco A", 20, null, "Seg–Sex: 08h–20h", recursos, status);
    }

    @Test
    void manutencaoMantemOLaboratorioAtivo() {
        LaboratoryResponseDTO dto = service.update(lab.getId(), request(LaboratoryStatus.MANUTENCAO, null));

        assertThat(dto.status()).isEqualTo(LaboratoryStatus.MANUTENCAO);
        assertThat(dto.active()).isTrue();
        assertThat(lab.isMaintenance()).isTrue();
    }

    @Test
    void inativarTiraDaManutencao() {
        lab.setMaintenance(true);

        LaboratoryResponseDTO dto = service.update(lab.getId(), request(LaboratoryStatus.INATIVO, null));

        assertThat(dto.status()).isEqualTo(LaboratoryStatus.INATIVO);
        assertThat(dto.active()).isFalse();
        assertThat(lab.isMaintenance()).isFalse();
    }

    @Test
    void reativaUmLaboratorioInativo() {
        lab.setActive(false);

        assertThat(service.update(lab.getId(), request(LaboratoryStatus.ATIVO, null)).status())
                .isEqualTo(LaboratoryStatus.ATIVO);
    }

    @Test
    void semStatusMantemOAtual() {
        lab.setMaintenance(true);

        assertThat(service.update(lab.getId(), request(null, null)).status())
                .isEqualTo(LaboratoryStatus.MANUTENCAO);
    }

    @Test
    void cadastroSemStatusNasceAtivoEAceitaManutencao() {
        assertThat(service.create(request(null, null)).status()).isEqualTo(LaboratoryStatus.ATIVO);
        assertThat(service.create(request(LaboratoryStatus.MANUTENCAO, null)).status())
                .isEqualTo(LaboratoryStatus.MANUTENCAO);
    }

    @Test
    void gravaHorarioERecursosSemItensVaziosNemQuebrasDeLinha() {
        LaboratoryResponseDTO dto = service.update(lab.getId(),
                request(null, Arrays.asList(" Projetor ", "", null, "Ar\ncondicionado")));

        assertThat(dto.openingHours()).isEqualTo("Seg–Sex: 08h–20h");
        assertThat(dto.resources()).containsExactly("Projetor", "Ar condicionado");
    }
}
