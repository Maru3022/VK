package com.example.vk.metrics;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, byte[]> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, byte[]> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            String pong = redisTemplate.execute(connection -> connection.ping());
            if ("PONG".equals(pong)) {
                return Health.up().withDetail("redis", "PONG").build();
            }
            return Health.down().withDetail("redis", "Unexpected response: " + pong).build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
