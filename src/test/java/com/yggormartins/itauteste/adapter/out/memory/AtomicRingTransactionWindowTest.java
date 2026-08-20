package com.yggormartins.itauteste.adapter.out.memory;

import com.yggormartins.itauteste.domain.model.Statistics;
import com.yggormartins.itauteste.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtomicRingTransactionWindowTest {
    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");

    @Test
    void rejectsInvalidWindow() {
        assertThatThrownBy(() -> new AtomicRingTransactionWindow(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsEmptyAndAggregatesValidBuckets() {
        var window = new AtomicRingTransactionWindow(Duration.ofSeconds(60));
        assertThat(window.snapshot(NOW)).isEqualTo(Statistics.empty());

        window.add(transaction("10.10", NOW));
        window.add(transaction("20.20", NOW.minusMillis(1)));
        window.add(transaction("5.05", NOW.minusSeconds(60)));

        Statistics result = window.snapshot(NOW);
        assertThat(result.count()).isEqualTo(3);
        assertThat(result.sum()).isEqualByComparingTo("35.35");
        assertThat(result.minimum()).isEqualByComparingTo("5.05");
        assertThat(result.maximum()).isEqualByComparingTo("20.20");
    }

    @Test
    void expiresOutsideWindowAndReusesRingSlot() {
        var window = new AtomicRingTransactionWindow(Duration.ofMillis(2));
        window.add(transaction("1", NOW.minusMillis(3)));
        window.add(transaction("2", NOW));

        Statistics result = window.snapshot(NOW);
        assertThat(result.count()).isOne();
        assertThat(result.sum()).isEqualByComparingTo("2");
    }

    @Test
    void clearIsLogicalAndConstantTime() {
        var window = new AtomicRingTransactionWindow(Duration.ofSeconds(60));
        window.add(transaction("10", NOW));
        window.clear();
        assertThat(window.snapshot(NOW)).isEqualTo(Statistics.empty());

        window.add(transaction("3", NOW));
        assertThat(window.snapshot(NOW).sum()).isEqualByComparingTo("3");
    }

    @Test
    void doesNotLoseTenThousandConcurrentUpdates() throws Exception {
        var window = new AtomicRingTransactionWindow(Duration.ofSeconds(60));
        int writers = 10_000;
        var start = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = IntStream.range(0, writers)
                    .mapToObj(ignored -> executor.<Void>submit(() -> {
                        start.await();
                        window.add(transaction("1", NOW));
                        return null;
                    })).toList();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        }

        Statistics result = window.snapshot(NOW);
        assertThat(result.count()).isEqualTo(writers);
        assertThat(result.sum()).isEqualByComparingTo("10000");
    }

    private static Transaction transaction(String amount, Instant time) {
        return new Transaction(new BigDecimal(amount), time);
    }
}
