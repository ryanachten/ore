package com.ryanachten.ore.common.config;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
    @NotBlank String region,
    @NotBlank String accessKeyId,
    @NotBlank String secretAccessKeyId,
    URI endpointOverride) {}
