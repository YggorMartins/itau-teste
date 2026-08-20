package com.yggormartins.itauteste.domain.service;

import com.yggormartins.itauteste.domain.exception.InvalidTransactionException;
import com.yggormartins.itauteste.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Ponto único das invariantes monetárias e temporais. */
public final class TransactionPolicy {
    private final Clock clock;
    private final Duration window;

    public TransactionPolicy(Clock clock, Duration window) {
        this.clock = clock;
        this.window = window;
    }

    public Transaction validate(BigDecimal amount, Instant occurredAt) {
        if (amount == null || occurredAt == null) {
            throw invalid("required_field", "valor e dataHora são obrigatórios");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalid("non_positive_amount", "valor deve ser maior que zero");
        }

        // Capturar o relógio uma vez elimina decisões diferentes na mesma operação.
        Instant now = clock.instant();
        if (occurredAt.isAfter(now)) {
            throw invalid("future_transaction", "dataHora não pode estar no futuro");
        }
        if (occurredAt.isBefore(now.minus(window))) {
            throw invalid("expired_transaction", "dataHora está fora da janela aceita");
        }
        return new Transaction(amount, occurredAt);
    }

    private static InvalidTransactionException invalid(String code, String message) {
        return new InvalidTransactionException(code, message);
    }
}
