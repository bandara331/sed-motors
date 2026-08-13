package com.sedmotors.config;

import com.sedmotors.model.Booking;
import com.sedmotors.model.Inquiry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * EmailService — Sends all transactional HTML emails for SED Motors.
 * Covers: booking confirmations, booking status updates, inquiry replies,
 * and custom admin-composed emails.
 * Author: Sasmit Tejan
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sends a booking received confirmation email (on new booking submission).
     */
    @Async
    public void sendBookingConfirmation(Booking booking) {
        if (isBlank(booking.getEmail())) return;
        String subject = "✅ Booking Received — SED Motors #" + booking.getId();
        String body = buildBookingConfirmationHtml(booking);
        sendHtml(booking.getEmail(), booking.getCustomerName(), subject, body);
    }

    /**
     * Sends a booking status update email when admin confirms or rejects a booking.
     */
    @Async
    public void sendBookingStatusUpdate(Booking booking) {
        if (isBlank(booking.getEmail())) return;
        boolean confirmed = "CONFIRMED".equals(booking.getStatus());
        String emoji  = confirmed ? "🎉" : "❌";
        String status = confirmed ? "Confirmed" : "Rejected";
        String subject = emoji + " Booking " + status + " — SED Motors #" + booking.getId();
        String body = buildStatusUpdateHtml(booking, confirmed);
        sendHtml(booking.getEmail(), booking.getCustomerName(), subject, body);
    }

    /**
     * Sends a reply to a customer inquiry from the admin dashboard.
     */
    @Async
    public void sendInquiryReply(Inquiry inquiry) {
        if (isBlank(inquiry.getEmail())) return;
        String subject = "📩 Reply from SED Motors — " + inquiry.getSubject();
        String body = buildInquiryReplyHtml(inquiry);
        sendHtml(inquiry.getEmail(), inquiry.getCustomerName(), subject, body);
    }

    /**
     * Sends a custom admin-composed email to any recipient.
     */
    @Async
    public void sendCustomEmail(String toEmail, String toName, String subject, String messageBody) {
        if (isBlank(toEmail)) return;
        String body = buildCustomEmailHtml(toName, subject, messageBody);
        sendHtml(toEmail, toName, subject, body);
    }

    // ── Core Send ─────────────────────────────────────────────────────────────

    private void sendHtml(String toEmail, String toName, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "SED Motors");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("[EmailService] ✉ Email sent to " + toEmail + " | Subject: " + subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            System.err.println("[EmailService] ✗ Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }

    // ── HTML Builders ─────────────────────────────────────────────────────────

    private String buildBookingConfirmationHtml(Booking booking) {
        String rows = buildRow("Customer", booking.getCustomerName())
                + buildRow("Phone", booking.getPhone())
                + buildRow("Vehicle", booking.getVehicleDetails())
                + buildRow("Service", booking.getServiceType())
                + buildRow("Preferred Date", nvl(booking.getPreferredDate(), "To be confirmed"))
                + buildRow("Preferred Time", nvl(booking.getPreferredTime(), "To be confirmed"))
                + (isBlank(booking.getMessage()) ? "" : buildRow("Notes", booking.getMessage()))
                + buildRow("Status", "⏳ Pending Confirmation");

        return wrapEmail(
            "Your Booking Has Been Received",
            "#f59e0b",
            """
            <p style="color:#1c2733;font-size:16px;margin:0 0 24px;">Hi <strong>%s</strong>,</p>
            <p style="color:#5c6b7a;font-size:15px;line-height:1.7;margin:0 0 28px;">
              Thank you for booking with <strong>SED Motors</strong>. Our team will review your request
              and contact you shortly to confirm your appointment and provide a cost estimate.
            </p>
            """.formatted(booking.getCustomerName()),
            buildTable("Booking Reference #" + booking.getId(), rows),
            "If you have any questions, please call us at <strong>(675) XXX XXXX</strong> or reply to this email."
        );
    }

    private String buildStatusUpdateHtml(Booking booking, boolean confirmed) {
        String banner = confirmed
            ? "<span style='color:#065f46;'>🎉 Great news — your booking has been <strong>Confirmed</strong>!</span>"
            : "<span style='color:#7f1d1d;'>Your booking has been reviewed and unfortunately <strong>could not be accommodated</strong> at this time.</span>";

        String message = confirmed
            ? "We look forward to seeing you. Please arrive at our workshop at your scheduled time. Our technicians will be ready to assist you."
            : "We apologize for the inconvenience. Please feel free to contact us to discuss alternative dates or services.";

        String rows = buildRow("Booking #", String.valueOf(booking.getId()))
                + buildRow("Service", booking.getServiceType())
                + buildRow("Vehicle", booking.getVehicleDetails())
                + buildRow("Date", nvl(booking.getPreferredDate(), "—"))
                + buildRow("Status", confirmed ? "✅ CONFIRMED" : "❌ REJECTED");

        return wrapEmail(
            "Booking Status Update",
            confirmed ? "#10b981" : "#ef4444",
            """
            <p style="color:#1c2733;font-size:16px;margin:0 0 16px;">Hi <strong>%s</strong>,</p>
            <p style="font-size:15px;line-height:1.7;margin:0 0 24px;background:%s;padding:14px;border-radius:8px;">%s</p>
            <p style="color:#5c6b7a;font-size:15px;line-height:1.7;margin:0 0 28px;">%s</p>
            """.formatted(
                booking.getCustomerName(),
                confirmed ? "#d1fae5" : "#fee2e2",
                banner,
                message
            ),
            buildTable("Booking Details", rows),
            "Questions? Call <strong>(675) XXX XXXX</strong> or reply to this email."
        );
    }

    private String buildInquiryReplyHtml(Inquiry inquiry) {
        return wrapEmail(
            "Reply to Your Inquiry",
            "#0ea5e9",
            """
            <p style="color:#1c2733;font-size:16px;margin:0 0 16px;">Hi <strong>%s</strong>,</p>
            <p style="color:#5c6b7a;font-size:15px;line-height:1.7;margin:0 0 20px;">
              Thank you for reaching out to us. Here is our response to your inquiry:
              <strong>"%s"</strong>
            </p>
            <div style="background:#f0f9ff;border-left:4px solid #0ea5e9;padding:16px 20px;border-radius:0 8px 8px 0;margin-bottom:24px;">
              <p style="font-size:13px;color:#64748b;margin:0 0 6px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">Your Question</p>
              <p style="color:#1e293b;font-size:14px;line-height:1.6;margin:0;">%s</p>
            </div>
            <div style="background:#f8fafc;border-left:4px solid #10b981;padding:16px 20px;border-radius:0 8px 8px 0;margin-bottom:28px;">
              <p style="font-size:13px;color:#64748b;margin:0 0 6px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">Our Reply</p>
              <p style="color:#1e293b;font-size:14px;line-height:1.7;margin:0;">%s</p>
            </div>
            """.formatted(
                inquiry.getCustomerName(),
                inquiry.getSubject(),
                inquiry.getMessage(),
                inquiry.getAdminReply()
            ),
            "",
            "If you have further questions, call us at <strong>(675) XXX XXXX</strong> or reply to this email."
        );
    }

    private String buildCustomEmailHtml(String toName, String subject, String body) {
        return wrapEmail(
            subject,
            "#f59e0b",
            """
            <p style="color:#1c2733;font-size:16px;margin:0 0 24px;">Hi <strong>%s</strong>,</p>
            """.formatted(nvl(toName, "Valued Customer")),
            "<div style='color:#374151;font-size:15px;line-height:1.8;white-space:pre-wrap;'>" + escHtml(body) + "</div>",
            "Thank you for choosing SED Motors. Call <strong>(675) XXX XXXX</strong> for assistance."
        );
    }

    // ── HTML Primitives ───────────────────────────────────────────────────────

    private String wrapEmail(String heading, String accentColor, String bodyContent, String tableContent, String footer) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title></head>
            <body style="margin:0;padding:0;background:#f7f8fa;font-family:'Segoe UI',Roboto,Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f7f8fa;padding:40px 20px;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0"
                    style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.08);max-width:600px;width:100%%;">
                    <tr><td style="background:#0f2942;padding:28px 40px;text-align:center;">
                      <h1 style="color:#fff;margin:0;font-size:24px;font-weight:800;">SED MOTORS</h1>
                      <p style="color:#94b8d0;margin:5px 0 0;font-size:12px;letter-spacing:1px;text-transform:uppercase;">Repairs &amp; Spare Parts &mdash; Port Moresby</p>
                    </td></tr>
                    <tr><td style="background:%s;padding:14px 40px;text-align:center;">
                      <p style="margin:0;font-weight:700;font-size:15px;color:#fff;">%s</p>
                    </td></tr>
                    <tr><td style="padding:32px 40px;">
                      %s
                      %s
                      <p style="color:#5c6b7a;font-size:13px;line-height:1.7;margin:24px 0 0;">%s</p>
                    </td></tr>
                    <tr><td style="background:#0f2942;padding:20px 40px;text-align:center;">
                      <p style="color:#94b8d0;font-size:12px;margin:0;">&copy; 2026 SED Motors &mdash; Boroko, NCD, Papua New Guinea</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body></html>
            """.formatted(heading, accentColor, heading, bodyContent, tableContent, footer);
    }

    private String buildTable(String title, String rows) {
        if (rows == null || rows.isBlank()) return "";
        return """
            <table width="100%%" cellpadding="0" cellspacing="0"
              style="background:#f7f8fa;border-radius:10px;border:1px solid #e3e7ec;overflow:hidden;margin-top:4px;">
              <tr><td colspan="2" style="padding:14px 20px;border-bottom:1px solid #e3e7ec;background:#f0f4f8;">
                <strong style="color:#0f2942;font-size:13px;letter-spacing:0.5px;text-transform:uppercase;">%s</strong>
              </td></tr>
              %s
            </table>
            """.formatted(title, rows);
    }

    private String buildRow(String label, String value) {
        return """
            <tr>
              <td style="padding:11px 20px;border-bottom:1px solid #e3e7ec;color:#5c6b7a;font-size:13px;font-weight:600;width:40%%;">%s</td>
              <td style="padding:11px 20px;border-bottom:1px solid #e3e7ec;color:#1c2733;font-size:14px;">%s</td>
            </tr>
            """.formatted(label, nvl(value, "—"));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String  nvl(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
