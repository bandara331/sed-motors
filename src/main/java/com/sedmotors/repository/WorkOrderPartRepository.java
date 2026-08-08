package com.sedmotors.repository;

import com.sedmotors.model.WorkOrderPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderPartRepository extends JpaRepository<WorkOrderPart, Long> {
    List<WorkOrderPart> findByWorkOrderId(Long workOrderId);
    void deleteByWorkOrderId(Long workOrderId);
}
