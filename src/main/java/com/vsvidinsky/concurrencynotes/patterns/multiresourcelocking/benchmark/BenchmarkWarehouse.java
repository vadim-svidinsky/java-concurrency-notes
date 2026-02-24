package com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.benchmark;

import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.Warehouse;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BenchmarkWarehouse implements Warehouse {
    private final String id;
    private final long simulationWorkTimeMs;
    private final Lock lock = new ReentrantLock();

    public BenchmarkWarehouse(String id, long simulationWorkTimeMs) {
        this.id = id;
        this.simulationWorkTimeMs = simulationWorkTimeMs;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void add(Item item) {
        simulateWork();
    }

    @Override
    public void remove(Item item) {
        simulateWork();
    }

    @Override
    public boolean hasItem(Item item) {
        // always has items, consider as fast operation
        return true;
    }

    @Override
    public Lock getLock() {
        return lock;
    }

    private void simulateWork() {
        try {
            Thread.sleep(simulationWorkTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
