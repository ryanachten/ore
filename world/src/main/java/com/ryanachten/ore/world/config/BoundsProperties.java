package com.ryanachten.ore.world.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bounds")
public record BoundsProperties(int width, int height) {}
