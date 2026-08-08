package com.sedmotors.controller;

import com.sedmotors.model.AuditLog;
import com.sedmotors.model.Supplier;
import com.sedmotors.repository.AuditLogRepository;
import com.sedmotors.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired private SupplierRepository supplierRepo;
    @Autowired private AuditLogRepository auditLogRepo;

    @GetMapping
    public List<Supplier> getAll() {
        return supplierRepo.findAllByOrderByNameAsc();
    }

    @PostMapping
    public ResponseEntity<Supplier> create(@RequestBody Supplier supplier) {
        Supplier saved = supplierRepo.save(supplier);
        logAction("Added Supplier: " + saved.getName(), "Supplier", saved.getId().toString());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Supplier details) {
        return supplierRepo.findById(id).map(s -> {
            s.setName(details.getName());
            s.setContactPerson(details.getContactPerson());
            s.setPhone(details.getPhone());
            s.setEmail(details.getEmail());
            s.setAddress(details.getAddress());
            s.setPartsSupplied(details.getPartsSupplied());
            Supplier updated = supplierRepo.save(s);
            logAction("Updated Supplier: " + updated.getName(), "Supplier", id.toString());
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return supplierRepo.findById(id).map(s -> {
            supplierRepo.delete(s);
            logAction("Deleted Supplier: " + s.getName(), "Supplier", id.toString());
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private String getActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "System";
    }
    private void logAction(String action, String entityType, String entityId) {
        try { auditLogRepo.save(new AuditLog(getActor(), action, entityType, entityId)); } catch (Exception ignored) {}
    }
}
