package com.vsvidinsky.patterns.multiresourcelocking.benchmark;

import com.vsvidinsky.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.patterns.multiresourcelocking.Warehouse;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BenchmarkWarehouse implements Warehouse {
    private static final long SIMULATION_WORK_TIME_MS = 5;

    private final String id;
    private final Lock lock = new ReentrantLock();

    public BenchmarkWarehouse(String id) {
        this.id = id;
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
            Thread.sleep(SIMULATION_WORK_TIME_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
