package com.example.lab_manager.controller;

import com.example.lab_manager.dto.response.CheckInResponseDTO;
import com.example.lab_manager.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
@Tag(name = "Check-In", description = "Validação e registro de presença via QR Code")
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/{codigoQr}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Realiza check-in validando se a reserva está CONFIRMADA e dentro da janela de horário")
    public ResponseEntity<CheckInResponseDTO> doCheckIn(@PathVariable String codigoQr) {
        CheckInResponseDTO response = checkInService.performCheckIn(codigoQr);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}