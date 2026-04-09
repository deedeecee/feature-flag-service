package com.debankar.featureflags.analytics;

import com.debankar.featureflags.sse.SseFlagBroadcaster;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final SseFlagBroadcaster sseBroadcaster;

    private Timer evaluationTimer;
    private Counter cacheMissCounter;
    private Counter cacheHitCounter;

    public MetricsService(MeterRegistry meterRegistry,
                          SseFlagBroadcaster sseBroadcaster) {
        this.meterRegistry = meterRegistry;
        this.sseBroadcaster = sseBroadcaster;
    }

    @PostConstruct
    public void init() {
        evaluationTimer = Timer.builder("feature_flag.evaluation.latency")
                .description("Time taken to evaluate a feature flag")
                .register(meterRegistry);

        cacheMissCounter = Counter.builder("feature_flag.cache.miss")
                .description("Number of Redis cache misses")
                .register(meterRegistry);

        cacheHitCounter = Counter.builder("feature_flag.cache.hit")
                .description("Number of Redis cache hits")
                .register(meterRegistry);

        meterRegistry.gauge(
                "feature_flag.sse.connections.active",
                sseBroadcaster,
                broadcaster -> broadcaster.getActiveConnectionCount());
    }

    public void recordEvaluation(long durationNanos) {
        evaluationTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordCacheMiss() {
        cacheMissCounter.increment();
    }

    public void recordCacheHit() {
        cacheHitCounter.increment();
    }
}
