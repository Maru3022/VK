package com.example.vk.config;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
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
        client = TarantoolClientFactory.createClient()
                .withAddress(host, port)
                .withCredentials(new SimpleTarantoolCredentials(user, password))
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout)
                .build();
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
