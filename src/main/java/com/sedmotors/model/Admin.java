package com.sedmotors.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "admins")
@Data
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    // Role is implicitly ROLE_ADMIN, but keeping it explicit can help with uniform handling
    @Enumerated(EnumType.STRING)
    private Role role = Role.ROLE_ADMIN;
}
