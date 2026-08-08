package com.sedmotors.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
@Data
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String contactPerson;

    private String phone;

    private String email;

    @Column(length = 1000)
    private String address;

    @Column(length = 2000)
    private String partsSupplied;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
