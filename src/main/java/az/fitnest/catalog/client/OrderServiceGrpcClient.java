package az.fitnest.catalog.client;

import az.fitnest.order.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceGrpcClient {

    @GrpcClient("order-backend")
    private SubscriptionPackageServiceGrpc.SubscriptionPackageServiceBlockingStub blockingStub;

    @GrpcClient("order-backend")
    private UserSubscriptionServiceGrpc.UserSubscriptionServiceBlockingStub userSubscriptionStub;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "orderService")
    public boolean checkPlanExists(Long packageId) {
        az.fitnest.order.grpc.CheckPlanExistsRequest request = az.fitnest.order.grpc.CheckPlanExistsRequest.newBuilder()
                .setPackageId(packageId)
                .build();
        az.fitnest.order.grpc.CheckPlanExistsResponse response = blockingStub
                .withDeadlineAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                .checkPlanExists(request);
        return response.getExists() && response.getIsActive();
    }

    public boolean checkPackageExists(Long packageId) {
        return checkPlanExists(packageId);
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "orderService")
    public List<az.fitnest.order.grpc.SubscriptionPackageInfo> getPlansByIds(List<Long> packageIds) {
        az.fitnest.order.grpc.GetPlansByIdsRequest request = az.fitnest.order.grpc.GetPlansByIdsRequest.newBuilder()
                .addAllPackageIds(packageIds)
                .build();
        az.fitnest.order.grpc.GetPlansByIdsResponse response = blockingStub
                .withDeadlineAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                .getPlansByIds(request);
        return response.getPackagesList();
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "orderService")
    public void checkIn(Long userId, Long gymId, boolean consumeFrozen) {
        az.fitnest.order.grpc.CheckInRequest request = az.fitnest.order.grpc.CheckInRequest.newBuilder()
                .setUserId(userId)
                .setGymId(gymId)
                .setConsumeFrozen(consumeFrozen)
                .build();

        az.fitnest.order.grpc.CheckInResponse response = blockingStub
                .withDeadlineAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                .checkIn(request);

        if (!response.getSuccess()) {
            throw new RuntimeException("error.rpc_failed");
        }
    }

    public List<az.fitnest.order.grpc.PackageNameInfo> getPackageNamesByIds(List<Long> packageIds) {
        az.fitnest.order.grpc.GetPackageNamesByIdsRequest request = az.fitnest.order.grpc.GetPackageNamesByIdsRequest.newBuilder()
                .addAllPackageIds(packageIds)
                .build();
        az.fitnest.order.grpc.GetPackageNamesByIdsResponse response = blockingStub.getPackageNamesByIds(request);
        return response.getPackagesList();
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "orderService")
    public List<az.fitnest.order.grpc.SubscriptionPackageInfo> getGymPlans() {
        az.fitnest.order.grpc.GetGymPlansRequest request = az.fitnest.order.grpc.GetGymPlansRequest.newBuilder().build();
        az.fitnest.order.grpc.GetGymPlansResponse response = blockingStub
                .withDeadlineAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                .getGymPlans(request);
        return response.getPackagesList();
    }

    public az.fitnest.order.grpc.ActiveSubscriptionResponse getActiveSubscription(Long userId) {
        az.fitnest.order.grpc.GetActiveSubscriptionRequest request = az.fitnest.order.grpc.GetActiveSubscriptionRequest.newBuilder()
                .setUserId(userId)
                .build();
        az.fitnest.order.grpc.ActiveSubscriptionResponse response = userSubscriptionStub.getActiveSubscription(request);
        return response;
    }

    public void freezeSession(Long userId) {
        az.fitnest.order.grpc.FreezeSessionRequest request = az.fitnest.order.grpc.FreezeSessionRequest.newBuilder()
                .setUserId(userId)
                .build();
        userSubscriptionStub.freezeSession(request);
    }

    public void restoreSession(Long userId) {
        az.fitnest.order.grpc.RestoreSessionRequest request = az.fitnest.order.grpc.RestoreSessionRequest.newBuilder()
                .setUserId(userId)
                .build();
        userSubscriptionStub.restoreSession(request);
    }

    public void consumeFrozenSession(Long userId) {
        az.fitnest.order.grpc.ConsumeFrozenSessionRequest request = az.fitnest.order.grpc.ConsumeFrozenSessionRequest.newBuilder()
                .setUserId(userId)
                .build();
        userSubscriptionStub.consumeFrozenSession(request);
    }
}
