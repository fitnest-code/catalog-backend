/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.google.protobuf.Empty
 *  io.grpc.stub.StreamObserver
 *  net.devh.boot.grpc.server.service.GrpcService
 *  org.springframework.transaction.annotation.Transactional
 */
package az.fitnest.catalog.grpc;

import az.fitnest.catalog.dto.StoreDetailResponseDto;
import az.fitnest.catalog.dto.StoreRequest;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.grpc.CreateStoreAdminRequest;
import az.fitnest.catalog.grpc.DeleteStoreAdminRequest;
import az.fitnest.catalog.grpc.GetStoresRequest;
import az.fitnest.catalog.grpc.StoreDetailResponse;
import az.fitnest.catalog.grpc.StoreListResponse;
import az.fitnest.catalog.grpc.StoreServiceGrpc;
import az.fitnest.catalog.grpc.ToggleFavoriteRequest;
import az.fitnest.catalog.grpc.ToggleFavoriteResponse;
import az.fitnest.catalog.grpc.UpdateStoreAdminRequest;
import az.fitnest.catalog.service.StoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

@GrpcService
public class StoreGrpcServiceImpl
        extends StoreServiceGrpc.StoreServiceImplBase {
    private final StoreService storeService;
    private final ObjectMapper objectMapper;

    public StoreGrpcServiceImpl(StoreService storeService, ObjectMapper objectMapper) {
        this.storeService = storeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void getMarketStores(GetStoresRequest request, StreamObserver<StoreListResponse> responseObserver) {
        try {
            Long userId = request.getUserId() > 0L ? Long.valueOf(request.getUserId()) : null;
            String query = request.getQuery() != null && !request.getQuery().isBlank() ? request.getQuery() : null;
            int page = request.getPage() > 0 ? request.getPage() : 1;
            int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
            PaginatedResponse<az.fitnest.catalog.dto.StoreMainPageDto> response = this.storeService.getStores(userId, query, "ALL", null, null, page, pageSize);
            String json = this.objectMapper.writeValueAsString(response);
            responseObserver.onNext(StoreListResponse.newBuilder().setJsonPayload(json).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    @Transactional
    public void toggleFavorite(ToggleFavoriteRequest request, StreamObserver<ToggleFavoriteResponse> responseObserver) {
        try {
            Long storeId = Long.parseLong(request.getStoreId());
            Long userId = request.getUserId();
            this.storeService.toggleSave(userId, storeId);
            responseObserver.onNext(ToggleFavoriteResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    @Transactional
    public void createStoreAdmin(CreateStoreAdminRequest request, StreamObserver<StoreDetailResponse> responseObserver) {
        try {
            StoreRequest storeReq = (StoreRequest) this.objectMapper.readValue(request.getJsonPayload(), StoreRequest.class);
            StoreDetailResponseDto responseDto = this.storeService.createStore(storeReq);
            String json = this.objectMapper.writeValueAsString(responseDto);
            responseObserver.onNext(StoreDetailResponse.newBuilder().setJsonPayload(json).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    @Transactional
    public void updateStoreAdmin(UpdateStoreAdminRequest request, StreamObserver<StoreDetailResponse> responseObserver) {
        try {
            Long storeId = Long.parseLong(request.getStoreId());
            StoreRequest storeReq = (StoreRequest) this.objectMapper.readValue(request.getJsonPayload(), StoreRequest.class);
            StoreDetailResponseDto responseDto = this.storeService.updateStore(storeId, storeReq);
            String json = this.objectMapper.writeValueAsString(responseDto);
            responseObserver.onNext(StoreDetailResponse.newBuilder().setJsonPayload(json).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    @Transactional
    public void deleteStoreAdmin(DeleteStoreAdminRequest request, StreamObserver<Empty> responseObserver) {
        try {
            Long storeId = Long.parseLong(request.getStoreId());
            this.storeService.deleteStore(storeId);
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}

