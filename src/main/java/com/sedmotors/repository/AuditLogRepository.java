package com.sedmotors.repository;

import com.sedmotors.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    List<AuditLog> findTop100ByOrderByTimestampDesc();
    List<AuditLog> findByActorNameContainingIgnoreCase(String name);
}
