package com.sedmotors.controller;

import com.sedmotors.model.AuditLog;
import com.sedmotors.model.Booking;
import com.sedmotors.repository.AuditLogRepository;
import com.sedmotors.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired private AuditLogRepository auditLogRepo;
    @Autowired private BookingRepository bookingRepo;

    // ── GET latest 100 audit log entries ────────────────────────────────────────
    @GetMapping
    public List<AuditLog> getAll() {
        return auditLogRepo.findTop100ByOrderByTimestampDesc();
    }

    // ── POST create a manual log entry ──────────────────────────────────────────
    @PostMapping
    public AuditLog create(@RequestBody Map<String, String> body) {
        AuditLog log = new AuditLog(
            body.getOrDefault("actorName", "System"),
            body.getOrDefault("action", "Manual action"),
            body.getOrDefault("entityType", ""),
            body.getOrDefault("entityId", "")
        );
        return auditLogRepo.save(log);
    }

    // ── GET vehicle history (CRM search) ────────────────────────────────────────
    @GetMapping("/vehicle-history")
    public ResponseEntity<?> vehicleHistory(@RequestParam String registration) {
        if (registration == null || registration.isBlank()) {
            return ResponseEntity.badRequest().body("Registration number is required");
        }
        List<Booking> results = bookingRepo.findByVehicleRegistrationContainingIgnoreCase(registration.trim());
        return ResponseEntity.ok(results);
    }
}
