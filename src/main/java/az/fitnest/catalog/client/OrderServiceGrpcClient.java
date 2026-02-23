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

    public List<GymPlanItemDto> getGymPlans(Long gymId) {
        GetGymPlansRequest request = GetGymPlansRequest.newBuilder()
                .setGymId(gymId)
                .build();

        GetGymPlansResponse response = blockingStub.getGymPlans(request);

        List<GymPlanItemDto> result = new ArrayList<>();
        for (GymMembershipPlan plan : response.getPlansList()) {
            // Collect all unique services from all duration options as benefits
            List<String> benefits = new ArrayList<>(plan.getBenefitsList());
            if (benefits.isEmpty()) {
                benefits = plan.getOptionsList().stream()
                        .flatMap(opt -> opt.getServicesList().stream())
                        .distinct()
                        .collect(Collectors.toList());
            }

            result.add(GymPlanItemDto.builder()
                    .plan_id(String.valueOf(plan.getPlanId()))
                    .name(plan.getName())
                    .benefits(benefits)
                    .build());
        }
        return result;
    }
}
