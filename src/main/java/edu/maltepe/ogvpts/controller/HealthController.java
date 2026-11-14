package edu.maltepe.ogvpts.controller;

import edu.maltepe.ogvpts.config.DatabaseHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DatabaseHealthService healthService;

    public HealthController(DatabaseHealthService healthService) {
        this.healthService = healthService;
    }

    /** Tarayıcıdan: http://localhost:8080/api/health/db */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> database() {
        try {
            return ResponseEntity.ok(healthService.check());
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "HATA");
            error.put("message", e.getMessage());
            return ResponseEntity.status(503).body(error);
        }
    }
}
