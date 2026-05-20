package az.fitnest.catalog.client;

import az.fitnest.identity.grpc.CreateGymAdminRequest;
import az.fitnest.identity.grpc.IdentityServiceGrpc;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityServiceGrpcClient {

    @GrpcClient("identity-service")
    private IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceBlockingStub;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "identityService")
    public Long createGymAdmin(String name, String surname, String phone, String email, String password) {
        CreateGymAdminRequest request = CreateGymAdminRequest.newBuilder()
                .setName(name)
                .setSurname(surname)
                .setPhoneNumber(phone)
                .setEmail(email != null ? email : "")
                .setPassword(password)
                .build();
        return identityServiceBlockingStub
                .withDeadlineAfter(10, java.util.concurrent.TimeUnit.SECONDS)
                .createGymAdmin(request).getUserId();
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "identityService")
    public az.fitnest.identity.grpc.CheckUserExistsResponse checkUserExists(String email, String phone) {
        az.fitnest.identity.grpc.CheckUserExistsRequest request = az.fitnest.identity.grpc.CheckUserExistsRequest.newBuilder()
                .setEmail(email != null ? email : "")
                .setPhoneNumber(phone != null ? phone : "")
                .build();
        return identityServiceBlockingStub
                .withDeadlineAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                .checkUserExists(request);
    }
}
