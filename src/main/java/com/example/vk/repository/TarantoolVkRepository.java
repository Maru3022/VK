package com.example.vk.repository;

import com.example.vk.exception.VkException;
import com.example.vk.proto.VkPair;
import com.google.protobuf.BytesValue;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.conditions.Conditions;
import io.tarantool.driver.api.tuple.DefaultTarantoolTupleFactory;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.TarantoolTupleFactory;
import io.tarantool.driver.mappers.DefaultMessagePackMapper;
import io.tarantool.driver.mappers.MessagePackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class TarantoolVkRepository {

    private static final Logger log = LoggerFactory.getLogger(TarantoolVkRepository.class);
    private final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client;
    private final TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> space;
    private final TarantoolTupleFactory tupleFactory;

    public TarantoolVkRepository(@Lazy TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client,
                                 @Lazy TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> space) {
        this.client = client;
        this.space = space;
        MessagePackMapper mapper = new DefaultMessagePackMapper();
        this.tupleFactory = new DefaultTarantoolTupleFactory(mapper);
    }

    public boolean put(String key, byte[] value) {
        log.debug("Tarantool put: key={}", key);
        try {
            VkValue existing = get(key);
            TarantoolTuple tuple = tupleFactory.create(List.of(key, value != null ? value : null));
            space.replace(tuple).get();
            return existing.isExists();
        } catch (InterruptedException | ExecutionException e) {
            throw new VkException("UNAVAILABLE", "Tarantool replace failed", e);
        }
    }

    public VkValue get(String key) {
        log.debug("Tarantool get: key={}", key);
        try {
            TarantoolResult<TarantoolTuple> result = space.select(
                    Conditions.indexEquals("primary", List.of(key))
            ).get();
            if (result.isEmpty()) {
                return VkValue.notFound();
            }
            TarantoolTuple tuple = result.get(0);
            byte[] val = tuple.getObject(1, byte[].class).orElse(null);
            return VkValue.found(val);
        } catch (InterruptedException | ExecutionException e) {
            throw new VkException("UNAVAILABLE", "Tarantool select failed", e);
        }
    }

    public boolean delete(String key) {
        log.debug("Tarantool delete: key={}", key);
        try {
            TarantoolResult<TarantoolTuple> result = space.delete(
                    Conditions.indexEquals("primary", List.of(key))
            ).get();
            return !result.isEmpty();
        } catch (InterruptedException | ExecutionException e) {
            throw new VkException("UNAVAILABLE", "Tarantool delete failed", e);
        }
    }

    public long count() {
        log.debug("Tarantool count");
        try {
            List<?> result = client.eval("return box.space.VK:len()").get();
            if (result != null && !result.isEmpty()) {
                return ((Number) result.get(0)).longValue();
            }
            return 0L;
        } catch (InterruptedException | ExecutionException e) {
            throw new VkException("UNAVAILABLE", "Tarantool eval failed", e);
        }
    }

    public void range(String keySince, String keyTo, int pageSize, StreamObserver<VkPair> observer) {
        log.debug("Tarantool range: since={}, to={}, pageSize={}", keySince, keyTo, pageSize);
        try {
            String currentKey = keySince;
            while (true) {
                TarantoolResult<TarantoolTuple> batch = space.select(
                        Conditions.indexGreaterOrEquals("primary", List.of(currentKey))
                                .withLimit(pageSize)
                ).get();

                if (batch.isEmpty()) {
                    break;
                }

                String lastKey = null;
                for (TarantoolTuple tuple : batch) {
                    String key = tuple.getString(0);
                    if (key == null) {
                        continue;
                    }
                    if (keyTo != null && !keyTo.isEmpty() && key.compareTo(keyTo) > 0) {
                        observer.onCompleted();
                        return;
                    }

                    byte[] val = tuple.getObject(1, byte[].class).orElse(null);
                    VkPair.Builder builder = VkPair.newBuilder().setKey(key);
                    if (val != null) {
                        builder.setValue(BytesValue.newBuilder().setValue(ByteString.copyFrom(val)).build());
                    }
                    observer.onNext(builder.build());
                    lastKey = key;
                }

                if (batch.size() < pageSize) {
                    break;
                }

                currentKey = lastKey + "\0";
            }
            observer.onCompleted();
        } catch (Exception e) {
            log.error("Tarantool range error", e);
            observer.onError(new VkException("INTERNAL", "Range operation failed", e));
        }
    }
}
