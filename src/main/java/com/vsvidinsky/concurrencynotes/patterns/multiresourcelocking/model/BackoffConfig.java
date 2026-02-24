package com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model;

public record BackoffConfig(long timeoutMs, long initialBackoffMs, long maxBackoffMs) {
}
