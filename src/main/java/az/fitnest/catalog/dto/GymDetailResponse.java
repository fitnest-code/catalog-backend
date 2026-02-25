/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymPlanItemDto;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.GymRoomDto;
import az.fitnest.catalog.dto.GymTrainerDto;
import az.fitnest.catalog.dto.GymWorkHourDto;
import az.fitnest.catalog.dto.LocationDto;
import java.util.List;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor

public class GymDetailResponse {
    private String gym_id;
    private String name;
    private String description;
    private LocationDto address;
    private Boolean isSaved;
    private String phone;
    private String email;
    private List<GymWorkHourDto> work_hours;
    private List<GymRoomDto> rooms;
    private List<GymPlanItemDto> membership_plans;
    private List<GymTrainerDto> trainers;
    private List<GymReviewDto> recent_reviews;
    private String qr_code_url;


}

