package com.sedmotors.controller;

import com.sedmotors.model.Part;
import com.sedmotors.repository.PartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
@CrossOrigin("*") // Allow frontend to access the API if run on different ports
public class PartController {

    @Autowired
    private PartRepository partRepository;

    @GetMapping
    public List<Part> getAllParts(@RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty() && !category.equalsIgnoreCase("all")) {
            return partRepository.findByCategory(category);
        }
        return partRepository.findAll();
    }

    @PostMapping
    public Part createPart(@RequestBody Part part) {
        return partRepository.save(part);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Part> updatePart(@PathVariable Long id, @RequestBody Part partDetails) {
        return partRepository.findById(id)
                .map(part -> {
                    part.setName(partDetails.getName());
                    part.setCategory(partDetails.getCategory());
                    part.setDescription(partDetails.getDescription());
                    part.setPrice(partDetails.getPrice());
                    part.setStockQuantity(partDetails.getStockQuantity());
                    part.setImageUrl(partDetails.getImageUrl());
                    return ResponseEntity.ok(partRepository.save(part));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePart(@PathVariable Long id) {
        return partRepository.findById(id)
                .map(part -> {
                    partRepository.delete(part);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
