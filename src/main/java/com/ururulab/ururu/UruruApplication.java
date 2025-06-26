package com.ururulab.ururu;

import com.ururulab.ururu.auth.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class UruruApplication {

	public static void main(String[] args) {
		SpringApplication.run(UruruApplication.class, args);
	}

}
