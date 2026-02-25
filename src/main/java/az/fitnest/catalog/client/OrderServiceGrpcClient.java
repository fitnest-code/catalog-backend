package az.fitnest.catalog.client;

import az.fitnest.catalog.dto.GymPlanItemDto;
import az.fitnest.order.grpc.GetGymPlansRequest;
import az.fitnest.order.grpc.GetGymPlansResponse;
import az.fitnest.order.grpc.GymMembershipPlan;
import az.fitnest.order.grpc.MembershipPlanServiceGrpc;
import az.fitnest.order.grpc.PlanDurationOption;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceGrpcClient {

    @GrpcClient("order-service")
    private MembershipPlanServiceGrpc.MembershipPlanServiceBlockingStub blockingStub;

    public boolean checkPlanExists(Long planId) {
        az.fitnest.order.grpc.CheckPlanExistsRequest request = az.fitnest.order.grpc.CheckPlanExistsRequest.newBuilder()
                .setPlanId(planId)
                .build();
        az.fitnest.order.grpc.CheckPlanExistsResponse response = blockingStub.checkPlanExists(request);
        return response.getExists() && response.getIsActive();
    }

    public List<az.fitnest.order.grpc.GymMembershipPlan> getPlansByIds(List<Long> planIds) {
        az.fitnest.order.grpc.GetPlansByIdsRequest request = az.fitnest.order.grpc.GetPlansByIdsRequest.newBuilder()
                .addAllPlanIds(planIds)
                .build();
        az.fitnest.order.grpc.GetPlansByIdsResponse response = blockingStub.getPlansByIds(request);
        return response.getPlansList();
    }

    public void checkIn(Long userId, Long gymId) {
        az.fitnest.order.grpc.CheckInRequest request = az.fitnest.order.grpc.CheckInRequest.newBuilder()
                .setUserId(userId)
                .setGymId(gymId)
                .build();

        az.fitnest.order.grpc.CheckInResponse response = blockingStub.checkIn(request);

        if (!response.getSuccess()) {
            throw new RuntimeException("Check-in failed: " + response.getMessage());
        }
    }
}
