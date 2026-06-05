package az.fitnest.catalog.grpc;

import az.fitnest.catalog.model.entity.GymAdmin;
import az.fitnest.catalog.repository.GymAdminRepository;
import az.fitnest.catalog.repository.GymAnalyticsRepository;
import az.fitnest.catalog.repository.GymAnalyticsRepository.PartnersKpiProjection;
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
        System.out.println("DEBUG: getGymAdminsByUsers called with userIds: " + userIds);
        
        // Print all gym admins in the database for diagnostics
        try {
            List<GymAdmin> dbAll = gymAdminRepository.findAll();
            System.out.println("DEBUG: [DIAGNOSTIC] Total gym admins in DB: " + dbAll.size());
            for (GymAdmin a : dbAll) {
                System.out.println("DEBUG: [DIAGNOSTIC] DB Admin: id=" + a.getId() + ", userId=" + a.getUserId() + ", name=" + a.getName() + " " + a.getSurname() + ", gym=" + (a.getGym() != null ? a.getGym().getName() : "null"));
            }
        } catch (Exception e) {
            System.err.println("DEBUG: failed to fetch all gym admins from database!");
            e.printStackTrace();
        }

        List<GymAdmin> admins = null;
        try {
            admins = gymAdminRepository.findAllByUserIdIn(userIds);
            System.out.println("DEBUG: found admins in db: " + (admins == null ? "null" : admins.size()));
            if (admins != null) {
                for (GymAdmin a : admins) {
                    System.out.println("DEBUG: admin userId=" + a.getUserId() + ", gym=" + (a.getGym() != null ? a.getGym().getName() : "null") + ", role=" + a.getRole());
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG: findAllByUserIdIn failed in catalog-backend!");
            e.printStackTrace();
        }

        List<GymAdminDetail> details = new java.util.ArrayList<>();
        if (admins != null) {
            details = admins.stream()
                    .map(admin -> GymAdminDetail.newBuilder()
                            .setUserId(admin.getUserId())
                            .setGymName(admin.getGym() != null ? admin.getGym().getName() : "")
                            .setRole(admin.getRole() != null ? admin.getRole() : "")
                            .build())
                    .collect(Collectors.toList());
        }

        responseObserver.onNext(
                GetGymAdminsByUsersResponse.newBuilder()
                        .addAllAdmins(details)
                        .build()
        );
        responseObserver.onCompleted();
    }
}
