package com.example.vk.config;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient() {
        return TarantoolClientFactory.createClient()
                .withAddress(host, port)
                .withCredentials(new SimpleTarantoolCredentials(user, password))
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout)
                .build();
    }

    @Bean
    public TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolSpaceOperations(
            TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client) {
        return client.space("VK");
    }
}
