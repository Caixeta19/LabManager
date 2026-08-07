package com.example.lab_manager.model;
import com.example.lab_manager.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name= "User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class User {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Column(name = "registration", length = 100, nullable = false)
    private String registration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy= "user")
    private List<Reserve> reserves;

    @OneToMany( mappedBy = "user")
    private List<Loan> loans;

}
