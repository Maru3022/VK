package com.example.vk.exception;

public class VkException extends RuntimeException {
    private final String code;

    public VkException(String code, String message) {
        super(message);
        this.code = code;
    }

    public VkException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
