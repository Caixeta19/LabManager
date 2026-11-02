package com.example.lab_manager.controller;

import com.example.lab_manager.dto.response.CheckInResponseDTO;
import com.example.lab_manager.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Realiza check-in (só o dono da reserva, CONFIRMADA e dentro da janela de horário)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Check-in registrado"),
            @ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado"),
            @ApiResponse(responseCode = "404", description = "ERR_CHECKIN_002 - QR Code inválido ou fora do horário permitido"),
            @ApiResponse(responseCode = "409", description = "ERR_CHECKIN_003 - check-in já realizado"),
            @ApiResponse(responseCode = "422", description = "ERR_CHECKIN_001 - reserva ainda não confirmada")
    })
    public ResponseEntity<CheckInResponseDTO> doCheckIn(@PathVariable String codigoQr) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkInService.performCheckIn(codigoQr));
    }
}
