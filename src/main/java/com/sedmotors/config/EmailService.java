package com.sedmotors.config;

import com.sedmotors.model.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * EmailService — Sends HTML confirmation emails to customers after booking.
 * Author: Sasmit Tejan
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends a booking confirmation email asynchronously so it does not block the API response.
     */
    @Async
    public void sendBookingConfirmation(Booking booking) {
        // Only send if the customer provided an email address
        if (booking.getEmail() == null || booking.getEmail().isBlank()) {
            System.out.println("[EmailService] No email provided for booking #" + booking.getId() + " — skipping.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "SED Motors");
            helper.setTo(booking.getEmail());
            helper.setSubject("✅ Booking Confirmed — SED Motors #" + booking.getId());
            helper.setText(buildEmailHtml(booking), true); // true = HTML content

            mailSender.send(message);
            System.out.println("[EmailService] Confirmation email sent to " + booking.getEmail());

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // Log but don't crash the API if email fails
            System.err.println("[EmailService] Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Builds a professional, branded HTML email body for the booking confirmation.
     */
    private String buildEmailHtml(Booking booking) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Booking Confirmation</title>
            </head>
            <body style="margin:0;padding:0;background:#f7f8fa;font-family:'Segoe UI',Roboto,Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f7f8fa;padding:40px 20px;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);max-width:600px;width:100%%;">
                      
                      <!-- Header -->
                      <tr>
                        <td style="background:#0f2942;padding:32px 40px;text-align:center;">
                          <h1 style="color:#ffffff;margin:0;font-size:26px;font-weight:800;letter-spacing:-0.5px;">SED MOTORS</h1>
                          <p style="color:#94b8d0;margin:6px 0 0;font-size:13px;letter-spacing:1px;text-transform:uppercase;">Repairs &amp; Spare Parts &mdash; Port Moresby</p>
                        </td>
                      </tr>
                      
                      <!-- Success Banner -->
                      <tr>
                        <td style="background:#f0a23b;padding:16px 40px;text-align:center;">
                          <p style="margin:0;color:#0f2942;font-weight:700;font-size:16px;">
                            ✅ &nbsp;Your Service Booking Has Been Received!
                          </p>
                        </td>
                      </tr>
                      
                      <!-- Body -->
                      <tr>
                        <td style="padding:36px 40px;">
                          <p style="color:#1c2733;font-size:16px;margin:0 0 24px;">Hi <strong>%s</strong>,</p>
                          <p style="color:#5c6b7a;font-size:15px;line-height:1.7;margin:0 0 28px;">
                            Thank you for booking with <strong>SED Motors</strong>. Our team will review your request and contact you shortly to confirm your appointment and provide a cost estimate.
                          </p>
                          
                          <!-- Booking Details Table -->
                          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f7f8fa;border-radius:10px;border:1px solid #e3e7ec;overflow:hidden;">
                            <tr>
                              <td colspan="2" style="padding:16px 20px;border-bottom:1px solid #e3e7ec;background:#f0f4f8;">
                                <strong style="color:#0f2942;font-size:14px;letter-spacing:0.5px;text-transform:uppercase;">Booking Reference #%d</strong>
                              </td>
                            </tr>
                            %s
                          </table>
                          
                          <p style="color:#5c6b7a;font-size:14px;line-height:1.7;margin:28px 0 0;">
                            If you have any questions, please call us at <strong>(675) XXX XXXX</strong> or reply to this email.
                          </p>
                        </td>
                      </tr>
                      
                      <!-- Footer -->
                      <tr>
                        <td style="background:#0f2942;padding:24px 40px;text-align:center;">
                          <p style="color:#94b8d0;font-size:13px;margin:0;">
                            &copy; 2026 SED Motors &mdash; Boroko, National Capital District, Papua New Guinea
                          </p>
                        </td>
                      </tr>
                      
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                booking.getCustomerName(),
                booking.getId(),
                buildDetailsRows(booking)
        );
    }

    private String buildDetailsRows(Booking booking) {
        StringBuilder rows = new StringBuilder();
        appendRow(rows, "Customer Name", booking.getCustomerName());
        appendRow(rows, "Phone", booking.getPhone());
        appendRow(rows, "Vehicle", booking.getVehicleDetails());
        appendRow(rows, "Service Required", booking.getServiceType());
        appendRow(rows, "Preferred Date", booking.getPreferredDate() != null ? booking.getPreferredDate() : "To be confirmed");
        appendRow(rows, "Preferred Time", booking.getPreferredTime() != null ? booking.getPreferredTime() : "To be confirmed");
        if (booking.getMessage() != null && !booking.getMessage().isBlank()) {
            appendRow(rows, "Additional Notes", booking.getMessage());
        }
        appendRow(rows, "Status", "⏳ Pending Confirmation");
        return rows.toString();
    }

    private void appendRow(StringBuilder sb, String label, String value) {
        sb.append("""
            <tr>
              <td style="padding:12px 20px;border-bottom:1px solid #e3e7ec;color:#5c6b7a;font-size:13px;font-weight:600;width:40%%;">%s</td>
              <td style="padding:12px 20px;border-bottom:1px solid #e3e7ec;color:#1c2733;font-size:14px;">%s</td>
            </tr>
            """.formatted(label, value));
    }
}
