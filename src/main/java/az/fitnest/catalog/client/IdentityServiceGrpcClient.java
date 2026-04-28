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

    public Long createGymAdmin(String name, String surname, String phone, String email, String password) {
        CreateGymAdminRequest request = CreateGymAdminRequest.newBuilder()
                .setName(name)
                .setSurname(surname)
                .setPhoneNumber(phone)
                .setEmail(email)
                .setPassword(password)
                .build();
        return identityServiceBlockingStub.createGymAdmin(request).getUserId();
    }
}
