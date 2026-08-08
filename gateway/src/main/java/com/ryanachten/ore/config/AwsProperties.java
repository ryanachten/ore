package com.ryanachten.ore.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Immutable binding for the {@code aws.*} configuration properties. */
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
  private final URI endpointOverride;
  private final String accessKeyId;
  private final String secretAccessKeyId;

  /** Creates a configuration snapshot from the bound {@code aws.*} properties. */
  public AwsProperties(URI endpointOverride, String accessKeyId, String secretAccessKeyId) {
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
