package com.vsvidinsky.patterns.multiresourcelocking.model;

public enum TransferResult {
    SUCCESS,
    ITEM_NOT_FOUND,
    TIMEOUT,
    INTERRUPTED
}
