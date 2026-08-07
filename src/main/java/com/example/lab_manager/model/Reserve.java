package com.example.lab_manager.model;
import com.example.lab_manager.enums.ReserveStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reserves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Reserve {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn( name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @Column(nullable = false)
    private LocalDateTime dataHorainicio;

    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ReserveStatus status = ReserveStatus.PENDENTE;

    // Token único para o link de confirmação por e-mail (1h antes do horário)

    @Builder.Default
    @Column(nullable = false, unique = true)
    private String ConfirmationToken = UUID.randomUUID().toString();

    // Código dedicado para o QR Code de check-in (separado do token de e-mail por segurança)

    @Builder.Default
    @Column(nullable = false)
    private String QRCode = UUID.randomUUID().toString();

    @Builder.Default
    @Column(nullable = false)
    private boolean emailConfirmacaoEnviado = false;

    private LocalDateTime dataConfirmacao;

    @OneToOne(mappedBy = "reserve")
    private CheckIn checkIn;






}
