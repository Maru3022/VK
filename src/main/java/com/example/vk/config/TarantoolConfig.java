package com.example.vk.config;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientConfig;
import io.tarantool.driver.api.TarantoolClusterAddressProvider;
import io.tarantool.driver.api.TarantoolServerAddress;
import io.tarantool.driver.api.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
import io.tarantool.driver.core.DefaultTarantoolClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

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

    @Value("${tarantool.pool.min-connections:2}")
    private int minConnections;

    @Value("${tarantool.pool.max-connections:10}")
    private int maxConnections;

    @Bean
    public TarantoolClient<TarantoolTuple> tarantoolClient() {
        TarantoolClientConfig config = TarantoolClientConfig.builder()
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout)
                .withConnections(minConnections)
                .withMaxConnections(maxConnections)
                .withCredentials(new SimpleTarantoolCredentials(user, password))
                .build();

        TarantoolClusterAddressProvider addressProvider = () -> 
                Collections.singletonList(new TarantoolServerAddress(host, port));

        return new DefaultTarantoolClientFactory()
                .createClient(config, addressProvider);
    }

    @Bean
    public TarantoolSpaceOperations<TarantoolTuple> tarantoolSpaceOperations(TarantoolClient<TarantoolTuple> client) {
        return client.space("VK");
    }
}
