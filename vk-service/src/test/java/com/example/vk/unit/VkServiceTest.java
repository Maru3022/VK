package com.example.vk.unit;

import com.example.vk.cache.VkCacheService;
import com.example.vk.exception.VkException;
import com.example.vk.service.VkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VkServiceTest {

    @Mock
    private VkCacheService cacheService;

    @InjectMocks
    private VkService vkService;

    private final byte[] value = "test-value".getBytes();

    @Test
    void put_newKey_returnsFalse() {
        when(cacheService.put("k", value)).thenReturn(false);
        assertFalse(vkService.put("k", value));
    }

    @Test
    void put_existingKey_returnsTrue() {
        when(cacheService.put("k", value)).thenReturn(true);
        assertTrue(vkService.put("k", value));
    }

    @Test
    void put_nullValue_noException() {
        assertDoesNotThrow(() -> vkService.put("k", null));
    }

    @Test
    void get_existingKey_returnsValue() {
        when(cacheService.get("k")).thenReturn(Optional.of(value));
        Optional<byte[]> result = vkService.get("k");
        assertTrue(result.isPresent());
        assertArrayEquals(value, result.get());
    }

    @Test
    void get_missingKey_throwsNotFound() {
        when(cacheService.get("k")).thenReturn(Optional.empty());
        VkException ex = assertThrows(VkException.class, () -> vkService.get("k"));
        assertEquals("NOT_FOUND", ex.getCode());
    }

    @Test
    void get_nullValue_returnsNull() {
        when(cacheService.get("k")).thenReturn(Optional.of(null));
        Optional<byte[]> result = vkService.get("k");
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    void delete_existingKey_returnsTrue() {
        when(cacheService.delete("k")).thenReturn(true);
        assertTrue(vkService.delete("k"));
    }

    @Test
    void delete_missingKey_throwsNotFound() {
        when(cacheService.delete("k")).thenReturn(false);
        VkException ex = assertThrows(VkException.class, () -> vkService.delete("k"));
        assertEquals("NOT_FOUND", ex.getCode());
    }

    @Test
    void put_emptyKey_throwsInvalidArgument() {
        VkException ex = assertThrows(VkException.class, () -> vkService.put("", value));
        assertEquals("INVALID_ARGUMENT", ex.getCode());
    }

    @Test
    void put_keyTooLong_throwsInvalidArgument() {
        String longKey = "a".repeat(257);
        VkException ex = assertThrows(VkException.class, () -> vkService.put(longKey, value));
        assertEquals("INVALID_ARGUMENT", ex.getCode());
    }

    @Test
    void count_returnsLong() {
        when(cacheService.count()).thenReturn(42L);
        assertEquals(42L, vkService.count());
    }
}
