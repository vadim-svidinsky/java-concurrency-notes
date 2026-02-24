package com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.solution;

import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.LogisticsCenter;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.TransferResult;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.Warehouse;

import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.BackoffConfig;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class PolledWithBackoffLogisticsCenter implements LogisticsCenter {
    private final BackoffConfig config;

    public PolledWithBackoffLogisticsCenter(BackoffConfig config) {
        this.config = config;
    }

    @Override
    public TransferResult transfer(Warehouse source, Warehouse destination, Item item) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(config.timeoutMs());
        long backoff = config.initialBackoffMs();

        while (System.nanoTime() < deadline) {
            if (source.getLock().tryLock()) {
                try {
                    if (destination.getLock().tryLock()) {
                        try {
                            if (!source.hasItem(item)) {
                                return TransferResult.ITEM_NOT_FOUND;
                            }
                            source.remove(item);
                            destination.add(item);
                            return TransferResult.SUCCESS;
                        } finally {
                            destination.getLock().unlock();
                        }
                    }
                } finally {
                    source.getLock().unlock();
                }
            }
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(config.initialBackoffMs(), backoff + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return TransferResult.INTERRUPTED;
            }
            backoff = Math.min(backoff * 2, config.maxBackoffMs());
        }

        return TransferResult.TIMEOUT;
    }
}
