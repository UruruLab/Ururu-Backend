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

	@Bean(name = "imageDeleteExecutor")
	public Executor imageDeleteExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2); // 적절한 사이즈 설정
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("image-delete-");
		executor.initialize();
		return executor;
	}
}
