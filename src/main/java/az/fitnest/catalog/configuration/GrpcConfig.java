/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package az.fitnest.catalog.configuration;

import az.fitnest.catalog.grpc.GymServiceGrpcImpl;
import az.fitnest.catalog.service.impl.GymImageService;
import az.fitnest.catalog.service.impl.GymReadService;
import az.fitnest.catalog.service.impl.GymReviewService;
import az.fitnest.catalog.service.impl.GymTrainerService;
import az.fitnest.catalog.service.impl.GymWriteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {
    @Bean
    public GymServiceGrpcImpl gymServiceGrpcImpl(GymReadService gymReadService, GymWriteService gymWriteService, GymImageService gymImageService, GymReviewService gymReviewService, GymTrainerService gymTrainerService) {
        return new GymServiceGrpcImpl(gymReadService, gymWriteService, gymImageService, gymReviewService, gymTrainerService);
    }
}

