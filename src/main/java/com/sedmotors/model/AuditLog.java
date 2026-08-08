package com.sedmotors.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actorName;

    @Column(nullable = false, length = 1000)
    private String action;

    private String entityType;

    private String entityId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    public AuditLog(String actorName, String action, String entityType, String entityId) {
        this.actorName  = actorName;
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
    }
}
