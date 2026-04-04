package com.example.vk.grpc;

import com.example.vk.exception.VkException;
import com.example.vk.proto.CountRequest;
import com.example.vk.proto.CountResponse;
import com.example.vk.proto.DeleteRequest;
import com.example.vk.proto.DeleteResponse;
import com.example.vk.proto.GetRequest;
import com.example.vk.proto.GetResponse;
import com.example.vk.proto.VkPair;
import com.example.vk.proto.VkServiceGrpc;
import com.example.vk.proto.PutRequest;
import com.example.vk.proto.PutResponse;
import com.example.vk.proto.RangeRequest;
import com.example.vk.repository.VkValue;
import com.example.vk.service.VkService;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class VkGrpcService extends VkServiceGrpc.VkServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(VkGrpcService.class);
    private final VkService vkService;

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        try {
            byte[] value = request.hasValue() ? request.getValue().getValue().toByteArray() : null;
            boolean updated = vkService.put(request.getKey(), value);
            responseObserver.onNext(PutResponse.newBuilder().setUpdated(updated).build());
            responseObserver.onCompleted();
        } catch (VkException e) {
            log.warn("Put error: {} - {}", e.getCode(), e.getMessage());
            responseObserver.onError(mapToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected put error", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        try {
            VkValue value = vkService.get(request.getKey());
            GetResponse.Builder builder = GetResponse.newBuilder()
                    .setKey(request.getKey())
                    .setFound(true);
            if (value.getData() != null) {
                builder.setValue(BytesValue.newBuilder().setValue(ByteString.copyFrom(value.getData())).build());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (VkException e) {
            log.warn("Get error: {} - {}", e.getCode(), e.getMessage());
            responseObserver.onError(mapToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected get error", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            vkService.delete(request.getKey());
            responseObserver.onNext(DeleteResponse.newBuilder().setDeleted(true).build());
            responseObserver.onCompleted();
        } catch (VkException e) {
            log.warn("Delete error: {} - {}", e.getCode(), e.getMessage());
            responseObserver.onError(mapToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected delete error", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    @Override
    public void count(CountRequest request, StreamObserver<CountResponse> responseObserver) {
        try {
            long count = vkService.count();
            responseObserver.onNext(CountResponse.newBuilder().setCount(count).build());
            responseObserver.onCompleted();
        } catch (VkException e) {
            log.warn("Count error: {} - {}", e.getCode(), e.getMessage());
            responseObserver.onError(mapToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected count error", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    @Override
    public void range(RangeRequest request, StreamObserver<VkPair> responseObserver) {
        try {
            vkService.range(request.getKeySince(), request.getKeyTo(), request.getPageSize(), responseObserver);
        } catch (VkException e) {
            log.warn("Range error: {} - {}", e.getCode(), e.getMessage());
            responseObserver.onError(mapToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected range error", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    private Status mapToGrpcStatus(VkException e) {
        return switch (e.getCode()) {
            case "INVALID_ARGUMENT" -> Status.INVALID_ARGUMENT.withDescription(e.getMessage());
            case "NOT_FOUND" -> Status.NOT_FOUND.withDescription(e.getMessage());
            case "UNAVAILABLE" -> Status.UNAVAILABLE.withDescription(e.getMessage());
            default -> Status.INTERNAL.withDescription(e.getMessage());
        };
    }
}
