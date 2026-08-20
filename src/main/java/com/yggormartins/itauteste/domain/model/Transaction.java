package com.yggormartins.itauteste.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Entidade imutável; só deve ser criada depois da validação das invariantes. */
public record Transaction(BigDecimal amount, Instant occurredAt) {
    public Transaction {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
