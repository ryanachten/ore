package com.ryanachten.ore.common.config;

import com.ryanachten.ore.common.services.KinesisStreamArnResolver;
import com.ryanachten.ore.common.services.SnsTopicArnResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.sns.SnsClient;

@AutoConfiguration
@ConditionalOnClass(SnsClient.class)
@EnableConfigurationProperties(AwsProperties.class)
public class AwsAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  public SnsClient snsClient(AwsProperties props) {
    return SnsClient.builder()
        .credentialsProvider(createCredentialProvider(props))
        .region(Region.of(props.region()))
        .endpointOverride(props.endpointOverride())
        .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public KinesisClient kinesisClient(AwsProperties props) {
    return KinesisClient.builder()
        .credentialsProvider(createCredentialProvider(props))
        .region(Region.of(props.region()))
        .endpointOverride(props.endpointOverride())
        .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public SnsTopicArnResolver snsTopicResolver(SnsClient snsClient) {
    return new SnsTopicArnResolver(snsClient);
  }

  @Bean
  @ConditionalOnMissingBean
  public KinesisStreamArnResolver kinesisStreamArnResolver(KinesisClient kinesisClient) {
    return new KinesisStreamArnResolver(kinesisClient);
  }

  private StaticCredentialsProvider createCredentialProvider(AwsProperties props) {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(props.accessKeyId(), props.secretAccessKeyId()));
  }
}
