package com.ryanachten.ore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/** Configuration for AWS services. */
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsConfiguration {
  /** Builds an SNS client from the supplied AWS configuration. */
  @Bean
  public SnsClient snsClient(AwsProperties props) {
    var credentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(props.getAccessKeyId(), props.getSecretAccessKeyId()));

    return SnsClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(Region.of(props.getRegion()))
        .endpointOverride(props.getEndpointOverride())
        .build();
  }
}
