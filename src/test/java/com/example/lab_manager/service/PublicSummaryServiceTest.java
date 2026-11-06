package com.example.lab_manager.service;

import com.example.lab_manager.dto.response.PublicSummaryDTO;
import com.example.lab_manager.enums.PublicLaboratoryStatus;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.EquipmentRepository;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.repository.ReserveRepository;
import com.example.lab_manager.repository.UserRepository;
import com.example.lab_manager.support.TestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicSummaryServiceTest {

    @Mock LaboratoryRepository laboratoryRepository;
    @Mock ReserveRepository reserveRepository;
    @Mock EquipmentRepository equipmentRepository;
    @Mock UserRepository userRepository;

    PublicSummaryService service;
    Laboratory livre;
    Laboratory emUso;
    Laboratory manutencao;

    @BeforeEach
    void setUp() {
        service = new PublicSummaryService(laboratoryRepository, reserveRepository, equipmentRepository,
                userRepository, TestData.CLOCK);

        livre = TestData.lab(true);
        livre.setName("C - Livre");
        emUso = TestData.lab(true);
        emUso.setName("A - Em uso");
        manutencao = TestData.lab(true);
        manutencao.setName("B - Manutenção");
        manutencao.setMaintenance(true);

        LocalDate hoje = AGORA.toLocalDate();
        when(laboratoryRepository.findByActiveTrue()).thenReturn(List.of(livre, emUso, manutencao));
        when(laboratoryRepository.countByActiveTrueAndMaintenanceFalse()).thenReturn(2L);
        when(reserveRepository.findLaboratoriosEmUso(ReserveStatus.CONFIRMADA, AGORA))
                .thenReturn(Set.of(emUso.getId(), manutencao.getId()));
        when(reserveRepository.countComInicioEntre(hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay(),
                ReserveRepository.STATUS_IGNORADOS_NO_CONFLITO)).thenReturn(7L);
        when(equipmentRepository.count()).thenReturn(12L);
        when(userRepository.countByActiveTrue()).thenReturn(30L);
    }

    @Test
    void devolveOsTotais() {
        PublicSummaryDTO resumo = service.resumo();

        assertThat(resumo.laboratoriosAtivos()).isEqualTo(2);
        assertThat(resumo.reservasHoje()).isEqualTo(7);
        assertThat(resumo.equipamentos()).isEqualTo(12);
        assertThat(resumo.usuarios()).isEqualTo(30);
    }

    @Test
    void calculaOStatusDeCadaLaboratorioEManutencaoTemPrioridade() {
        assertThat(service.resumo().laboratorios()).containsExactly(
                new PublicSummaryDTO.LaboratorioDTO("A - Em uso", PublicLaboratoryStatus.EM_USO_AGORA),
                new PublicSummaryDTO.LaboratorioDTO("B - Manutenção", PublicLaboratoryStatus.MANUTENCAO),
                new PublicSummaryDTO.LaboratorioDTO("C - Livre", PublicLaboratoryStatus.DISPONIVEL));
    }

    @Test
    void naoExpoeDadosPessoais() throws Exception {
        String json = new ObjectMapper().writeValueAsString(service.resumo());

        assertThat(json).doesNotContainIgnoringCase("email").doesNotContainIgnoringCase("userName");
        assertThat(Arrays.stream(PublicSummaryDTO.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("laboratoriosAtivos", "reservasHoje", "equipamentos", "usuarios", "laboratorios");
    }
}
