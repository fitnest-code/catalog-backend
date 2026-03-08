package az.fitnest.catalog.client;

import az.fitnest.order.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceGrpcClient {

    @GrpcClient("order-service")
    private SubscriptionPackageServiceGrpc.SubscriptionPackageServiceBlockingStub blockingStub;

    public boolean checkPlanExists(Long packageId) {
        az.fitnest.order.grpc.CheckPlanExistsRequest request = az.fitnest.order.grpc.CheckPlanExistsRequest.newBuilder()
                .setPackageId(packageId)
                .build();
        az.fitnest.order.grpc.CheckPlanExistsResponse response = blockingStub.checkPlanExists(request);
        return response.getExists() && response.getIsActive();
    }

    public List<az.fitnest.order.grpc.SubscriptionPackageInfo> getPlansByIds(List<Long> packageIds) {
        az.fitnest.order.grpc.GetPlansByIdsRequest request = az.fitnest.order.grpc.GetPlansByIdsRequest.newBuilder()
                .addAllPackageIds(packageIds)
                .build();
        az.fitnest.order.grpc.GetPlansByIdsResponse response = blockingStub.getPlansByIds(request);
        return response.getPackagesList();
    }

    public void checkIn(Long userId, Long gymId) {
        az.fitnest.order.grpc.CheckInRequest request = az.fitnest.order.grpc.CheckInRequest.newBuilder()
                .setUserId(userId)
                .setGymId(gymId)
                .build();

        az.fitnest.order.grpc.CheckInResponse response = blockingStub.checkIn(request);

        if (!response.getSuccess()) {
            throw new RuntimeException("error.rpc_failed");
        }
    }
}
