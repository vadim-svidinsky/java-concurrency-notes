package com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.benchmark;

import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.LogisticsCenter;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.Warehouse;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.BackoffConfig;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.TransferResult;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.solution.LockOrderingLogisticsCenter;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.solution.OrderedPolledWithBackoffLogisticsCenter;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.solution.PolledWithBackoffLogisticsCenter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 5, time = 15)
@Fork(3)
@Threads(16)
public class TransferBenchmark {

    // Shared between all threads: warehouses and the logistics center implementations
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        // Time spent doing actual warehouse work (add/remove) inside the critical section
        private static final long SIMULATION_WORK_TIME_MS = 5;
        // Max time a transfer waits for locks before returning TIMEOUT
        private static final long OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
        // Starting backoff delay when tryLock fails
        private static final long INITIAL_BACKOFF_MS = 1;
        // Upper cap for exponential backoff
        private static final long MAX_BACKOFF_MS = 9;

        @Param({"4", "8", "16", "32", "64"})
        public int warehouseCount;

        public Warehouse[] warehouses;
        public final Item item = new Item("benchmark-item");
        public final BackoffConfig backoffConfig = new BackoffConfig(OPERATION_TIMEOUT_MS, INITIAL_BACKOFF_MS, MAX_BACKOFF_MS);
        public final LogisticsCenter lockOrdering = new LockOrderingLogisticsCenter();
        public final LogisticsCenter polledWithBackoff = new PolledWithBackoffLogisticsCenter(backoffConfig);
        public final LogisticsCenter orderedPolledWithBackoff = new OrderedPolledWithBackoffLogisticsCenter(backoffConfig);

        @Setup
        public void setup() {
            warehouses = new Warehouse[warehouseCount];
            for (int i = 0; i < warehouseCount; i++) {
                warehouses[i] = new BenchmarkWarehouse(String.valueOf((char) ('A' + i)), SIMULATION_WORK_TIME_MS);
            }
        }
    }

    // Per-thread: tracks which warehouse this thread will transfer from next
    @State(Scope.Thread)
    public static class ThreadState {

        public int sourceIndex;

        @Setup
        public void setup(BenchmarkState state) {
            sourceIndex = ThreadLocalRandom.current().nextInt(state.warehouseCount);
        }
    }

    @Benchmark
    public TransferResult lockOrdering(BenchmarkState state, ThreadState thread) {
        return transfer(state.lockOrdering, state, thread);
    }

    @Benchmark
    public TransferResult polledWithBackoff(BenchmarkState state, ThreadState thread) {
        return transfer(state.polledWithBackoff, state, thread);
    }

    // Hot warehouse benchmarks: warehouses[0] is hot, involved in ~80% of transfers
    @Benchmark
    public TransferResult lockOrderingHot(BenchmarkState state) {
        return transferHot(state.lockOrdering, state);
    }

    @Benchmark
    public TransferResult polledWithBackoffHot(BenchmarkState state) {
        return transferHot(state.polledWithBackoff, state);
    }

    @Benchmark
    public TransferResult orderedPolledWithBackoff(BenchmarkState state, ThreadState thread) {
        return transfer(state.orderedPolledWithBackoff, state, thread);
    }

    @Benchmark
    public TransferResult orderedPolledWithBackoffHot(BenchmarkState state) {
        return transferHot(state.orderedPolledWithBackoff, state);
    }

    private static TransferResult transfer(LogisticsCenter center, BenchmarkState state, ThreadState thread) {
        int dest;
        do {
            dest = ThreadLocalRandom.current().nextInt(state.warehouses.length);
        } while (dest == thread.sourceIndex);

        TransferResult result = center.transfer(state.warehouses[thread.sourceIndex], state.warehouses[dest], state.item);
        thread.sourceIndex = dest;
        return result;
    }

    private static TransferResult transferHot(LogisticsCenter center, BenchmarkState state) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int n = state.warehouses.length;
        int src, dst;

        if (n == 2 || rng.nextDouble() < 0.8) {
            // hot warehouse (index 0) is source or destination with equal probability
            if (rng.nextBoolean()) {
                src = 0;
                dst = 1 + rng.nextInt(n - 1);
            } else {
                src = 1 + rng.nextInt(n - 1);
                dst = 0;
            }
        } else {
            // remaining 20%: two random non-hot warehouses
            src = 1 + rng.nextInt(n - 1);
            do {
                dst = 1 + rng.nextInt(n - 1);
            } while (dst == src);
        }

        return center.transfer(state.warehouses[src], state.warehouses[dst], state.item);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(TransferBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
