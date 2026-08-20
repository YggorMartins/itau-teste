package com.yggormartins.itauteste.application.port.out;

import com.yggormartins.itauteste.domain.model.Statistics;
import com.yggormartins.itauteste.domain.model.Transaction;

import java.time.Instant;

public interface TransactionWindow {
    void add(Transaction transaction);
    Statistics snapshot(Instant now);
    void clear();
}
