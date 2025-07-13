package com.ururulab.ururu.global.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter requestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ururu_requests_total")
                .description("Total number of requests")
                .register(meterRegistry);
    }

    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ururu_request_duration")
                .description("Request duration")
                .register(meterRegistry);
    }
} 