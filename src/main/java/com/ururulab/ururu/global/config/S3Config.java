package com.ururulab.ururu.global.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class S3Config {

	@Value("${cloud.aws.region}")
	private String region;

	@Value("${cloud.aws.credentials.access-key}")
	private String accessKey;

	@Value("${cloud.aws.credentials.secret-key}")
	private String secretKey;

	@Value("${cloud.aws.endpoint:}")
	private String endpoint;

	@Value("${cloud.aws.s3.path-style-access-enabled:false}")
	private boolean pathStyleAccessEnabled;

	@Bean
	public S3Client s3Client() {
		AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(creds))
				.forcePathStyle(pathStyleAccessEnabled);

		if (StringUtils.hasText(endpoint)) {
			builder.endpointOverride(URI.create(endpoint));
		}

		return builder.build();
	}
}
