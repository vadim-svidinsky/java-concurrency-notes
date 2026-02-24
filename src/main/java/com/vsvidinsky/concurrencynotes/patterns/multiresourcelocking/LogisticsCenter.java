package com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking;

import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.TransferResult;

public interface LogisticsCenter {
    TransferResult transfer(Warehouse source, Warehouse destination, Item item);
}
