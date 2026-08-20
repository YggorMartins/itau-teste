package com.yggormartins.itauteste.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsTest {
    @Test
    void returnsCanonicalEmptySnapshot() {
        assertThat(Statistics.empty()).isEqualTo(new Statistics(0, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertThat(Statistics.of(0, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN))
                .isEqualTo(Statistics.empty());
    }

    @Test
    void calculatesAverageWithDecimalPrecision() {
        Statistics statistics = Statistics.of(3, new BigDecimal("10.00"),
                new BigDecimal("1.00"), new BigDecimal("7.00"));
        assertThat(statistics.average()).isEqualByComparingTo("3.333333333333333333333333333333333");
    }
}
