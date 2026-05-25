package az.fitnest.catalog.client;

import az.fitnest.user.grpc.UserServiceGrpc;
import az.fitnest.user.grpc.GetUserByIdRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class UserServiceGrpcClient {
    @GrpcClient("user-backend")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @org.springframework.cache.annotation.Cacheable(value = "users", key = "#userId")
    public CachedUser getUserById(Long userId) {
        GetUserByIdRequest request = GetUserByIdRequest.newBuilder().setUserId(userId).build();
        return CachedUser.fromProto(userServiceStub.getUserById(request));
    }
}
