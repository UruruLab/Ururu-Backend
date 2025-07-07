package com.ururulab.ururu.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
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

	/**
	 * 재고 체크 전용 스레드풀
	 * 주문 완료 후 재고 소진 체크용
	 */
	@Bean("stockCheckExecutor")
	public TaskExecutor stockCheckExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("StockCheck-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}
