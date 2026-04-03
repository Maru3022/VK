package com.example.vk.metrics;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TarantoolHealthIndicator implements HealthIndicator {

    private final TarantoolClient<TarantoolTuple> client;

    @Override
    public Health health() {
        try {
            client.eval("return 1").get();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
