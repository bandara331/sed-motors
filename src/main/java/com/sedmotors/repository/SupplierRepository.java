package com.sedmotors.repository;

import com.sedmotors.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findAllByOrderByNameAsc();
    List<Supplier> findByNameContainingIgnoreCase(String name);
}
