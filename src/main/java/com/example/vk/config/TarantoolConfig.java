package com.example.vk.config;

import io.tarantool.driver.api.TarantoolClient;
<<<<<<< HEAD
import io.tarantool.driver.api.TarantoolClientConfig;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolClusterAddressProvider;
=======
import io.tarantool.driver.api.TarantoolResult;
>>>>>>> c91dbd91ac5f056538cb581f14520676e83be7f6
import io.tarantool.driver.api.TarantoolServerAddress;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.TarantoolClientBuilder;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
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

    @Bean
    public TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient() {
        return TarantoolClientBuilder.create()
                .withAddress(host, port)
                .withCredentials(new SimpleTarantoolCredentials(user, password))
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout)
<<<<<<< HEAD
                .withCredentials(new SimpleTarantoolCredentials(user, password))
                .build();

        TarantoolClusterAddressProvider addressProvider = () ->
                Collections.singletonList(new TarantoolServerAddress(host, port));

        return TarantoolClientFactory.createClient(config, addressProvider);
=======
                .withConnections(minConnections)
                .build();
>>>>>>> c91dbd91ac5f056538cb581f14520676e83be7f6
    }

    @Bean
    public TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolSpaceOperations(
            TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client) {
        return client.space("VK");
    }
}
