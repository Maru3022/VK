package com.example.vk.service;

import com.example.vk.cache.VkCacheService;
import com.example.vk.exception.VkException;
import com.example.vk.metrics.VkMetrics;
import com.example.vk.proto.VkPair;
import com.example.vk.repository.VkValue;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VkService {

    private static final Logger log = LoggerFactory.getLogger(VkService.class);
    private final VkCacheService cacheService;
    private final VkMetrics metrics;

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new VkException("INVALID_ARGUMENT", "key is blank");
        }
        if (key.length() > 256) {
            throw new VkException("INVALID_ARGUMENT", "key too long");
        }
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "put"})
    public boolean put(String key, byte[] value) {
        validateKey(key);
        log.debug("Put request: key={}", key);
        boolean success = cacheService.put(key, value);
        metrics.recordRequest("put", success);
        return success;
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "get"})
    public VkValue get(String key) {
        validateKey(key);
        log.debug("Get request: key={}", key);
        VkValue result = cacheService.get(key);
        boolean success = result.isExists();
        metrics.recordRequest("get", success);
        if (!success) {
            throw new VkException("NOT_FOUND", "Key not found: " + key);
        }
        return result;
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "delete"})
    public boolean delete(String key) {
        validateKey(key);
        log.debug("Delete request: key={}", key);
        boolean deleted = cacheService.delete(key);
        metrics.recordRequest("delete", deleted);
        if (!deleted) {
            throw new VkException("NOT_FOUND", "Key not found for delete: " + key);
        }
        return deleted;
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "count"})
    public long count() {
        log.debug("Count request");
        long count = cacheService.count();
        metrics.recordRequest("count", true);
        return count;
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "range"})
    public void range(String keySince, String keyTo, int pageSize, StreamObserver<VkPair> responseObserver) {
        log.debug("Range request: since={}, to={}, pageSize={}", keySince, keyTo, pageSize);
        try {
            cacheService.range(keySince, keyTo, pageSize, responseObserver);
            metrics.recordRequest("range", true);
        } catch (Exception e) {
            metrics.recordRequest("range", false);
            throw e;
        }
    }
}
