package com.vsvidinsky.patterns.multiresourcelocking;

import com.vsvidinsky.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.patterns.multiresourcelocking.model.TransferResult;

public interface LogisticsCenter {
    TransferResult transfer(Warehouse source, Warehouse destination, Item item);
}
