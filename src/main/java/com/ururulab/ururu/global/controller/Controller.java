package com.ururulab.ururu.global.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class Controller {

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> healthCheck() {
		try {
			Map<String, Object> healthData = Map.of(
					"status", "UP",
					"timestamp", LocalDateTime.now(),
					"service", "ururu-backend",
					"message", "Health Check OK"
			);
			return ResponseEntity.ok(healthData); // HTTP 200 OK
		} catch (Exception e) {
			Map<String, Object> errorData = Map.of(
					"status", "DOWN",
					"timestamp", LocalDateTime.now(),
					"service", "ururu-backend",
					"error", e.getMessage()
			);
			return ResponseEntity.status(500).body(errorData); // HTTP 500
		}
	}
}
