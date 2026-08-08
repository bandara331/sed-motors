package com.sedmotors.repository;

import com.sedmotors.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findAllByOrderByIdDesc();
    List<WorkOrder> findByStatus(WorkOrder.Status status);
    List<WorkOrder> findByBookingId(Long bookingId);
}
