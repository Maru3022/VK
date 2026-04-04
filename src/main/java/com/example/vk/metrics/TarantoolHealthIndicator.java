package com.example.vk.metrics;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class TarantoolHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(TarantoolHealthIndicator.class);
    private final ObjectProvider<TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>>> clientProvider;

    public TarantoolHealthIndicator(@Lazy ObjectProvider<TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>>> clientProvider) {
        this.clientProvider = clientProvider;
    }

    @Override
    public Health health() {
        try {
            TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client = clientProvider.getIfAvailable();
            if (client == null) {
                return Health.unknown().withDetail("database", "Tarantool client not available").build();
            }
            client.eval("return 1").get();
            return Health.up().withDetail("database", "Tarantool").build();
        } catch (Exception e) {
            log.error("Tarantool health check failed", e);
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
