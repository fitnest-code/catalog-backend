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

    @GrpcClient("identity-service")
    private az.fitnest.user.grpc.UserServiceGrpc.UserServiceBlockingStub identityUserServiceBlockingStub;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "identityService")
    public void updateUserProfile(Long userId, String name, String surname, String email, String mobile) {
        az.fitnest.user.grpc.UpdateUserProfileRequest request = az.fitnest.user.grpc.UpdateUserProfileRequest.newBuilder()
                .setUserId(userId)
                .setFirstName(name)
                .setLastName(surname)
                .setEmail(email != null ? email : "")
                .setMobile(mobile != null ? mobile : "")
                .build();
        identityUserServiceBlockingStub
                .withDeadlineAfter(10, java.util.concurrent.TimeUnit.SECONDS)
                .updateUserProfile(request);
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "identityService")
    public void deactivateUser(Long userId, String reason) {
        az.fitnest.user.grpc.DeactivateUserRequest request = az.fitnest.user.grpc.DeactivateUserRequest.newBuilder()
                .setUserId(userId)
                .setReason(reason != null ? reason : "")
                .build();
        identityUserServiceBlockingStub
                .withDeadlineAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                .deactivateUser(request);
    }

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
