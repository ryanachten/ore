package com.ryanachten.ore.services;

import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.ryanachten.ore.config.AwsConfiguration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SnsException;

@Service
public class NotificationService {
    private final SnsClient snsClient;
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    public NotificationService(AwsConfiguration config) {

        var credentialsProvider = StaticCredentialsProvider
                .create(AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKeyId()));

        this.snsClient = SnsClient.builder()
                .credentialsProvider(credentialsProvider)
                .endpointOverride(config.getEndpointOverride())
                .build();
    }

    public void listTopics() {
        try {
            var listTopics = snsClient.listTopicsPaginator();
            listTopics.stream()
                    .flatMap(r -> r.topics().stream())
                    .forEach(content -> logger.info(" Topic ARN: " + content.topicArn()));

        } catch (SnsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
