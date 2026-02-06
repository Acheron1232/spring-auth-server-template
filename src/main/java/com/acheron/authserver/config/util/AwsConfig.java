package com.acheron.authserver.config.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {
    @Value("${aws.public_key}")
    private String publicKey;

    @Value("${aws.private_key}")
    private String privateKey;

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(credentialsProvider())
                .region(Region.of(region))
                .build();
    }

    @Bean
    public KmsClient kmsClient() {
        return KmsClient.builder()
                .credentialsProvider(credentialsProvider())
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Presigner presigner() {
        return S3Presigner.builder()
                .credentialsProvider(credentialsProvider())
                .region(Region.of(region))
                .build();
    }

    private AwsCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(publicKey, privateKey));
    }
}
