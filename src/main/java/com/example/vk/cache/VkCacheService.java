package com.example.vk.cache;

import com.example.vk.metrics.VkMetrics;
import com.example.vk.proto.VkPair;
import com.example.vk.repository.TarantoolVkRepository;
import com.example.vk.repository.VkValue;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VkCacheService {

    private static final Logger log = LoggerFactory.getLogger(VkCacheService.class);
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final TarantoolVkRepository repository;
    private final VkMetrics metrics;

    @Value("${redis.ttl-seconds:60}")
    private long ttl;

    private static final byte[] NULL_SENTINEL = {0x00};

    public VkValue get(String key) {
        String cacheKey = "vk:" + key;
        metrics.recordCacheTotal();
        try {
            byte[] cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                metrics.recordCacheHit();
                if (Arrays.equals(cached, NULL_SENTINEL)) {
                    return VkValue.found(null);
                }
                return VkValue.found(cached);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for get, fallback: {}", e.getMessage());
        }

        VkValue result = repository.get(key);
        if (result.isExists()) {
            try {
                byte[] toCache = result.getData() == null ? NULL_SENTINEL : result.getData();
                redisTemplate.opsForValue().set(cacheKey, toCache, ttl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis write failed: {}", e.getMessage());
            }
        }
        return result;
    }

    public boolean put(String key, byte[] value) {
        boolean updated = repository.put(key, value);
        try {
            redisTemplate.delete("vk:" + key);
        } catch (Exception e) {
            log.warn("Redis evict failed: {}", e.getMessage());
        }
        return updated;
    }

    public boolean delete(String key) {
        boolean deleted = repository.delete(key);
        try {
            redisTemplate.delete("vk:" + key);
        } catch (Exception e) {
            log.warn("Redis evict failed: {}", e.getMessage());
        }
        return deleted;
    }

    public long count() {
        return repository.count();
    }

    public void range(String keySince, String keyTo, int pageSize, StreamObserver<VkPair> responseObserver) {
        repository.range(keySince, keyTo, pageSize, responseObserver);
    }
}
