/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package az.fitnest.catalog.configuration;

import az.fitnest.catalog.grpc.GymServiceGrpcImpl;
import az.fitnest.catalog.service.GymService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {
    @Bean
    public GymServiceGrpcImpl gymServiceGrpcImpl(GymService gymService) {
        return new GymServiceGrpcImpl(gymService);
    }
}

