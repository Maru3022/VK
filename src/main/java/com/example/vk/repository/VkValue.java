package com.example.vk.repository;

import java.util.Arrays;
import java.util.Objects;

public class VkValue {
    private final byte[] data;
    private final boolean exists;

    public VkValue(byte[] data, boolean exists) {
        this.data = data;
        this.exists = exists;
    }

    public static VkValue found(byte[] data) {
        return new VkValue(data, true);
    }

    public static VkValue notFound() {
        return new VkValue(null, false);
    }

    public byte[] getData() {
        return data;
    }

    public boolean isExists() {
        return exists;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VkValue vkValue = (VkValue) o;
        return exists == vkValue.exists && Arrays.equals(data, vkValue.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(exists);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "VkValue{" +
                "data=" + (data == null ? "null" : "[" + data.length + " bytes]") +
                ", exists=" + exists +
                '}';
    }
}
