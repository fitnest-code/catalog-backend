package az.fitnest.catalog.client;

import az.fitnest.user.grpc.UserServiceGrpc;
import az.fitnest.user.grpc.GetUserByIdRequest;
import az.fitnest.user.grpc.UserResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class UserServiceGrpcClient {
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public UserResponse getUserById(Long userId) {
        GetUserByIdRequest request = GetUserByIdRequest.newBuilder().setUserId(userId).build();
        return userServiceStub.getUserById(request);
    }
}
