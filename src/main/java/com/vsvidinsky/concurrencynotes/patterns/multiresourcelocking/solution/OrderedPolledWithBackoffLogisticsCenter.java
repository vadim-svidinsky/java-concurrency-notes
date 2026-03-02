package com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.solution;

import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.LogisticsCenter;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.Warehouse;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.BackoffConfig;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.TransferResult;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Not recommended, it's better to choose {@link PolledWithBackoffLogisticsCenter} instead.
 */
public class OrderedPolledWithBackoffLogisticsCenter implements LogisticsCenter {
    private final BackoffConfig config;

    public OrderedPolledWithBackoffLogisticsCenter(BackoffConfig config) {
        this.config = config;
    }

    @Override
    public TransferResult transfer(Warehouse source, Warehouse destination, Item item) {
        boolean sourceFirst = source.getId().compareTo(destination.getId()) < 0;
        Lock firstLock = sourceFirst ? source.getLock() : destination.getLock();
        Lock secondLock = sourceFirst ? destination.getLock() : source.getLock();

        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(config.timeoutMs());
        long backoff = config.initialBackoffMs();

        while (System.nanoTime() < deadline) {
            if (firstLock.tryLock()) {
                try {
                    if (secondLock.tryLock()) {
                        try {
                            if (!source.hasItem(item)) {
                                return TransferResult.ITEM_NOT_FOUND;
                            }
                            source.remove(item);
                            destination.add(item);
                            return TransferResult.SUCCESS;
                        } finally {
                            secondLock.unlock();
                        }
                    }
                } finally {
                    firstLock.unlock();
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
