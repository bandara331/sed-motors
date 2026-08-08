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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // ─── Seed sample parts ────────────────────────────────────────────────────
        if (partRepository.count() == 0) {
            System.out.println("Seeding initial parts data...");

            partRepository.save(new Part(null, "Toyota Hilux Oil Filter", "engine", "Genuine oil filter for 2015-2022 Toyota Hilux.", new BigDecimal("45.00"), 50));
            partRepository.save(new Part(null, "Ford Ranger Brake Pads (Front)", "suspension", "Ceramic front brake pads for Ford Ranger.", new BigDecimal("120.00"), 30));
            partRepository.save(new Part(null, "Nissan Patrol Alternator", "electrical", "12V 100A Alternator for Nissan Patrol GU/GQ.", new BigDecimal("350.00"), 10));
            partRepository.save(new Part(null, "Mitsubishi Triton Headlight Assembly", "body", "Left-side headlamp assembly for Mitsubishi Triton MR.", new BigDecimal("280.00"), 15));
            partRepository.save(new Part(null, "Heavy Duty Snorkel Kit", "accessories", "4x4 Snorkel kit for Toyota Landcruiser 70 series.", new BigDecimal("450.00"), 8));

            System.out.println("Data seeding completed.");
        }
    }
}
