package com.sedmotors.controller;

import com.sedmotors.model.*;
import com.sedmotors.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10% VAT
    private static final BigDecimal LABOR_RATE_PER_HOUR = new BigDecimal("50.00"); // $50/hr default

    @Autowired private InvoiceRepository invoiceRepo;
    @Autowired private WorkOrderRepository workOrderRepo;
    @Autowired private WorkOrderPartRepository workOrderPartRepo;
    @Autowired private AuditLogRepository auditLogRepo;

    // ── GET all ────────────────────────────────────────────────────────────────
    @GetMapping
    public List<Invoice> getAll() {
        return invoiceRepo.findAllByOrderByIdDesc();
    }

    // ── POST auto-generate from work order ─────────────────────────────────────
    @PostMapping("/work-order/{woId}")
    public ResponseEntity<?> generate(@PathVariable Long woId, @RequestBody(required = false) Map<String, Object> body) {
        // Prevent duplicate invoice
        if (invoiceRepo.findByWorkOrderId(woId).isPresent()) {
            return ResponseEntity.badRequest().body("Invoice already exists for Work Order #" + woId);
        }

        WorkOrder wo = workOrderRepo.findById(woId).orElse(null);
        if (wo == null) return ResponseEntity.badRequest().body("Work Order not found");

        // Calculate parts total from used parts
        List<WorkOrderPart> usedParts = workOrderPartRepo.findByWorkOrderId(woId);
        BigDecimal partsTotal = usedParts.stream()
            .map(p -> p.getUnitPriceAtTime().multiply(BigDecimal.valueOf(p.getQuantityUsed())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Labor = hours × rate
        BigDecimal hours = wo.getLaborHours() != null ? wo.getLaborHours() : BigDecimal.ZERO;
        BigDecimal laborTotal = hours.multiply(LABOR_RATE_PER_HOUR).setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotal = partsTotal.add(laborTotal);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = new Invoice();
        invoice.setWorkOrder(wo);
        invoice.setPartsTotal(partsTotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setLaborTotal(laborTotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setGrandTotal(grandTotal);
        if (body != null && body.containsKey("notes")) invoice.setNotes((String) body.get("notes"));

        Invoice saved = invoiceRepo.save(invoice);
        logAction("Generated Invoice #" + saved.getId() + " for Work Order #" + woId + " — Total: $" + grandTotal, "Invoice", saved.getId().toString());
        return ResponseEntity.ok(saved);
    }

    // ── PUT update payment ─────────────────────────────────────────────────────
    @PutMapping("/{id}/payment")
    public ResponseEntity<?> updatePayment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return invoiceRepo.findById(id).map(inv -> {
            if (body.containsKey("paymentStatus")) inv.setPaymentStatus(Invoice.PaymentStatus.valueOf(body.get("paymentStatus")));
            if (body.containsKey("paymentMethod")) inv.setPaymentMethod(Invoice.PaymentMethod.valueOf(body.get("paymentMethod")));
            Invoice updated = invoiceRepo.save(inv);
            logAction("Invoice #" + id + " marked " + updated.getPaymentStatus() + " via " + updated.getPaymentMethod(), "Invoice", id.toString());
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── GET printable HTML invoice ─────────────────────────────────────────────
    @GetMapping("/{id}/print")
    public ResponseEntity<String> printInvoice(@PathVariable Long id) {
        Invoice inv = invoiceRepo.findById(id).orElse(null);
        if (inv == null) return ResponseEntity.notFound().build();

        WorkOrder wo = inv.getWorkOrder();
        Booking b    = wo.getBooking();
        List<WorkOrderPart> parts = workOrderPartRepo.findByWorkOrderId(wo.getId());

        StringBuilder rows = new StringBuilder();
        for (WorkOrderPart p : parts) {
            BigDecimal lineTotal = p.getUnitPriceAtTime().multiply(BigDecimal.valueOf(p.getQuantityUsed()));
            rows.append(String.format("""
                <tr>
                  <td>%s</td><td style="text-align:center">%d</td>
                  <td style="text-align:right">$%.2f</td>
                  <td style="text-align:right">$%.2f</td>
                </tr>""",
                p.getPart().getName(), p.getQuantityUsed(),
                p.getUnitPriceAtTime(), lineTotal));
        }

        String issuedAt = inv.getIssuedAt() != null
            ? inv.getIssuedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) : "—";

        String html = String.format("""
        <!DOCTYPE html><html lang="en"><head><meta charset="UTF-8">
        <title>Invoice #%d — SED Motors</title>
        <style>
          body{font-family:Arial,sans-serif;margin:40px;color:#1e293b}
          h1{color:#0f172a}.accent{color:#f59e0b}
          table{width:100%%;border-collapse:collapse;margin:24px 0}
          th{background:#0f172a;color:#fff;padding:10px 14px;text-align:left}
          td{padding:9px 14px;border-bottom:1px solid #e2e8f0}
          .totals td{border:none;font-size:15px}
          .grand{font-weight:800;font-size:18px;color:#f59e0b}
          .badge{display:inline-block;padding:3px 12px;border-radius:20px;font-weight:700;
                 background:%s;color:%s}
          @media print{.no-print{display:none}}
        </style></head><body>
        <div class="no-print" style="margin-bottom:20px">
          <button onclick="window.print()" style="padding:10px 24px;background:#f59e0b;border:none;border-radius:6px;font-weight:700;cursor:pointer">🖨 Print / Save as PDF</button>
        </div>
        <table style="border:none;margin:0"><tr>
          <td><h1>SED <span class="accent">Motors</span></h1><p>Garage &amp; Auto Services</p></td>
          <td style="text-align:right"><h2>INVOICE <span class="accent">#%d</span></h2>
            <p>Issued: %s</p>
            <span class="badge">%s</span>
          </td>
        </tr></table><hr>
        <table style="border:none;margin:16px 0"><tr>
          <td><strong>Customer:</strong> %s<br><strong>Phone:</strong> %s<br><strong>Email:</strong> %s</td>
          <td><strong>Vehicle:</strong> %s<br><strong>Service:</strong> %s<br><strong>Work Order:</strong> #%d</td>
          <td><strong>Mechanic:</strong> %s<br><strong>Bay:</strong> %s<br><strong>Payment:</strong> %s</td>
        </tr></table>
        <table>
          <thead><tr><th>Part / Service</th><th style="text-align:center">Qty</th><th style="text-align:right">Unit Price</th><th style="text-align:right">Total</th></tr></thead>
          <tbody>%s
          <tr><td colspan="3">Labor (%s hrs @ $%.2f/hr)</td><td style="text-align:right">$%.2f</td></tr>
          </tbody>
        </table>
        <table class="totals" style="width:350px;margin-left:auto">
          <tr><td>Parts Subtotal</td><td style="text-align:right">$%.2f</td></tr>
          <tr><td>Labor Subtotal</td><td style="text-align:right">$%.2f</td></tr>
          <tr><td>Tax (10%% VAT)</td><td style="text-align:right">$%.2f</td></tr>
          <tr class="grand"><td>GRAND TOTAL</td><td style="text-align:right">$%.2f</td></tr>
        </table>
        %s
        </body></html>""",
            inv.getId(),
            inv.getPaymentStatus() == Invoice.PaymentStatus.PAID ? "#dcfce7" : inv.getPaymentStatus() == Invoice.PaymentStatus.PARTIAL ? "#fef9c3" : "#fee2e2",
            inv.getPaymentStatus() == Invoice.PaymentStatus.PAID ? "#166534" : inv.getPaymentStatus() == Invoice.PaymentStatus.PARTIAL ? "#854d0e" : "#991b1b",
            inv.getId(), issuedAt, inv.getPaymentStatus().name(),
            b.getCustomerName(), b.getPhone(), orDash(b.getEmail()),
            b.getVehicleDetails(), b.getServiceType(), wo.getId(),
            wo.getMechanicName(), orDash(wo.getBayNumber()), inv.getPaymentMethod().name(),
            rows,
            wo.getLaborHours(), LABOR_RATE_PER_HOUR, inv.getLaborTotal(),
            inv.getPartsTotal(), inv.getLaborTotal(), inv.getTaxAmount(), inv.getGrandTotal(),
            inv.getNotes() != null ? "<p style='margin-top:24px;padding:16px;background:#f8fafc;border-radius:8px'><strong>Notes:</strong> " + inv.getNotes() + "</p>" : ""
        );

        return ResponseEntity.ok().header("Content-Type","text/html;charset=UTF-8").body(html);
    }

    private String orDash(String s) { return (s != null && !s.isBlank()) ? s : "—"; }

    private String getActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "System";
    }
    private void logAction(String action, String entityType, String entityId) {
        try { auditLogRepo.save(new AuditLog(getActor(), action, entityType, entityId)); } catch (Exception ignored) {}
    }
}
