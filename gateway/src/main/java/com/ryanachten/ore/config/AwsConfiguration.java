package com.ryanachten.ore.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public class AwsConfiguration {
    private final URI endpointOverride;
    private final String accessKeyId;
    private final String secretAccessKeyId;

    public AwsConfiguration(URI endpointOverride, String accessKeyId, String secretAccessKeyId) {
        this.endpointOverride = endpointOverride;
        this.accessKeyId = accessKeyId;
        this.secretAccessKeyId = secretAccessKeyId;
    }

    public URI getEndpointOverride() {
        return endpointOverride;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKeyId() {
        return secretAccessKeyId;
    }
}
