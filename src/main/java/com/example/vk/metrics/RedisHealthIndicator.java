package com.example.vk.metrics;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
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
            RedisCallback<String> pingCallback = (RedisCallback<String>) connection -> {
                String pong = connection.ping();
                return pong;
            };
            String pong = redisTemplate.execute(pingCallback);
            if ("PONG".equals(pong)) {
                return Health.up().withDetail("redis", "PONG").build();
            }
            return Health.down().withDetail("redis", "Unexpected response: " + pong).build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
