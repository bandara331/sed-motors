package com.sedmotors.repository;

import com.sedmotors.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByIdDesc();
    Optional<Invoice> findByWorkOrderId(Long workOrderId);
    List<Invoice> findByPaymentStatus(Invoice.PaymentStatus status);
}
