package com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model;

public enum TransferResult {
    SUCCESS,
    ITEM_NOT_FOUND,
    TIMEOUT,
    INTERRUPTED
}
