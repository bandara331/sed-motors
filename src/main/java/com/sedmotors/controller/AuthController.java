package com.sedmotors.controller;

import com.sedmotors.model.Admin;
import com.sedmotors.model.AuthProvider;
import com.sedmotors.model.Role;
import com.sedmotors.model.User;
import com.sedmotors.repository.AdminRepository;
import com.sedmotors.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");
        String password = request.get("password");

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body("Missing fields");
        }

        if (userRepository.existsByEmail(email) || adminRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.ROLE_USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");
        String password = request.get("password");

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body("Missing fields");
        }

        if (userRepository.existsByEmail(email) || adminRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email is already registered");
        }

        Admin admin = new Admin();
        admin.setEmail(email);
        admin.setName(name);
        admin.setPassword(passwordEncoder.encode(password));
        
        adminRepository.save(admin);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Admin registered successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String email = authentication.getName(); // Usually email
        
        // First check Admin table
        Optional<Admin> adminOptional = adminRepository.findByEmail(email);
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", admin.getId());
            userData.put("email", admin.getEmail());
            userData.put("name", admin.getName());
            userData.put("role", admin.getRole().name());
            userData.put("authProvider", "LOCAL");
            return ResponseEntity.ok(userData);
        }

        // Then check User table
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("role", user.getRole().name());
            userData.put("authProvider", user.getAuthProvider().name());
            return ResponseEntity.ok(userData);
        }

        return ResponseEntity.status(404).body("User record not found");
    }
}
