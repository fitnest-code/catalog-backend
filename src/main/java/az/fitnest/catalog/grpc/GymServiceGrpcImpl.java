package az.fitnest.catalog.grpc;

import az.fitnest.catalog.service.impl.GymImageService;
import az.fitnest.catalog.service.impl.GymReadService;
import az.fitnest.catalog.service.impl.GymReviewService;
import az.fitnest.catalog.service.impl.GymTrainerService;
import az.fitnest.catalog.service.impl.GymWriteService;
import io.grpc.stub.StreamObserver;

import java.util.List;

import net.devh.boot.grpc.server.service.GrpcService;
import az.fitnest.catalog.grpc.GymServiceGrpc;
import az.fitnest.catalog.grpc.GetGymDetailRequest;
import az.fitnest.catalog.grpc.GymDetailResponse;
import az.fitnest.catalog.grpc.Address;
import az.fitnest.catalog.grpc.PutGymImageRequest;
import az.fitnest.catalog.grpc.PutGymImageResponse;
import az.fitnest.catalog.grpc.GymImageItem;
import az.fitnest.catalog.grpc.GymWorkHour;
import az.fitnest.catalog.grpc.GetGymImagesRequest;
import az.fitnest.catalog.grpc.GymImageResponse;

import az.fitnest.catalog.grpc.GetTrainersRequest;
import az.fitnest.catalog.grpc.GymTrainersResponse;
import az.fitnest.catalog.grpc.GymTrainer;
import az.fitnest.catalog.grpc.GetReviewsRequest;
import az.fitnest.catalog.grpc.GymReviewsResponse;
import az.fitnest.catalog.grpc.GymReview;
import az.fitnest.catalog.grpc.GymReviewAuthor;
import az.fitnest.catalog.grpc.AddReviewRequest;
import az.fitnest.catalog.grpc.AddReviewResponse;
import az.fitnest.catalog.grpc.GetReservationRulesRequest;
import az.fitnest.catalog.grpc.ReservationRulesResponse;
import az.fitnest.catalog.grpc.GetMainPageGymsRequest;
import az.fitnest.catalog.grpc.GetMainPageGymsResponse;
import az.fitnest.catalog.grpc.GymMainPage;
import az.fitnest.catalog.grpc.AddTrainerRequest;
import az.fitnest.catalog.grpc.AddTrainerResponse;
import az.fitnest.catalog.grpc.UpdateTrainerRequest;
import az.fitnest.catalog.grpc.UpdateTrainerResponse;
import az.fitnest.catalog.grpc.DeleteTrainerRequest;
import az.fitnest.catalog.grpc.DeleteTrainerResponse;
import az.fitnest.catalog.grpc.CreateGymRequest;
import az.fitnest.catalog.grpc.CreateGymResponse;
import az.fitnest.catalog.grpc.UpdateGymRequest;
import az.fitnest.catalog.grpc.UpdateGymResponse;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class GymServiceGrpcImpl extends GymServiceGrpc.GymServiceImplBase {
    private final GymReadService gymReadService;
    private final GymWriteService gymWriteService;
    private final GymImageService gymImageService;
    private final GymReviewService gymReviewService;
    private final GymTrainerService gymTrainerService;

    @Autowired
    public GymServiceGrpcImpl(GymReadService gymReadService, GymWriteService gymWriteService, GymImageService gymImageService, GymReviewService gymReviewService, GymTrainerService gymTrainerService) {
        this.gymReadService = gymReadService;
        this.gymWriteService = gymWriteService;
        this.gymImageService = gymImageService;
        this.gymReviewService = gymReviewService;
        this.gymTrainerService = gymTrainerService;
    }

    @Override
    public void getGymDetail(GetGymDetailRequest request, StreamObserver<GymDetailResponse> responseObserver) {
        az.fitnest.catalog.dto.GymDetailResponse dto = gymReadService.getGymDetail(request.getUserId(), request.getGymId());

        GymDetailResponse.Builder protoBuilder = GymDetailResponse.newBuilder()
                .setGymId(dto.gym_id() != null ? dto.gym_id() : "")
                .setName(dto.name() != null ? dto.name() : "")
                .setDescription(dto.description() != null ? dto.description() : "")
                .setCoverImageUrl("")
                .setAddress(Address.newBuilder()
                        .setAddressText(dto.address() != null && dto.address().addressText() != null ? dto.address().addressText() : "")
                        .build())
                .setPhone(dto.phone() != null ? dto.phone() : "")
                .setEmail(dto.email() != null ? dto.email() : "")
                .setRating(0.0)
                .setReviewsCount(0)
                .setIsSaved(dto.isSaved() != null ? dto.isSaved() : false);

        if (dto.work_hours() != null) {
            for (az.fitnest.catalog.dto.GymWorkHourDto wh : dto.work_hours()) {
                protoBuilder.addWorkHours(GymWorkHour.newBuilder()
                        .setDay(wh.day() != null ? wh.day().name() : "")
                        .setFrom(wh.from() != null ? wh.from().toString() : "")
                        .setTo(wh.to() != null ? wh.to().toString() : "")
                        .build());
            }
        }

        GymDetailResponse protoResponse = protoBuilder.build();

        responseObserver.onNext(protoResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getGymImages(GetGymImagesRequest request, StreamObserver<GymImageResponse> responseObserver) {
        az.fitnest.catalog.dto.GymImageResponse dto = gymReadService.getGymImages(request.getGymId());
        GymImageResponse.Builder builder = GymImageResponse.newBuilder();
        if (dto.items() != null) {
            for (az.fitnest.catalog.dto.GymImageItemDto item : dto.items()) {
                builder.addItems(GymImageItem.newBuilder()
                        .setImageId(item.image_id() != null ? item.image_id() : "")
                        .setUrl(item.url())
                        .setType(item.type())
                        .setTitle(item.title())
                        .build());
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    public void getTrainers(GetTrainersRequest request, StreamObserver<GymTrainersResponse> responseObserver) {
        az.fitnest.catalog.dto.PaginatedResponse<az.fitnest.catalog.dto.GymTrainerDto> dto = gymTrainerService.getTrainers(request.getGymId(), request.getPage(), request.getPageSize());
        GymTrainersResponse.Builder builder = GymTrainersResponse.newBuilder();
        if (dto.items() != null) {
            for (az.fitnest.catalog.dto.GymTrainerDto item : dto.items()) {
                builder.addItems(GymTrainer.newBuilder()
                        .setTrainerId(item.trainer_id() != null ? item.trainer_id() : "")
                        .setName(item.name())
                        .setSurname(item.surname())
                        .setProfessionId(item.profession() != null ? item.profession().id() : 0)
                        .setProfessionName(item.profession() != null ? item.profession().name() : "")
                        .setPicture(item.picture() != null ? item.picture() : "")
                        .setPhone(item.phone() != null ? item.phone() : "")
                        .setEmail(item.email() != null ? item.email() : "")
                        .build());
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getReviews(GetReviewsRequest request, StreamObserver<GymReviewsResponse> responseObserver) {
        az.fitnest.catalog.dto.PaginatedResponse<az.fitnest.catalog.dto.GymReviewDto> dto = gymReviewService.getReviews(request.getGymId(), request.getPage(), request.getPageSize(), request.getSort());
        GymReviewsResponse.Builder builder = GymReviewsResponse.newBuilder();
        if (dto.items() != null) {
            for (az.fitnest.catalog.dto.GymReviewDto item : dto.items()) {
                GymReview.Builder reviewBuilder = GymReview.newBuilder()
                        .setReviewId(item.review_id() != null ? item.review_id() : "")
                        .setRating(item.rating() != null ? item.rating() : 0)
                        .setComment(item.comment() != null ? item.comment() : "")
                        .setCreatedAt(item.created_at() != null ? item.created_at().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME) : "");
                if (item.author() != null) {
                    reviewBuilder.setAuthor(GymReviewAuthor.newBuilder()
                            .setUserId(item.author().user_id())
                            .setFullName(item.author().full_name())
                            .setAvatarUrl(item.author().avatar_url() != null ? item.author().avatar_url() : "")
                            .build());
                }
                builder.addItems(reviewBuilder.build());
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void addReview(AddReviewRequest request, StreamObserver<AddReviewResponse> responseObserver) {
        az.fitnest.catalog.dto.ReviewRequest dto = az.fitnest.catalog.dto.ReviewRequest.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        gymReviewService.addReview(request.getUserId(), request.getGymId(), dto);
        responseObserver.onNext(AddReviewResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getReservationRules(GetReservationRulesRequest request, StreamObserver<ReservationRulesResponse> responseObserver) {
        ReservationRulesResponse.Builder builder = ReservationRulesResponse.newBuilder();
        // Placeholder as ReservationRules internal structure is opaque
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getMainPageGyms(GetMainPageGymsRequest request, StreamObserver<GetMainPageGymsResponse> responseObserver) {
        // Proto doesn't include q/pagination — use defaults and delegate to new service signature
        double lat = request.getLat();
        double lng = request.getLng();
        az.fitnest.catalog.dto.PaginatedResponse<az.fitnest.catalog.dto.GymMainPageDto> resp = gymReadService.getClosestGyms(null, 1, 10, lat == 0.0 ? null : lat, lng == 0.0 ? null : lng);
        GetMainPageGymsResponse.Builder builder = GetMainPageGymsResponse.newBuilder();
        if (resp.items() != null) {
            for (az.fitnest.catalog.dto.GymMainPageDto dto : resp.items()) {
                builder.addItems(GymMainPage.newBuilder()
                        .setGymId(dto.gymId())
                        .setName(dto.name())
                        .setImageUrl(dto.coverImageUrl() != null ? dto.coverImageUrl() : "")
                        .setStars(dto.stars())
                        .setIsNew(dto.isNew())
                        .setLocation(dto.location())
                        .setDistanceKm(dto.distanceKm() != null ? dto.distanceKm() : 0.0)
                        .build());
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void putGymImage(PutGymImageRequest request, StreamObserver<PutGymImageResponse> responseObserver) {
        az.fitnest.catalog.dto.GymImageDto dto = gymImageService.putGymImage(request.getGymId(), request.getImageName(), request.getUrl());
        GymImageItem item = GymImageItem.newBuilder()
                .setImageId(dto.id() != null ? dto.id().toString() : "")
                .setUrl(dto.url())
                .setTitle(dto.name())
                .build();
        responseObserver.onNext(PutGymImageResponse.newBuilder().setSuccess(true).setItem(item).build());
        responseObserver.onCompleted();
    }


    @Override
    public void addTrainer(AddTrainerRequest request, StreamObserver<AddTrainerResponse> responseObserver) {
        az.fitnest.catalog.dto.TrainerRequest trainerRequest = az.fitnest.catalog.dto.TrainerRequest.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .professionId(request.getProfessionId())
                .picture(request.getPicture())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        gymTrainerService.addTrainer(request.getGymId(), trainerRequest);
        responseObserver.onNext(AddTrainerResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateTrainer(UpdateTrainerRequest request, StreamObserver<UpdateTrainerResponse> responseObserver) {
        az.fitnest.catalog.dto.TrainerRequest trainerRequest = az.fitnest.catalog.dto.TrainerRequest.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .professionId(request.getProfessionId())
                .picture(request.getPicture())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        gymTrainerService.updateTrainer(request.getGymId(), request.getTrainerId(), trainerRequest);
        responseObserver.onNext(UpdateTrainerResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteTrainer(DeleteTrainerRequest request, StreamObserver<DeleteTrainerResponse> responseObserver) {
        gymTrainerService.deleteTrainer(request.getGymId(), request.getTrainerId());
        responseObserver.onNext(DeleteTrainerResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void createGym(CreateGymRequest request, StreamObserver<CreateGymResponse> responseObserver) {
        gymWriteService.createGym(mapToGymRequest(request));
        responseObserver.onNext(CreateGymResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateGym(UpdateGymRequest request, StreamObserver<UpdateGymResponse> responseObserver) {
        gymWriteService.updateGym(request.getGymId(), mapToGymRequest(request));
        responseObserver.onNext(UpdateGymResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    private az.fitnest.catalog.dto.GymRequest mapToGymRequest(CreateGymRequest request) {
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

        az.fitnest.catalog.dto.AddressDto addressDto = az.fitnest.catalog.dto.AddressDto.builder()
                .addressText(addrText)
                .latitude(lat == 0.0 ? null : lat)
                .longitude(lng == 0.0 ? null : lng)
                .build();

        return az.fitnest.catalog.dto.GymRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(addressDto)
                .phone(request.getPhone())
                .email(request.getEmail())
                .categoryIds(new java.util.HashSet<>(request.getCategoryIdsList().stream()
                        .map(Long::valueOf).collect(java.util.stream.Collectors.toList())))
                .build();
    }

    private az.fitnest.catalog.dto.GymRequest mapToGymRequest(UpdateGymRequest request) {
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

        az.fitnest.catalog.dto.AddressDto addressDto = az.fitnest.catalog.dto.AddressDto.builder()
                .addressText(addrText)
                .latitude(lat == 0.0 ? null : lat)
                .longitude(lng == 0.0 ? null : lng)
                .build();

        return az.fitnest.catalog.dto.GymRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(addressDto)
                .phone(request.getPhone())
                .email(request.getEmail())
                .categoryIds(new java.util.HashSet<>(request.getCategoryIdsList().stream()
                        .map(Long::valueOf).collect(java.util.stream.Collectors.toList())))
                .build();
    }
}
