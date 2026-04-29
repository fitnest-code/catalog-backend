package az.fitnest.catalog.mapper;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.grpc.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GymProtoMapper {
    public static az.fitnest.catalog.grpc.GymDetailResponse toProto(az.fitnest.catalog.dto.GymDetailResponse dto) {
        az.fitnest.catalog.grpc.GymDetailResponse.Builder builder = az.fitnest.catalog.grpc.GymDetailResponse.newBuilder()
                .setGymId(dto.gym_id() != null ? dto.gym_id() : "")
                .setName(dto.name() != null ? dto.name() : "")
                .setDescription(dto.description() != null ? dto.description() : "")
                .setCoverImageUrl(dto.coverImageUrl() != null ? dto.coverImageUrl() : "")
                .setPhone(dto.phone() != null ? dto.phone() : "")
                .setEmail(dto.email() != null ? dto.email() : "")
                .setRating(dto.rating() != null ? dto.rating() : 0.0)
                .setReviewsCount(dto.reviewsCount() != null ? dto.reviewsCount() : 0)
                .setIsSaved(dto.isSaved() != null ? dto.isSaved() : false);
        if (dto.address() != null) {
            builder.setAddress(az.fitnest.catalog.grpc.Address.newBuilder()
                    .setAddressText(dto.address().addressText() != null ? dto.address().addressText() : "")
                    .build());
        }
        if (dto.general_work_hours() != null) {
            for (az.fitnest.catalog.dto.GymWorkHourDto wh : dto.general_work_hours()) {
                builder.addWorkHours(az.fitnest.catalog.grpc.GymWorkHour.newBuilder()
                        .setDay(wh.period() != null ? wh.period() : "")
                        .setFrom(wh.from() != null ? wh.from().toString() : "")
                        .setTo(wh.to() != null ? wh.to().toString() : "")
                        .build());
            }
        }
        return builder.build();
    }

    public static az.fitnest.catalog.grpc.GymImageResponse toProto(az.fitnest.catalog.dto.GymImageResponse dto) {
        az.fitnest.catalog.grpc.GymImageResponse.Builder builder = az.fitnest.catalog.grpc.GymImageResponse.newBuilder();
        if (dto.items() != null) {
            for (az.fitnest.catalog.dto.GymImageItemDto item : dto.items()) {
                builder.addItems(az.fitnest.catalog.grpc.GymImageItem.newBuilder()
                        .setImageId(item.image_id() != null ? item.image_id() : "")
                        .setUrl(item.url())
                        .setType(item.type())
                        .setTitle(item.title())
                        .build());
            }
        }
        return builder.build();
    }

    public static GymRequest mapToGymRequest(CreateGymRequest request) {
        String addrText = null;
        Double lat = null;
        Double lng = null;
        if (request.hasAddress()) {
            addrText = request.getAddress().getAddressText();
            lat = request.getAddress().getLatitude();
            lng = request.getAddress().getLongitude();
            if ((lat == 0.0 && lng == 0.0) && addrText != null && addrText.contains(",")) {
                String[] parts = addrText.split(",");
                if (parts.length >= 2) {
                    try {
                        lat = Double.parseDouble(parts[0].trim());
                        lng = Double.parseDouble(parts[1].trim());
                        addrText = null;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        AddressDto addressDto = AddressDto.builder()
                .addressText(addrText)
                .latitude(lat == 0.0 ? null : lat)
                .longitude(lng == 0.0 ? null : lng)
                .build();
        Set<Long> categoryIds = request.getCategoryIdsList().stream().map(Long::valueOf).collect(Collectors.toSet());
        return mapToGymRequest(request.getName(), request.getDescription(), addressDto, request.getPhone(), request.getEmail(), categoryIds);
    }

    public static GymRequest mapToGymRequest(UpdateGymRequest request) {
        String addrText = null;
        Double lat = null;
        Double lng = null;
        if (request.hasAddress()) {
            addrText = request.getAddress().getAddressText();
            lat = request.getAddress().getLatitude();
            lng = request.getAddress().getLongitude();
            if ((lat == 0.0 && lng == 0.0) && addrText != null && addrText.contains(",")) {
                String[] parts = addrText.split(",");
                if (parts.length >= 2) {
                    try {
                        lat = Double.parseDouble(parts[0].trim());
                        lng = Double.parseDouble(parts[1].trim());
                        addrText = null;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        AddressDto addressDto = AddressDto.builder()
                .addressText(addrText)
                .latitude(lat == 0.0 ? null : lat)
                .longitude(lng == 0.0 ? null : lng)
                .build();
        Set<Long> categoryIds = request.getCategoryIdsList().stream().map(Long::valueOf).collect(Collectors.toSet());
        return mapToGymRequest(request.getName(), request.getDescription(), addressDto, request.getPhone(), request.getEmail(), categoryIds);
    }

    public static GymRequest mapToGymRequest(
        String name,
        String description,
        AddressDto address,
        String phone,
        String email,
        Set<Long> categoryIds
    ) {
        return GymRequest.builder()
            .name(name)
            .description(description)
            .address(address)
            .phone(phone)
            .email(email)
            .categoryIds(categoryIds)
            .build();
    }

}
