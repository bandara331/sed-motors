package com.sedmotors.repository;

import com.sedmotors.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByOrderByIdDesc();
    List<Booking> findByVehicleRegistrationContainingIgnoreCase(String vehicleRegistration);
    List<Booking> findByCustomerNameContainingIgnoreCase(String customerName);
}
