package com.example.vk.repository;

import lombok.Value;

@Value
public class VkValue {
    byte[] data;
    boolean exists;

    public static VkValue found(byte[] data) {
        return new VkValue(data, true);
    }

    public static VkValue notFound() {
        return new VkValue(null, false);
    }
}
