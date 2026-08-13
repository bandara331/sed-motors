package com.sedmotors.controller;

import com.sedmotors.config.EmailService;
import com.sedmotors.model.AuditLog;
import com.sedmotors.model.Booking;
import com.sedmotors.repository.AuditLogRepository;
import com.sedmotors.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BookingController — Manages customer bookings.
 * Triggers emails on status changes and supports custom email dispatch.
 * Author: Sasmit Tejan
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin("*")
public class BookingController {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private EmailService emailService;
    @Autowired private AuditLogRepository auditLogRepository;

    // ── GET all bookings (Admin) ──────────────────────────────────────────────
    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByIdDesc();
    }

    // ── GET my bookings (Authenticated customer) ──────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<?> getMyBookings() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        return ResponseEntity.ok(bookingRepository.findByEmailOrderByIdDesc(auth.getName()));
    }

    // ── POST create booking (Public) ──────────────────────────────────────────
    @PostMapping
    public Booking createBooking(@RequestBody Booking booking) {
        if (booking.getStatus() == null || booking.getStatus().isEmpty()) {
            booking.setStatus("PENDING");
        }
        Booking saved = bookingRepository.save(booking);

        // Real-time push to admin dashboard
        messagingTemplate.convertAndSend("/topic/admin-notifications", saved);

        // Async booking confirmation email
        emailService.sendBookingConfirmation(saved);

        // Audit
        auditLogRepository.save(new AuditLog(
            saved.getCustomerName(),
            "Submitted booking #" + saved.getId() + " for " + saved.getServiceType(),
            "Booking", String.valueOf(saved.getId())
        ));

        return saved;
    }

    // ── PUT update status (Admin) ─────────────────────────────────────────────
    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {

        return bookingRepository.findById(id).map(booking -> {
            String newStatus = statusUpdate.get("status");
            booking.setStatus(newStatus);
            Booking saved = bookingRepository.save(booking);

            // Send status email for CONFIRMED or REJECTED
            if ("CONFIRMED".equals(newStatus) || "REJECTED".equals(newStatus)) {
                emailService.sendBookingStatusUpdate(saved);
            }

            // Audit
            String actor = getActorName();
            auditLogRepository.save(new AuditLog(
                actor,
                "Updated booking #" + id + " status to " + newStatus,
                "Booking", String.valueOf(id)
            ));

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── POST send custom email to booking customer (Admin) ────────────────────
    @PostMapping("/{id}/send-email")
    public ResponseEntity<?> sendCustomEmail(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String subject = body.get("subject");
        String message = body.get("message");
        if (subject == null || subject.isBlank() || message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body("Subject and message are required.");
        }

        return bookingRepository.findById(id).map(booking -> {
            if (booking.getEmail() == null || booking.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body("No email address on file for this booking.");
            }

            emailService.sendCustomEmail(booking.getEmail(), booking.getCustomerName(), subject, message);

            // Audit
            String actor = getActorName();
            auditLogRepository.save(new AuditLog(
                actor,
                "Sent custom email to " + booking.getEmail() + " (Booking #" + id + "): " + subject,
                "Booking", String.valueOf(id)
            ));

            return ResponseEntity.ok(Map.of("status", "sent", "to", booking.getEmail()));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private String getActorName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName() : "Admin";
    }
}
