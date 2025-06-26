package com.ururulab.ururu.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	@Bean("imageUploadExecutor")
	public Executor imageUploadExecutor() {
		ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
		exec.setCorePoolSize(2);
		exec.setMaxPoolSize(4);
		exec.setQueueCapacity(50);
		exec.setThreadNamePrefix("img-upload-");
		exec.initialize();
		return exec;
	}
}
