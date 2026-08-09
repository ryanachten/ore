package com.ryanachten.ore.common.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Immutable binding for the {@code aws.*} configuration properties. */
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
  private final URI endpointOverride;
  private final String region;
  private final String accessKeyId;
  private final String secretAccessKeyId;

  /** Creates a configuration snapshot from the bound {@code aws.*} properties. */
  public AwsProperties(
      URI endpointOverride, String region, String accessKeyId, String secretAccessKeyId) {
    this.endpointOverride = endpointOverride;
    this.region = region;
    this.accessKeyId = accessKeyId;
    this.secretAccessKeyId = secretAccessKeyId;
  }

  public URI getEndpointOverride() {
    return endpointOverride;
  }

  public String getRegion() {
    return region;
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public String getSecretAccessKeyId() {
    return secretAccessKeyId;
  }
}
