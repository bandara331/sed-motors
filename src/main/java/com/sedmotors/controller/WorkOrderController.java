package com.sedmotors.controller;

import com.sedmotors.model.*;
import com.sedmotors.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    @Autowired private WorkOrderRepository workOrderRepo;
    @Autowired private WorkOrderPartRepository workOrderPartRepo;
    @Autowired private BookingRepository bookingRepo;
    @Autowired private PartRepository partRepo;
    @Autowired private AuditLogRepository auditLogRepo;

    // ── GET all ────────────────────────────────────────────────────────────────
    @GetMapping
    public List<WorkOrder> getAll() {
        return workOrderRepo.findAllByOrderByIdDesc();
    }

    // ── GET parts for a work order ─────────────────────────────────────────────
    @GetMapping("/{id}/parts")
    public List<WorkOrderPart> getParts(@PathVariable Long id) {
        return workOrderPartRepo.findByWorkOrderId(id);
    }

    // ── POST create ─────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long bookingId    = Long.parseLong(body.get("bookingId").toString());
        String mechanic   = (String) body.getOrDefault("mechanicName", "Unassigned");
        String bay        = (String) body.getOrDefault("bayNumber", "");

        Booking booking = bookingRepo.findById(bookingId).orElse(null);
        if (booking == null) return ResponseEntity.badRequest().body("Booking not found");

        WorkOrder wo = new WorkOrder();
        wo.setBooking(booking);
        wo.setMechanicName(mechanic);
        wo.setBayNumber(bay);
        wo.setStatus(WorkOrder.Status.RECEIVED);
        WorkOrder saved = workOrderRepo.save(wo);

        logAction("Created Work Order #" + saved.getId() + " for Booking #" + bookingId, "WorkOrder", saved.getId().toString());
        return ResponseEntity.ok(saved);
    }

    // ── PUT update status ───────────────────────────────────────────────────────
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return workOrderRepo.findById(id).map(wo -> {
            WorkOrder.Status newStatus = WorkOrder.Status.valueOf(body.get("status"));
            wo.setStatus(newStatus);
            if (body.containsKey("mechanicName")) wo.setMechanicName(body.get("mechanicName"));
            if (body.containsKey("bayNumber"))    wo.setBayNumber(body.get("bayNumber"));
            if (body.containsKey("laborHours"))   wo.setLaborHours(new BigDecimal(body.get("laborHours")));
            if (body.containsKey("notes"))        wo.setNotes(body.get("notes"));
            WorkOrder updated = workOrderRepo.save(wo);
            logAction("Updated Work Order #" + id + " status → " + newStatus, "WorkOrder", id.toString());
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── POST add part to work order (deducts stock) ─────────────────────────────
    @PostMapping("/{id}/parts")
    public ResponseEntity<?> addPart(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        WorkOrder wo = workOrderRepo.findById(id).orElse(null);
        if (wo == null) return ResponseEntity.badRequest().body("Work Order not found");

        Long   partId = Long.parseLong(body.get("partId").toString());
        int    qty    = Integer.parseInt(body.get("quantity").toString());

        Part part = partRepo.findById(partId).orElse(null);
        if (part == null) return ResponseEntity.badRequest().body("Part not found");
        if (part.getStockQuantity() < qty) {
            return ResponseEntity.badRequest().body("Insufficient stock. Available: " + part.getStockQuantity());
        }

        // Deduct stock
        part.setStockQuantity(part.getStockQuantity() - qty);
        partRepo.save(part);

        // Create line item
        WorkOrderPart wop = new WorkOrderPart();
        wop.setWorkOrder(wo);
        wop.setPart(part);
        wop.setQuantityUsed(qty);
        wop.setUnitPriceAtTime(part.getPrice());
        WorkOrderPart saved = workOrderPartRepo.save(wop);

        logAction("Added " + qty + "x " + part.getName() + " to Work Order #" + id, "WorkOrderPart", saved.getId().toString());
        return ResponseEntity.ok(saved);
    }

    // ── DELETE remove a part line (restores stock) ──────────────────────────────
    @DeleteMapping("/{woId}/parts/{wopId}")
    public ResponseEntity<?> removePart(@PathVariable Long woId, @PathVariable Long wopId) {
        return workOrderPartRepo.findById(wopId).map(wop -> {
            // Restore stock
            Part part = wop.getPart();
            part.setStockQuantity(part.getStockQuantity() + wop.getQuantityUsed());
            partRepo.save(part);
            workOrderPartRepo.delete(wop);
            logAction("Removed part " + part.getName() + " from Work Order #" + woId, "WorkOrderPart", wopId.toString());
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Helper: get current admin name ─────────────────────────────────────────
    private String getActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "System";
    }

    private void logAction(String action, String entityType, String entityId) {
        try { auditLogRepo.save(new AuditLog(getActor(), action, entityType, entityId)); } catch (Exception ignored) {}
    }
}
