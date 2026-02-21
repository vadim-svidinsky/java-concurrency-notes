package com.vsvidinsky.patterns.multiresourcelocking.solution;

import com.vsvidinsky.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.patterns.multiresourcelocking.LogisticsCenter;
import com.vsvidinsky.patterns.multiresourcelocking.model.TransferResult;
import com.vsvidinsky.patterns.multiresourcelocking.Warehouse;

import java.util.concurrent.locks.Lock;

public class LockOrderingLogisticsCenter implements LogisticsCenter {

    @Override
    public TransferResult transfer(Warehouse source, Warehouse destination, Item item) {
        Lock firstLock = source.getId().compareTo(destination.getId()) < 0
                ? source.getLock()
                : destination.getLock();
        Lock secondLock = source.getId().compareTo(destination.getId()) < 0
                ? destination.getLock()
                : source.getLock();

        firstLock.lock();
        try {
            secondLock.lock();
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
        } finally {
            firstLock.unlock();
        }
    }
}
