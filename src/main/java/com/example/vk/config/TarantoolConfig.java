package com.example.vk.config;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientConfig;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
import java.lang.reflect.Method;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class TarantoolConfig {

    @Value("${tarantool.host:localhost}")
    private String host;

    @Value("${tarantool.port:3301}")
    private int port;

    @Value("${tarantool.user:guest}")
    private String user;

    @Value("${tarantool.password:}")
    private String password;

    @Value("${tarantool.connect-timeout-ms:3000}")
    private int connectTimeout;

    @Value("${tarantool.read-timeout-ms:3000}")
    private int readTimeout;

    private TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client;

    @Bean
    @Lazy
    public TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient() {
        TarantoolClientConfig config = TarantoolClientConfig.builder()
                .withCredentials(new SimpleTarantoolCredentials(user, password))
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout)
                .build();

        // Используем Reflection для создания клиента из-за проблем с API
        try {
            Class<?> clientFactoryClass = Class.forName("io.tarantool.driver.core.TarantoolClientFactory");
            Object clientFactory = clientFactoryClass.getMethod("getInstance").invoke(null);
            Method createMethod = clientFactoryClass.getMethod("create", 
                    TarantoolClientConfig.class, io.tarantool.driver.api.TarantoolServerAddress.class);
            client = (TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>>) 
                    createMethod.invoke(clientFactory, config, new io.tarantool.driver.api.TarantoolServerAddress(host, port));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Tarantool client", e);
        }
        return client;
    }

    @Bean
    @Lazy
    public TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolSpaceOperations(
            TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client) {
        return client.space("VK");
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // Log but don't throw - cleanup best effort
            }
        }
    }
}
