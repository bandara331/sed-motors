package com.sedmotors.controller;

import com.sedmotors.config.EmailService;
import com.sedmotors.model.Booking;
import com.sedmotors.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin("*")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByIdDesc();
    }

    @PostMapping
    public Booking createBooking(@RequestBody Booking booking) {
        if (booking.getStatus() == null || booking.getStatus().isEmpty()) {
            booking.setStatus("PENDING");
        }
        Booking savedBooking = bookingRepository.save(booking);

        // Push real-time notification to the admin dashboard via WebSocket
        messagingTemplate.convertAndSend("/topic/admin-notifications", savedBooking);

        // Send confirmation email to the customer (runs asynchronously, won't delay response)
        emailService.sendBookingConfirmation(savedBooking);

        return savedBooking;
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> statusUpdate) {
        return bookingRepository.findById(id)
                .map(booking -> {
                    booking.setStatus(statusUpdate.get("status"));
                    return ResponseEntity.ok(bookingRepository.save(booking));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
