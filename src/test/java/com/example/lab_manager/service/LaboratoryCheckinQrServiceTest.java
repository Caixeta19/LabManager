package com.example.lab_manager.service;

import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaboratoryCheckinQrServiceTest {

    @Mock LaboratoryRepository laboratoryRepository;
    @Mock QrCodeService qrCodeService;

    LaboratoryCheckinQrService service;
    Laboratory lab;

    @BeforeEach
    void setUp() {
        service = new LaboratoryCheckinQrService(laboratoryRepository, qrCodeService, "http://front.local/");
        lab = TestData.lab(true);
    }

    @Test
    void laboratorioNasceComToken() {
        assertThat(lab.getCheckinToken()).hasSize(43).doesNotContain("+", "/", "=");
        assertThat(TestData.lab(true).getCheckinToken()).isNotEqualTo(lab.getCheckinToken());
    }

    @Test
    void qrLevaAUrlDoFrontComOToken() {
        when(laboratoryRepository.findById(lab.getId())).thenReturn(Optional.of(lab));
        when(qrCodeService.gerarImagemPng("http://front.local/check-in?lab=" + lab.getCheckinToken()))
                .thenReturn(new byte[]{7});

        assertThat(service.gerarImagemPng(lab.getId())).containsExactly(7);
    }

    @Test
    void regenerarTrocaOToken() {
        String antigo = lab.getCheckinToken();
        when(laboratoryRepository.findById(lab.getId())).thenReturn(Optional.of(lab));

        service.regenerarToken(lab.getId());

        assertThat(lab.getCheckinToken()).isNotBlank().isNotEqualTo(antigo);
        verify(laboratoryRepository).save(lab);
    }

    @Test
    void laboratorioInexistente404() {
        UUID id = UUID.randomUUID();
        when(laboratoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarImagemPng(id)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(qrCodeService);
    }
}
