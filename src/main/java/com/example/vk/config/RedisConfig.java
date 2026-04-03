package com.example.vk.config;

import com.example.vk.cache.ByteArrayRedisSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.lettuce.pool.max-active:20}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.min-idle:2}")
    private int minIdle;

    @Value("${redis.ttl-seconds:60}")
    private long ttlSeconds;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);
        if (password != null && !password.isEmpty()) {
            redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
        }

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMinIdle(minIdle);

        LettucePoolingClientConfiguration lettucePoolingClientConfiguration = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofSeconds(1))
                .build();

        return new LettuceConnectionFactory(redisStandaloneConfiguration, lettucePoolingClientConfiguration);
    }

    @Bean
    public RedisTemplate<String, byte[]> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new ByteArrayRedisSerializer());
        return template;
    }
}
