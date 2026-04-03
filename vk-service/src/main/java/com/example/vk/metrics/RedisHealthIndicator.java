package com.example.vk.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, byte[]> redisTemplate;

    @Override
    public Health health() {
        try {
            redisTemplate.opsForValue().get("__health__");
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
