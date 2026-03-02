package com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.benchmark;

import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.LogisticsCenter;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.Warehouse;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.BackoffConfig;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.model.TransferResult;
import com.vsvidinsky.concurrencynotes.patterns.multiresourcelocking.solution.LockOrderingLogisticsCenter;
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
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 10)
@Fork(3)
@Threads(16)
public class WorkTimeBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private static final int WAREHOUSE_COUNT = 16;
        private static final long OPERATION_TIMEOUT_MS = 500;

        @Param({"1", "2", "4", "8"})
        public long simulationWorkTimeMs;

        public Warehouse[] warehouses;
        public final Item item = new Item("benchmark-item");
        public LogisticsCenter lockOrdering;
        public LogisticsCenter polledWithBackoff;

        @Setup
        public void setup() {
            warehouses = new Warehouse[WAREHOUSE_COUNT];
            for (int i = 0; i < WAREHOUSE_COUNT; i++) {
                warehouses[i] = new BenchmarkWarehouse(String.valueOf((char) ('A' + i)), simulationWorkTimeMs);
            }
            BackoffConfig backoffConfig = new BackoffConfig(
                    OPERATION_TIMEOUT_MS,
                    simulationWorkTimeMs / 2,
                    simulationWorkTimeMs * 2
            );
            lockOrdering = new LockOrderingLogisticsCenter();
            polledWithBackoff = new PolledWithBackoffLogisticsCenter(backoffConfig);
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {
        public int sourceIndex;

        @Setup
        public void setup(BenchmarkState state) {
            sourceIndex = ThreadLocalRandom.current().nextInt(state.warehouses.length);
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

    private static TransferResult transfer(LogisticsCenter center, BenchmarkState state, ThreadState thread) {
        int dest;
        do {
            dest = ThreadLocalRandom.current().nextInt(state.warehouses.length);
        } while (dest == thread.sourceIndex);

        TransferResult result = center.transfer(state.warehouses[thread.sourceIndex], state.warehouses[dest], state.item);
        thread.sourceIndex = dest;
        return result;
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(WorkTimeBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
