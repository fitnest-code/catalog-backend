package az.fitnest.catalog.util;

import az.fitnest.catalog.client.UserServiceGrpcClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class UserContextInitializer {

    private final UserServiceGrpcClient userServiceGrpcClient;

    public UserContextInitializer(UserServiceGrpcClient userServiceGrpcClient) {
        this.userServiceGrpcClient = userServiceGrpcClient;
    }

    @PostConstruct
    public void init() {
        UserContext.setUserServiceGrpcClient(userServiceGrpcClient);
    }
}
