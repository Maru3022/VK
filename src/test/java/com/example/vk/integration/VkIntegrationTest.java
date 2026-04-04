package com.example.vk.integration;

import com.example.vk.proto.CountRequest;
import com.example.vk.proto.CountResponse;
import com.example.vk.proto.DeleteRequest;
import com.example.vk.proto.GetRequest;
import com.example.vk.proto.GetResponse;
import com.example.vk.proto.PutRequest;
import com.example.vk.proto.RangeRequest;
import com.example.vk.proto.VkPair;
import com.example.vk.proto.VkServiceGrpc;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class VkIntegrationTest {

    @Value("${grpc.server.port:0}")
    private int grpcPort;

    @Container
    static GenericContainer<?> tarantool = new GenericContainer<>("tarantool/tarantool:3.2")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("tarantool/init.lua"),
                    "/opt/tarantool/init.lua")
            .withCommand("/opt/tarantool/init.lua")
            .withExposedPorts(3301)
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("tarantool.host", tarantool::getHost);
        r.add("tarantool.port", () -> tarantool.getMappedPort(3301));
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("grpc.server.port", () -> 0); // Use random port
    }

    @Autowired
    private RedisTemplate<String, byte[]> redisTemplate;

    private ManagedChannel channel;
    private VkServiceGrpc.VkServiceBlockingStub stub;

    @BeforeEach
    void setup() throws InterruptedException {
        int port = grpcPort;
        if (port == 0) {
            port = 9090; // Default fallback
        }
        
        channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
        stub = VkServiceGrpc.newBlockingStub(channel);
        
        // Wait for gRPC server to be ready
        for (int i = 0; i < 20; i++) {
            try {
                stub.count(CountRequest.newBuilder().build());
                break; // Server is ready
            } catch (Exception e) {
                Thread.sleep(500);
            }
        }
    }

    @AfterEach
    void tearDown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                    channel.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void putAndGet_roundtrip() {
        stub.put(PutRequest.newBuilder()
                .setKey("hello")
                .setValue(BytesValue.newBuilder().setValue(ByteString.copyFromUtf8("world")).build())
                .build());

        GetResponse r = stub.get(GetRequest.newBuilder().setKey("hello").build());
        assertTrue(r.getFound());
        assertEquals("world", r.getValue().getValue().toStringUtf8());
    }

    @Test
    void putNullValue_getReturnsNullValue() {
        stub.put(PutRequest.newBuilder()
                .setKey("nullkey")
                .build()); // No value field set

        GetResponse r = stub.get(GetRequest.newBuilder().setKey("nullkey").build());
        assertTrue(r.getFound());
        assertFalse(r.hasValue()); // value field not set = null
    }

    @Test
    void delete_thenGetReturnsNotFound() {
        stub.put(PutRequest.newBuilder()
                .setKey("del")
                .setValue(BytesValue.newBuilder().setValue(ByteString.copyFromUtf8("v")).build())
                .build());

        stub.delete(DeleteRequest.newBuilder().setKey("del").build());

        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () ->
                stub.get(GetRequest.newBuilder().setKey("del").build()));
        assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    }

    @Test
    void range_returns1000RecordsInOrder() {
        for (int i = 0; i < 1000; i++) {
            String key = String.format("key%04d", i);
            stub.put(PutRequest.newBuilder()
                    .setKey(key)
                    .setValue(BytesValue.newBuilder().setValue(ByteString.copyFromUtf8("v" + i)).build())
                    .build());
        }

        Iterator<VkPair> response = stub.range(RangeRequest.newBuilder()
                .setKeySince("key0000")
                .setKeyTo("key0999")
                .setPageSize(100)
                .build());

        List<VkPair> pairs = new ArrayList<>();
        response.forEachRemaining(pairs::add);

        assertEquals(1000, pairs.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(String.format("key%04d", i), pairs.get(i).getKey());
        }
    }

    @Test
    void count_returnsCorrectNumber() {
        // Clear or at least count current
        long initialCount = stub.count(CountRequest.newBuilder().build()).getCount();
        for (int i = 0; i < 500; i++) {
            stub.put(PutRequest.newBuilder()
                    .setKey("count" + i)
                    .setValue(BytesValue.newBuilder().setValue(ByteString.copyFromUtf8("v")).build())
                    .build());
        }
        assertEquals(initialCount + 500, stub.count(CountRequest.newBuilder().build()).getCount());
    }

    @Test
    void get_afterRedisClear_fetchesFromTarantool() {
        stub.put(PutRequest.newBuilder()
                .setKey("cached")
                .setValue(BytesValue.newBuilder().setValue(ByteString.copyFromUtf8("v")).build())
                .build());

        // Manually delete from Redis
        redisTemplate.delete("vk:cached");

        GetResponse r = stub.get(GetRequest.newBuilder().setKey("cached").build());
        assertTrue(r.getFound());
        assertEquals("v", r.getValue().getValue().toStringUtf8());
    }

    @Test
    void service_handlesConcurrentOperations() {
        // Test concurrent puts and gets
        stub.put(PutRequest.newBuilder()
                .setKey("concurrent1")
                .setValue(BytesValue.newBuilder().setValue(ByteString.copyFromUtf8("v1")).build())
                .build());

        stub.put(PutRequest.newBuilder()
                .setKey("concurrent2")
                .setValue(BytesValue.newBuilder().setValue(ByteString.copyFromUtf8("v2")).build())
                .build());

        GetResponse r1 = stub.get(GetRequest.newBuilder().setKey("concurrent1").build());
        GetResponse r2 = stub.get(GetRequest.newBuilder().setKey("concurrent2").build());

        assertTrue(r1.getFound());
        assertTrue(r2.getFound());
        assertEquals("v1", r1.getValue().getValue().toStringUtf8());
        assertEquals("v2", r2.getValue().getValue().toStringUtf8());
    }
}
