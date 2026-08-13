package com.sedmotors.repository;

import com.sedmotors.model.Inquiry;
import com.sedmotors.model.InquiryMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryMessageRepository extends JpaRepository<InquiryMessage, Long> {
    List<InquiryMessage> findByInquiryOrderBySentAtAsc(Inquiry inquiry);
    List<InquiryMessage> findByInquiryIdOrderBySentAtAsc(Long inquiryId);
}
