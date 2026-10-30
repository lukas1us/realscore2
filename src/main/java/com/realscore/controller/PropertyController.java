package com.realscore.controller;

import com.realscore.model.Property;
import com.realscore.repository.PropertyRepository;
import com.realscore.service.PropertyScoringService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyRepository propertyRepository;
    private final PropertyScoringService propertyScoringService;

    public PropertyController(PropertyRepository propertyRepository, PropertyScoringService propertyScoringService) {
        this.propertyRepository = propertyRepository;
        this.propertyScoringService = propertyScoringService;
    }

    @GetMapping
    public List<Property> list() {
        return propertyRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Property create(@Valid @RequestBody Property property) {
        return propertyRepository.save(property);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Property> get(@PathVariable Long id) {
        return propertyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/score")
    public ResponseEntity<Map<String, Object>> score(@PathVariable Long id) {
        return propertyRepository.findById(id)
                .map(property -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", property.getId(),
                        "score", propertyScoringService.score(property))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
