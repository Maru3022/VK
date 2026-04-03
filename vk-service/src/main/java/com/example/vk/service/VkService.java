package com.example.vk.service;

import com.example.vk.cache.VkCacheService;
import com.example.vk.exception.VkException;
import com.example.vk.proto.VkPair;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VkService {

    private final VkCacheService cacheService;

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
        return cacheService.put(key, value);
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "get"})
    public Optional<byte[]> get(String key) {
        validateKey(key);
        Optional<byte[]> result = cacheService.get(key);
        if (result.isEmpty()) {
            throw new VkException("NOT_FOUND", "Key not found: " + key);
        }
        return result;
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "delete"})
    public boolean delete(String key) {
        validateKey(key);
        boolean deleted = cacheService.delete(key);
        if (!deleted) {
            throw new VkException("NOT_FOUND", "Key not found for delete: " + key);
        }
        return deleted;
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "count"})
    public long count() {
        return cacheService.count();
    }

    @Timed(value = "vk.request.duration", extraTags = {"method", "range"})
    public void range(String keySince, String keyTo, int pageSize, StreamObserver<VkPair> responseObserver) {
        cacheService.range(keySince, keyTo, pageSize, responseObserver);
    }
}
