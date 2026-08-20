package com.yggormartins.itauteste.adapter.out.memory;

import com.yggormartins.itauteste.application.port.out.TransactionWindow;
import com.yggormartins.itauteste.domain.model.Statistics;
import com.yggormartins.itauteste.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Ring buffer com um slot por milissegundo.
 *
 * <p>POST é O(1) e lock-free por CAS. GET é O(B), em que B é fixado pela
 * duração da janela (60.001 slots por padrão), e não cresce com a quantidade
 * de transações. Estados de bucket são imutáveis para publicar count/sum/min/max
 * como uma única unidade coerente.</p>
 */
public final class AtomicRingTransactionWindow implements TransactionWindow {
    private final long windowMillis;
    private final int slotCount;
    private final AtomicReferenceArray<Bucket> ring;
    private final AtomicLong generation = new AtomicLong();

    public AtomicRingTransactionWindow(Duration window) {
        this.windowMillis = window.toMillis();
        if (windowMillis <= 0 || windowMillis >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("window must fit in an int-sized ring");
        }
        this.slotCount = Math.toIntExact(windowMillis + 1);
        this.ring = new AtomicReferenceArray<>(slotCount);
    }

    @Override
    public void add(Transaction transaction) {
        long epochMillis = transaction.occurredAt().toEpochMilli();
        int index = Math.floorMod(epochMillis, slotCount);
        long currentGeneration = generation.get();

        for (;;) {
            Bucket current = ring.get(index);
            Bucket updated;
            if (current == null || current.epochMillis != epochMillis
                    || current.generation != currentGeneration) {
                updated = Bucket.first(epochMillis, currentGeneration,
                        transaction.amount());
            } else {
                updated = current.add(transaction.amount());
            }

            if (ring.compareAndSet(index, current, updated)) {
                // Se DELETE concorreu com este CAS, publicar novamente na geração
                // atual evita perder um POST que terminou depois da limpeza.
                long observedGeneration = generation.get();
                if (observedGeneration == currentGeneration) {
                    return;
                }
                currentGeneration = observedGeneration;
            }
        }
    }

    @Override
    public Statistics snapshot(Instant now) {
        long end = now.toEpochMilli();
        long start = end - windowMillis;
        long expectedGeneration = generation.get();
        long count = 0;
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal minimum = null;
        BigDecimal maximum = null;

        for (int index = 0; index < slotCount; index++) {
            Bucket bucket = ring.get(index);
            if (bucket == null || bucket.generation != expectedGeneration
                    || bucket.epochMillis < start || bucket.epochMillis > end) {
                continue;
            }
            count += bucket.count;
            sum = sum.add(bucket.sum);
            minimum = minimum == null || bucket.minimum.compareTo(minimum) < 0
                    ? bucket.minimum : minimum;
            maximum = maximum == null || bucket.maximum.compareTo(maximum) > 0
                    ? bucket.maximum : maximum;
        }
        return count == 0 ? Statistics.empty()
                : Statistics.of(count, sum, minimum, maximum);
    }

    @Override
    public void clear() {
        // Limpeza lógica O(1); referências antigas são reutilizadas naturalmente.
        generation.incrementAndGet();
    }

    private record Bucket(long epochMillis, long generation, long count,
                          BigDecimal sum, BigDecimal minimum, BigDecimal maximum) {
        private static Bucket first(long epochMillis, long generation,
                                    BigDecimal amount) {
            return new Bucket(epochMillis, generation, 1, amount, amount, amount);
        }

        private Bucket add(BigDecimal amount) {
            return new Bucket(epochMillis, generation, count + 1, sum.add(amount),
                    amount.compareTo(minimum) < 0 ? amount : minimum,
                    amount.compareTo(maximum) > 0 ? amount : maximum);
        }
    }
}
