package com.ryanachten.ore.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@AutoConfiguration
@ConditionalOnClass(SnsClient.class)
@EnableConfigurationProperties(AwsProperties.class)
public class AwsAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
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

  @Bean
  @ConditionalOnMissingBean
  public SnsTopicResolver snsTopicResolver(SnsClient snsClient) {
    return new SnsTopicResolver(snsClient);
  }
}
