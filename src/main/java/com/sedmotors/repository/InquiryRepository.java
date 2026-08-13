package com.sedmotors.repository;

import com.sedmotors.model.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * InquiryRepository — JPA Repository for customer inquiries.
 * Author: Sasmit Tejan
 */
@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByOrderByCreatedAtDesc();
    List<Inquiry> findByEmailOrderByCreatedAtDesc(String email);
    long countByStatus(Inquiry.Status status);
}
