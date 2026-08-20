package com.yggormartins.itauteste.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull Duration statisticsWindow,
        @NotNull @Valid Security security) {

    public record Security(
            @NotNull @Valid RateLimit rateLimit,
            @Min(1024) @Max(1_048_576) int maxPayloadBytes,
            boolean oauth2Enabled) {
    }

    public record RateLimit(
            @Min(1) long capacity,
            @Min(1) long refillTokens,
            @NotNull Duration refillPeriod,
            @NotNull Duration temporaryBlock,
            @Min(100) long maximumTrackedClients) {
    }
}
