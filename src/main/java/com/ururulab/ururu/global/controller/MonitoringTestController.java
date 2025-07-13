package com.ururulab.ururu.global.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringTestController {

    private final Counter testCounter;
    private final Timer testTimer;

    public MonitoringTestController(MeterRegistry meterRegistry) {
        this.testCounter = Counter.builder("ururu_test_requests")
                .description("Test requests counter")
                .register(meterRegistry);
        
        this.testTimer = Timer.builder("ururu_test_duration")
                .description("Test request duration")
                .register(meterRegistry);
    }

    @GetMapping("/test")
    public String testMetrics() {
        testCounter.increment();
        
        return testTimer.record(() -> {
            try {
                // 시뮬레이션된 작업 지연
                Thread.sleep(100);
                return "Monitoring test successful!";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Monitoring test interrupted!";
            }
        });
    }

    @GetMapping("/health")
    public String health() {
        return "Application is healthy!";
    }
} 