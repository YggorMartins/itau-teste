package com.yggormartins.itauteste.domain.service;

import com.yggormartins.itauteste.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionPolicyTest {
    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");
    private final TransactionPolicy policy = new TransactionPolicy(
            Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(60));

    @Test
    void acceptsPositiveAmountAtCurrentInstant() {
        assertThat(policy.validate(new BigDecimal("10.25"), NOW))
                .satisfies(transaction -> {
                    assertThat(transaction.amount()).isEqualByComparingTo("10.25");
                    assertThat(transaction.occurredAt()).isEqualTo(NOW);
                });
    }

    @Test
    void acceptsExactWindowCutoff() {
        assertThatCode(() -> policy.validate(BigDecimal.ONE, NOW.minusSeconds(60)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullFields() {
        assertCode(() -> policy.validate(null, NOW), "required_field");
        assertCode(() -> policy.validate(BigDecimal.ONE, null), "required_field");
    }

    @Test
    void rejectsZeroAndNegativeAmounts() {
        assertCode(() -> policy.validate(BigDecimal.ZERO, NOW), "non_positive_amount");
        assertCode(() -> policy.validate(new BigDecimal("-0.01"), NOW),
                "non_positive_amount");
    }

    @Test
    void rejectsFutureAndExpiredTransactions() {
        assertCode(() -> policy.validate(BigDecimal.ONE, NOW.plusMillis(1)),
                "future_transaction");
        assertCode(() -> policy.validate(BigDecimal.ONE,
                NOW.minusSeconds(60).minusMillis(1)), "expired_transaction");
    }

    private static void assertCode(Runnable action, String expectedCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(InvalidTransactionException.class)
                .satisfies(error -> assertThat(((InvalidTransactionException) error).code())
                        .isEqualTo(expectedCode));
    }
}
