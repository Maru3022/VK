package com.example.vk.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class VkMetrics {

    private final MeterRegistry registry;
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheTotal = new AtomicLong(0);

    public VkMetrics(MeterRegistry registry) {
        this.registry = registry;

        Gauge.builder("vk.cache.hit.ratio", () -> {
            long total = cacheTotal.get();
            return total == 0 ? 0.0 : (double) cacheHits.get() / total;
        }).register(registry);
    }

    public void recordRequest(String method, boolean success) {
        Counter.builder("vk_requests_total")
                .tag("method", method)
                .tag("status", success ? "success" : "error")
                .register(registry)
                .increment();
    }

    public void recordDuration(String method, Runnable block) {
        Timer timer = Timer.builder("vk.request.duration")
                .tag("method", method)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
        timer.record(block);
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheTotal() {
        cacheTotal.incrementAndGet();
    }

    public void recordRangeSize(long count) {
        DistributionSummary.builder("vk.range.records.returned")
                .register(registry)
                .record(count);
    }
}
