package az.fitnest.catalog.grpc;

import az.fitnest.catalog.model.entity.GymAdmin;
import az.fitnest.catalog.repository.GymAdminRepository;
import az.fitnest.catalog.repository.GymAnalyticsRepository;
import az.fitnest.catalog.repository.GymAnalyticsRepository.PartnersKpiProjection;
import az.fitnest.catalog.repository.GymRepository;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * @author: nijataghayev
 */

@GrpcService
@RequiredArgsConstructor
public class CatalogAnalyticsGrpcService extends GymServiceGrpc.GymServiceImplBase {

    private final GymAnalyticsRepository gymAnalyticsRepository;
    private final GymAdminRepository gymAdminRepository;
    private final GymRepository gymRepository;
    private final az.fitnest.catalog.repository.ReservationRepository reservationRepository;
    private final az.fitnest.catalog.service.homegym.HomeGymRuleService homeGymRuleService;

    @Override
    public void getHomeGymInfo(GetHomeGymInfoRequest request,
                               StreamObserver<GetHomeGymInfoResponse> responseObserver) {
        try {
            var gym = gymRepository.findWithDetailsById(request.getGymId()).orElse(null);
            if (gym == null) {
                responseObserver.onNext(GetHomeGymInfoResponse.newBuilder().setFound(false).build());
                responseObserver.onCompleted();
                return;
            }
            Long packageId = request.getPackageId() > 0 ? request.getPackageId() : null;
            var packageContext = homeGymRuleService.packageContext(packageId);
            boolean supportsPackage = homeGymRuleService.supportsPackage(gym, packageContext);
            boolean homeEligible = homeGymRuleService.isHomeEligible(gym, packageId);
            String availableHours = homeGymRuleService.availableHours(gym, packageContext);
            responseObserver.onNext(GetHomeGymInfoResponse.newBuilder()
                    .setFound(true)
                    .setName(gym.getName() != null ? gym.getName() : "")
                    .setSupportsPackage(supportsPackage)
                    .setHomeEligible(homeEligible)
                    .setAvailableHours(availableHours != null ? availableHours : "")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to load home-gym info: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void hasEligibleHomeGym(HasEligibleHomeGymRequest request,
                                   StreamObserver<HasEligibleHomeGymResponse> responseObserver) {
        try {
            Long packageId = request.getPackageId() > 0 ? request.getPackageId() : null;
            List<Long> packageIds = homeGymRuleService.eligiblePackageIds(packageId);
            // The reminder is only useful when the package has at least one selectable gym
            // (R1: master switch on and package-level price override not disabled).
            var ids = gymRepository.findHomeGymIds(null, packageIds,
                    org.springframework.data.domain.PageRequest.of(0, 200)).getContent();
            boolean eligible = false;
            for (Long gymId : ids) {
                var gym = gymRepository.findWithDetailsById(gymId).orElse(null);
                if (gym != null && homeGymRuleService.isHomeEligible(gym, packageId)) {
                    eligible = true;
                    break;
                }
            }
            responseObserver.onNext(HasEligibleHomeGymResponse.newBuilder()
                    .setEligible(eligible)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to check eligible home gyms: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getActivePartnersKpi(
            GetActivePartnersKpiRequest request,
            StreamObserver<ActivePartnersKpiResponse> responseObserver
    ) {
        PartnersKpiProjection kpi = gymAnalyticsRepository.getActivePartnersKpi();

        responseObserver.onNext(
                ActivePartnersKpiResponse.newBuilder()
                        .setTotalActivePartners(kpi.getTotalActivePartners())
                        .setPercentageChange(kpi.getPercentageChange())
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getGymAdminsByUsers(
            GetGymAdminsByUsersRequest request,
            StreamObserver<GetGymAdminsByUsersResponse> responseObserver
    ) {
        List<Long> userIds = request.getUserIdsList();
        List<String> phoneNumbers = request.getPhoneNumbersList();
        List<String> emails = request.getEmailsList();

        List<Long> queryUserIds = userIds.isEmpty() ? List.of(-1L) : userIds;
        List<String> queryPhones = phoneNumbers.isEmpty() ? List.of("") : phoneNumbers;
        List<String> queryEmails = emails.isEmpty() ? List.of("") : emails;

        List<GymAdmin> admins = gymAdminRepository.findAllByUserIdInOrPhoneNumberInOrEmailIn(queryUserIds, queryPhones, queryEmails);

        List<GymAdminDetail> details = admins.stream()
                .map(admin -> GymAdminDetail.newBuilder()
                        .setUserId(admin.getUserId() != null ? admin.getUserId() : 0L)
                        .setGymName(admin.getGym() != null ? admin.getGym().getName() : "")
                        .setRole(admin.getRole() != null ? admin.getRole() : "")
                        .setPhoneNumber(admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "")
                        .setEmail(admin.getEmail() != null ? admin.getEmail() : "")
                        .build())
                .collect(Collectors.toList());

        responseObserver.onNext(
                GetGymAdminsByUsersResponse.newBuilder()
                        .addAllAdmins(details)
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void countGymsByPackage(
            CountGymsByPackageRequest request,
            StreamObserver<CountGymsByPackageResponse> responseObserver
    ) {
        long count = request.getPackageId() == 0
                ? 0
                : gymRepository.countActiveGymsByPackageId(request.getPackageId());
        responseObserver.onNext(
                CountGymsByPackageResponse.newBuilder()
                        .setGymCount(count)
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void countGymsByPackages(
            CountGymsByPackagesRequest request,
            StreamObserver<CountGymsByPackagesResponse> responseObserver
    ) {
        java.util.Map<Long, Long> byPackageId = new java.util.HashMap<>();
        for (Object[] row : gymRepository.countGymsBySubscriptionPackageId()) {
            if (row == null || row[0] == null || row[1] == null) {
                continue;
            }
            long packageId = ((Number) row[0]).longValue();
            long gymCount = ((Number) row[1]).longValue();
            byPackageId.put(packageId, gymCount);
        }

        CountGymsByPackagesResponse.Builder response = CountGymsByPackagesResponse.newBuilder();
        if (request.getPackageIdsCount() == 0) {
            byPackageId.forEach(response::putGymCounts);
        } else {
            for (long packageId : request.getPackageIdsList()) {
                response.putGymCounts(packageId, byPackageId.getOrDefault(packageId, 0L));
            }
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void hasActiveReservations(
            HasActiveReservationsRequest request,
            StreamObserver<HasActiveReservationsResponse> responseObserver
    ) {
        boolean hasActive = false;
        try {
            Long userId = request.getUserId();
            java.time.LocalDateTime windowStart = java.time.LocalDateTime.now();
            java.time.LocalDateTime windowEnd = windowStart.plusDays(30);

            if (!request.getStartTime().isEmpty()) {
                try {
                    windowStart = java.time.LocalDateTime.parse(request.getStartTime());
                } catch (Exception ignored) {
                    // keep default now
                }
            }
            if (!request.getEndTime().isEmpty()) {
                try {
                    windowEnd = java.time.LocalDateTime.parse(request.getEndTime());
                } catch (Exception ignored) {
                    // keep default +30d
                }
            }
            if (windowEnd.isBefore(windowStart)) {
                windowEnd = windowStart.plusDays(30);
            }

            java.time.LocalDate fromDate = windowStart.toLocalDate();
            java.time.LocalTime fromTime = windowStart.toLocalTime();
            java.time.LocalDate toDate = windowEnd.toLocalDate();

            // Confirmed bookings block freeze; PENDING included until BA says otherwise
            java.util.List<az.fitnest.catalog.model.enums.ReservationStatus> activeStatuses = java.util.List.of(
                    az.fitnest.catalog.model.enums.ReservationStatus.APPROVED,
                    az.fitnest.catalog.model.enums.ReservationStatus.PENDING
            );
            hasActive = reservationRepository.hasUpcomingActiveReservations(userId, fromDate, fromTime, toDate, activeStatuses);
        } catch (Exception e) {
            // Fail closed toward order: report conflict when check cannot run
            hasActive = true;
        }

        responseObserver.onNext(
                HasActiveReservationsResponse.newBuilder()
                        .setHasActiveReservations(hasActive)
                        .build()
        );
        responseObserver.onCompleted();
    }
}
