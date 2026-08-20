package com.yggormartins.itauteste.application.service;

import com.yggormartins.itauteste.application.port.out.TransactionWindow;
import com.yggormartins.itauteste.domain.model.Statistics;
import com.yggormartins.itauteste.domain.service.TransactionPolicy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

/** Caso de uso; orquestra o domínio sem conhecer HTTP ou armazenamento. */
public final class TransactionApplicationService {
    private final TransactionPolicy policy;
    private final TransactionWindow window;
    private final Clock clock;

    public TransactionApplicationService(TransactionPolicy policy,
                                         TransactionWindow window,
                                         Clock clock) {
        this.policy = policy;
        this.window = window;
        this.clock = clock;
    }

    public void create(BigDecimal amount, Instant occurredAt) {
        window.add(policy.validate(amount, occurredAt));
    }

    public Statistics statistics() {
        return window.snapshot(clock.instant());
    }

    public void clear() {
        window.clear();
    }
}
