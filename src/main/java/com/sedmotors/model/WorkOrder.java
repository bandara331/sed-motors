package com.sedmotors.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_orders")
@Data
@NoArgsConstructor
public class WorkOrder {

    public enum Status {
        RECEIVED, DIAGNOSING, IN_PROGRESS, READY, COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private String mechanicName;

    private String bayNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RECEIVED;

    @Column(precision = 10, scale = 2)
    private BigDecimal laborHours = BigDecimal.ZERO;

    @Column(length = 3000)
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
