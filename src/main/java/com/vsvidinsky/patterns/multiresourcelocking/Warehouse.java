package com.vsvidinsky.patterns.multiresourcelocking;

import com.vsvidinsky.patterns.multiresourcelocking.model.Item;

import java.util.concurrent.locks.Lock;

public interface Warehouse {
    String getId();
    void add(Item item);
    void remove(Item item);
    boolean hasItem(Item item);

    /**
     * Exposes the lock for external coordination.
     */
    Lock getLock();
}
