package com.sedmotors.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * InquiryMessage — A single message in a chat thread tied to an Inquiry.
 * Author: Sasmit Tejan
 */
@Entity
@Table(name = "inquiry_messages")
@Data
@NoArgsConstructor
public class InquiryMessage {

    public enum Sender { CUSTOMER, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sender sender;

    @Column(nullable = false, length = 5000)
    private String message;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime sentAt;
}
