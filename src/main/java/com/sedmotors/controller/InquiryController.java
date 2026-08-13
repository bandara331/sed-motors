package com.sedmotors.controller;

import com.sedmotors.config.EmailService;
import com.sedmotors.model.AuditLog;
import com.sedmotors.model.Inquiry;
import com.sedmotors.model.InquiryMessage;
import com.sedmotors.repository.AuditLogRepository;
import com.sedmotors.repository.InquiryMessageRepository;
import com.sedmotors.repository.InquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * InquiryController — WhatsApp-style chat between customer and admin.
 * Author: Sasmit Tejan
 */
@RestController
@RequestMapping("/api/inquiries")
@CrossOrigin("*")
public class InquiryController {

    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private InquiryMessageRepository messageRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private EmailService emailService;

    // ── GET all inquiries (Admin) ─────────────────────────────────────────────
    @GetMapping
    public List<Inquiry> getAllInquiries() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc();
    }

    // ── GET inquiries for the currently logged-in customer ────────────────────
    @GetMapping("/my")
    public ResponseEntity<?> getMyInquiries() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        String email = auth.getName();
        List<Inquiry> mine = inquiryRepository.findByEmailOrderByCreatedAtDesc(email);
        return ResponseEntity.ok(mine);
    }

    // ── GET single inquiry ────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Inquiry> getInquiry(@PathVariable Long id) {
        return inquiryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET chat messages for an inquiry ─────────────────────────────────────
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id) {
        return inquiryRepository.findById(id).map(inquiry -> {
            List<InquiryMessage> msgs = messageRepository.findByInquiryIdOrderBySentAtAsc(id);
            List<Map<String, Object>> result = new ArrayList<>();
            for (InquiryMessage m : msgs) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", m.getId());
                map.put("sender", m.getSender().name());
                map.put("message", m.getMessage());
                map.put("sentAt", m.getSentAt() != null ? m.getSentAt().toString() : "");
                result.add(map);
            }
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── POST new inquiry from public site ─────────────────────────────────────
    @PostMapping
    public ResponseEntity<Inquiry> createInquiry(@RequestBody Inquiry inquiry) {
        inquiry.setStatus(Inquiry.Status.PENDING);
        inquiry.setAdminReply(null);
        Inquiry saved = inquiryRepository.save(inquiry);

        // Save first message from customer
        InquiryMessage firstMsg = new InquiryMessage();
        firstMsg.setInquiry(saved);
        firstMsg.setSender(InquiryMessage.Sender.CUSTOMER);
        firstMsg.setMessage(saved.getMessage());
        messageRepository.save(firstMsg);

        auditLogRepository.save(new AuditLog(
            "Customer",
            "Inquiry submitted: \"" + truncate(saved.getSubject(), 80) + "\" from " + saved.getEmail(),
            "Inquiry", String.valueOf(saved.getId())
        ));

        return ResponseEntity.ok(saved);
    }

    // ── POST customer sends a follow-up chat message ──────────────────────────
    @PostMapping("/{id}/message")
    public ResponseEntity<?> sendCustomerMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String text = body.get("message");
        if (text == null || text.isBlank()) return ResponseEntity.badRequest().body("Empty message.");

        return inquiryRepository.findById(id).map(inquiry -> {
            // Reopen if previously responded
            inquiry.setStatus(Inquiry.Status.PENDING);
            inquiryRepository.save(inquiry);

            InquiryMessage msg = new InquiryMessage();
            msg.setInquiry(inquiry);
            msg.setSender(InquiryMessage.Sender.CUSTOMER);
            msg.setMessage(text.trim());
            InquiryMessage saved = messageRepository.save(msg);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", saved.getId());
            resp.put("sender", saved.getSender().name());
            resp.put("message", saved.getMessage());
            resp.put("sentAt", saved.getSentAt() != null ? saved.getSentAt().toString() : "");
            return ResponseEntity.ok(resp);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── POST admin reply (saves as chat message + optionally sends email) ─────
    @PostMapping("/{id}/reply")
    public ResponseEntity<?> replyToInquiry(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String reply = body.get("reply");
        if (reply == null || reply.isBlank()) {
            return ResponseEntity.badRequest().body("Reply message cannot be empty.");
        }

        return inquiryRepository.findById(id).map(inquiry -> {
            inquiry.setAdminReply(reply);
            inquiry.setStatus(Inquiry.Status.RESPONDED);
            inquiry.setRespondedAt(LocalDateTime.now());
            Inquiry saved = inquiryRepository.save(inquiry);

            // Save as chat message
            InquiryMessage msg = new InquiryMessage();
            msg.setInquiry(saved);
            msg.setSender(InquiryMessage.Sender.ADMIN);
            msg.setMessage(reply.trim());
            messageRepository.save(msg);
            
            // Send email to customer
            try {
                emailService.sendInquiryReply(saved);
            } catch (Exception e) {
                // Log and ignore to prevent failing the chat response
                System.err.println("Failed to send inquiry reply email: " + e.getMessage());
            }

            // Audit
            String actor = getActorName();
            auditLogRepository.save(new AuditLog(
                actor,
                "Replied to inquiry #" + id + " (chat) from " + inquiry.getEmail(),
                "Inquiry", String.valueOf(id)
            ));

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE inquiry ────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInquiry(@PathVariable Long id) {
        if (!inquiryRepository.existsById(id)) return ResponseEntity.notFound().build();
        // Cascade delete messages first
        List<InquiryMessage> msgs = messageRepository.findByInquiryIdOrderBySentAtAsc(id);
        messageRepository.deleteAll(msgs);
        inquiryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private String getActorName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName() : "Admin";
    }

    private String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) + "…" : s;
    }
}
