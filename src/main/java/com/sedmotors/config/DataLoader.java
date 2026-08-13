package com.sedmotors.config;

import com.sedmotors.model.AuthProvider;
import com.sedmotors.model.Part;
import com.sedmotors.model.Role;
import com.sedmotors.model.User;
import com.sedmotors.repository.PartRepository;
import com.sedmotors.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import com.sedmotors.model.Booking;
import com.sedmotors.model.WorkOrder;
import com.sedmotors.model.Invoice;
import com.sedmotors.model.Supplier;
import com.sedmotors.repository.BookingRepository;
import com.sedmotors.repository.WorkOrderRepository;
import com.sedmotors.repository.InvoiceRepository;
import com.sedmotors.repository.SupplierRepository;

/**
 * DataLoader — Seeds initial Parts and default Admin account on first startup.
 * Author: Sasmit Tejan
 */
@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // ─── Seed sample parts ────────────────────────────────────────────────────
        if (partRepository.count() == 0) {
            System.out.println("Seeding initial parts data...");

            partRepository.save(new Part(null, "Toyota Hilux Oil Filter", "engine", "Genuine oil filter for 2015-2022 Toyota Hilux.", new BigDecimal("45.00"), 50, null, 10));
            partRepository.save(new Part(null, "Ford Ranger Brake Pads (Front)", "suspension", "Ceramic front brake pads for Ford Ranger.", new BigDecimal("120.00"), 30, null, 10));
            partRepository.save(new Part(null, "Nissan Patrol Alternator", "electrical", "12V 100A Alternator for Nissan Patrol GU/GQ.", new BigDecimal("350.00"), 10, null, 10));
            partRepository.save(new Part(null, "Mitsubishi Triton Headlight Assembly", "body", "Left-side headlamp assembly for Mitsubishi Triton MR.", new BigDecimal("280.00"), 15, null, 10));
            partRepository.save(new Part(null, "Heavy Duty Snorkel Kit", "accessories", "4x4 Snorkel kit for Toyota Landcruiser 70 series.", new BigDecimal("450.00"), 8, null, 10));
        }

        // ─── Seed sample suppliers ────────────────────────────────────────────────────
        if (supplierRepository.count() == 0) {
            System.out.println("Seeding suppliers data...");
            Supplier s1 = new Supplier(); s1.setName("Genuine Auto Parts Co."); s1.setContactPerson("John Doe"); s1.setPhone("555-0101"); s1.setEmail("sales@genuineauto.com"); s1.setAddress("123 Industrial Park, POM"); s1.setPartsSupplied("Engine, Suspension");
            Supplier s2 = new Supplier(); s2.setName("ElectroMotive Supplies"); s2.setContactPerson("Jane Smith"); s2.setPhone("555-0202"); s2.setEmail("orders@electromotive.com"); s2.setAddress("45 Tech Blvd, POM"); s2.setPartsSupplied("Electrical, Batteries");
            supplierRepository.saveAll(Arrays.asList(s1, s2));
        }

        // ─── Seed sample bookings ────────────────────────────────────────────────────
        if (bookingRepository.count() == 0) {
            System.out.println("Seeding bookings data...");
            Booking b1 = new Booking(null, "Alice Johnson", "555-1234", "alice@example.com", "2019 Toyota Hilux", "BAM-123", "Full Service", "2026-08-20", "09:00", "Car making a weird noise", "PENDING");
            Booking b2 = new Booking(null, "Bob Williams", "555-5678", "bob@example.com", "2021 Ford Ranger", "CDA-456", "Brake Replacement", "2026-08-21", "10:30", "Brakes are squeaking", "CONFIRMED");
            Booking b3 = new Booking(null, "Charlie Brown", "555-9012", "charlie@example.com", "2018 Nissan Patrol", "EFG-789", "Electrical Diagnostics", "2026-08-15", "14:00", "Alternator failing", "COMPLETED");
            bookingRepository.saveAll(Arrays.asList(b1, b2, b3));

            // ─── Seed sample work orders (for confirmed/completed bookings) ────────────
            if (workOrderRepository.count() == 0) {
                System.out.println("Seeding work orders data...");
                WorkOrder w1 = new WorkOrder(); w1.setBooking(b2); w1.setMechanicName("Mike Tyson"); w1.setBayNumber("Bay 1"); w1.setStatus(WorkOrder.Status.IN_PROGRESS); w1.setLaborHours(new BigDecimal("2.5")); w1.setNotes("Inspected brakes. Pads are worn out completely. Replacing front pads.");
                WorkOrder w2 = new WorkOrder(); w2.setBooking(b3); w2.setMechanicName("Sarah Connor"); w2.setBayNumber("Bay 3"); w2.setStatus(WorkOrder.Status.COMPLETED); w2.setLaborHours(new BigDecimal("4.0")); w2.setNotes("Tested alternator. Output was 11.2V. Replaced with new unit. Now charging at 14.4V.");
                workOrderRepository.saveAll(Arrays.asList(w1, w2));

                // ─── Seed sample invoices (for completed work orders) ─────────────────
                if (invoiceRepository.count() == 0) {
                    System.out.println("Seeding invoices data...");
                    Invoice i1 = new Invoice(); i1.setWorkOrder(w2); i1.setPartsTotal(new BigDecimal("350.00")); i1.setLaborTotal(new BigDecimal("200.00")); i1.setTaxAmount(new BigDecimal("55.00")); i1.setGrandTotal(new BigDecimal("605.00")); i1.setPaymentStatus(Invoice.PaymentStatus.UNPAID); i1.setPaymentMethod(Invoice.PaymentMethod.NONE); i1.setNotes("Net 30 days. Please pay promptly.");
                    invoiceRepository.save(i1);
                }
            }
        }
        System.out.println("Data seeding completed.");
    }
}
