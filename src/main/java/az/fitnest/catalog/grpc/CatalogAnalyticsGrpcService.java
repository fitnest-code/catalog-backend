package az.fitnest.catalog.grpc;

import az.fitnest.catalog.repository.GymAnalyticsRepository;
import az.fitnest.catalog.repository.GymAnalyticsRepository.PartnersKpiProjection;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * @author: nijataghayev
 */

@GrpcService
@RequiredArgsConstructor
public class CatalogAnalyticsGrpcService extends GymServiceGrpc.GymServiceImplBase {

    private final GymAnalyticsRepository gymAnalyticsRepository;

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
}
