package com.sedmotors.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(nullable = false)
    private String vehicleDetails;

    // Vehicle registration / plate number — used by CRM module for history search
    private String vehicleRegistration;

    @Column(nullable = false)
    private String serviceType;

    private String preferredDate;

    private String preferredTime;

    @Column(length = 2000)
    private String message;

    @Column(nullable = false)
    private String status = "PENDING";
}
