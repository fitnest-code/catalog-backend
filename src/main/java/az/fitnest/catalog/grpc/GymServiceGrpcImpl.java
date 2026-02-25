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
import az.fitnest.catalog.grpc.GetGymPackagesRequest;
import az.fitnest.catalog.grpc.GymPackagesResponse;
import az.fitnest.catalog.grpc.GymPlanItem;
import az.fitnest.catalog.grpc.GetPackageIncludesRequest;
import az.fitnest.catalog.grpc.GymPackageIncludesResponse;
import az.fitnest.catalog.grpc.GymPlanBenefit;
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
        
        // Build proto response while avoiding fields removed from the internal DTO.
        // For removed/missing fields we provide safe defaults so the proto contract is satisfied.
        GymDetailResponse.Builder protoBuilder = GymDetailResponse.newBuilder()
                .setGymId(dto.getGym_id() != null ? dto.getGym_id() : "")
                .setName(dto.getName() != null ? dto.getName() : "")
                .setDescription(dto.getDescription() != null ? dto.getDescription() : "")
                .setStatus(dto.getStatus() != null ? dto.getStatus() : "")
                // cover_image_url was removed from DTO; return empty string as default
                .setCoverImageUrl("")
                // Only return resolved address text for clients; do not expose lat/lng here
                .setAddress(Address.newBuilder()
                        .setAddressText(dto.getAddress() != null ? dto.getAddress() : "")
                        .build())
                .setPhone(dto.getPhone() != null ? dto.getPhone() : "")
                .setEmail(dto.getEmail() != null ? dto.getEmail() : "")
                // rating, reviews_count, is_saved were removed from DTO; default values
                .setRating(0.0)
                .setReviewsCount(0)
                .setIsSaved(false);

        // Map work_hours if present
        if (dto.getWork_hours() != null) {
            for (az.fitnest.catalog.dto.GymWorkHourDto wh : dto.getWork_hours()) {
                protoBuilder.addWorkHours(GymWorkHour.newBuilder()
                        .setDay(wh.getDay() != null ? wh.getDay().name() : "")
                        .setFrom(wh.getFrom() != null ? wh.getFrom().toString() : "")
                        .setTo(wh.getTo() != null ? wh.getTo().toString() : "")
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
        if (dto.getItems() != null) {
            for (az.fitnest.catalog.dto.GymImageItemDto item : dto.getItems()) {
                builder.addItems(GymImageItem.newBuilder()
                        .setImageId(item.getImage_id() != null ? item.getImage_id() : "")
                        .setUrl(item.getUrl())
                        .setType(item.getType())
                        .setTitle(item.getTitle())
                        .build());
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getGymPackages(GetGymPackagesRequest request, StreamObserver<GymPackagesResponse> responseObserver) {
        az.fitnest.catalog.dto.GymPackagesResponse dto = gymReadService.getGymPackages(request.getGymId());
        GymPackagesResponse.Builder builder = GymPackagesResponse.newBuilder();
        if (dto.getItems() != null) {
            for (az.fitnest.catalog.dto.GymPlanItemDto item : dto.getItems()) {
                GymPlanItem.Builder itemBuilder = GymPlanItem.newBuilder()
                        .setPlanId(item.getPlan_id() != null ? item.getPlan_id() : "")
                        .setName(item.getName() != null ? item.getName() : "");
                if (item.getBenefits() != null) {
                    for (String benefit : item.getBenefits()) {
                        itemBuilder.addBenefits(benefit != null ? benefit : "");
                    }
                }
                builder.addItems(itemBuilder.build());
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPackageIncludes(GetPackageIncludesRequest request, StreamObserver<GymPackageIncludesResponse> responseObserver) {
        az.fitnest.catalog.dto.GymPackageIncludesResponse dto = gymReadService.getPackageIncludes(request.getGymId(), request.getPackageId());
        GymPackageIncludesResponse.Builder builder = GymPackageIncludesResponse.newBuilder();
        builder.setPlanId(dto.getPlan_id() != null ? dto.getPlan_id() : "");
        if (dto.getItems() != null) {
            for (az.fitnest.catalog.dto.GymPlanBenefitDto item : dto.getItems()) {
                builder.addItems(GymPlanBenefit.newBuilder()
                        .setLogo(item.getLogo())
                        .setDescription(item.getDescription())
                        .build());
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTrainers(GetTrainersRequest request, StreamObserver<GymTrainersResponse> responseObserver) {
        az.fitnest.catalog.dto.GymTrainersResponse dto = gymTrainerService.getTrainers(request.getGymId(), request.getPage(), request.getPageSize());
        GymTrainersResponse.Builder builder = GymTrainersResponse.newBuilder();
        if (dto.getItems() != null) {
            for (az.fitnest.catalog.dto.GymTrainerDto item : dto.getItems()) {
                builder.addItems(GymTrainer.newBuilder()
                        .setTrainerId(item.getTrainer_id() != null ? item.getTrainer_id() : "")
                        .setFullName(item.getFull_name())
                        .setSpecialization(item.getSpecialization())
                        .setImageUrl(item.getImage_url() != null ? item.getImage_url() : "")
                        .build());
            }
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getReviews(GetReviewsRequest request, StreamObserver<GymReviewsResponse> responseObserver) {
        az.fitnest.catalog.dto.GymReviewsResponse dto = gymReviewService.getReviews(request.getGymId(), request.getPage(), request.getPageSize(), request.getSort());
        GymReviewsResponse.Builder builder = GymReviewsResponse.newBuilder();
        if (dto.getItems() != null) {
            for (az.fitnest.catalog.dto.GymReviewDto item : dto.getItems()) {
                GymReview.Builder reviewBuilder = GymReview.newBuilder()
                        .setReviewId(item.getReview_id() != null ? item.getReview_id() : "")
                        .setRating(item.getRating() != null ? item.getRating() : 0)
                        .setComment(item.getComment() != null ? item.getComment() : "")
                        .setCreatedAt(item.getCreated_at() != null ? item.getCreated_at().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME) : "");
                if (item.getAuthor() != null) {
                    reviewBuilder.setAuthor(GymReviewAuthor.newBuilder()
                            .setUserId(item.getAuthor().getUser_id())
                            .setFullName(item.getAuthor().getFull_name())
                            .setAvatarUrl(item.getAuthor().getAvatar_url() != null ? item.getAuthor().getAvatar_url() : "")
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
        az.fitnest.catalog.dto.ReviewRequest dto = new az.fitnest.catalog.dto.ReviewRequest();
        dto.setRating(request.getRating());
        dto.setComment(request.getComment());
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
        List<az.fitnest.catalog.dto.GymMainPageDto> dtos = gymReadService.getClosestGyms(null, 1, 10, lat == 0.0 ? null : lat, lng == 0.0 ? null : lng);
        GetMainPageGymsResponse.Builder builder = GetMainPageGymsResponse.newBuilder();
        for (az.fitnest.catalog.dto.GymMainPageDto dto : dtos) {
            builder.addItems(GymMainPage.newBuilder()
                    .setGymId(dto.getGymId())
                    .setName(dto.getName())
                    .setImageUrl(dto.getImageUrl() != null ? dto.getImageUrl() : "")
                    .setStars(dto.getStars())
                    .setIsNew(dto.isNew())
                    .setLocation(dto.getLocation())
                    .setDistanceKm(dto.getDistanceKm() != null ? dto.getDistanceKm() : 0.0)
                    .build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void putGymImage(PutGymImageRequest request, StreamObserver<PutGymImageResponse> responseObserver) {
        az.fitnest.catalog.dto.GymImageDto dto = gymImageService.putGymImage(request.getGymId(), request.getImageName(), request.getUrl());
        GymImageItem item = GymImageItem.newBuilder()
                .setImageId(dto.getId() != null ? dto.getId().toString() : "")
                .setUrl(dto.getUrl())
                .setTitle(dto.getName())
                .build();
        responseObserver.onNext(PutGymImageResponse.newBuilder().setSuccess(true).setItem(item).build());
        responseObserver.onCompleted();
    }



    @Override
    public void addTrainer(AddTrainerRequest request, StreamObserver<AddTrainerResponse> responseObserver) {
        az.fitnest.catalog.dto.TrainerRequest trainerRequest = new az.fitnest.catalog.dto.TrainerRequest();
        trainerRequest.setFullName(request.getFullName());
        trainerRequest.setSpecialization(request.getSpecialization());
        trainerRequest.setImageUrl(request.getImageUrl());
        
        gymTrainerService.addTrainer(request.getGymId(), trainerRequest);
        responseObserver.onNext(AddTrainerResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateTrainer(UpdateTrainerRequest request, StreamObserver<UpdateTrainerResponse> responseObserver) {
        az.fitnest.catalog.dto.TrainerRequest trainerRequest = new az.fitnest.catalog.dto.TrainerRequest();
        trainerRequest.setFullName(request.getFullName());
        trainerRequest.setSpecialization(request.getSpecialization());
        trainerRequest.setImageUrl(request.getImageUrl());
        
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
        az.fitnest.catalog.dto.GymRequest gymRequest = new az.fitnest.catalog.dto.GymRequest();
        gymRequest.setName(request.getName());
        gymRequest.setDescription(request.getDescription());
        gymRequest.setStatus(request.getStatus());

        
        az.fitnest.catalog.dto.AddressDto addressDto = new az.fitnest.catalog.dto.AddressDto();
        // support address provided as textual "lat, lon" in addressText or as separate latitude/longitude
        if (request.hasAddress()) {
            String addrText = request.getAddress().getAddressText();
            double lat = request.getAddress().getLatitude();
            double lng = request.getAddress().getLongitude();
            // If lat/lng are zero and addressText contains coordinates, try to parse them
            if ((lat == 0.0 && lng == 0.0) && addrText != null && addrText.contains(",")) {
                String[] parts = addrText.split(",");
                if (parts.length >= 2) {
                    try {
                        lat = Double.parseDouble(parts[0].trim());
                        lng = Double.parseDouble(parts[1].trim());
                        // clear addressText so service will resolve it from coordinates
                        addrText = null;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            addressDto.setAddressText(addrText);
            addressDto.setLatitude(lat == 0.0 ? null : lat);
            addressDto.setLongitude(lng == 0.0 ? null : lng);
        }
         gymRequest.setAddress(addressDto);

         gymRequest.setPhone(request.getPhone());
         gymRequest.setEmail(request.getEmail());
         gymRequest.setCategoryIds(new java.util.HashSet<>(request.getCategoryIdsList().stream()
                 .map(Long::valueOf).collect(java.util.stream.Collectors.toList())));
         return gymRequest;
     }

     private az.fitnest.catalog.dto.GymRequest mapToGymRequest(UpdateGymRequest request) {
         az.fitnest.catalog.dto.GymRequest gymRequest = new az.fitnest.catalog.dto.GymRequest();
         gymRequest.setName(request.getName());
         gymRequest.setDescription(request.getDescription());
         gymRequest.setStatus(request.getStatus());


         az.fitnest.catalog.dto.AddressDto addressDto = new az.fitnest.catalog.dto.AddressDto();
        if (request.hasAddress()) {
            String addrText = request.getAddress().getAddressText();
            double lat = request.getAddress().getLatitude();
            double lng = request.getAddress().getLongitude();
            if ((lat == 0.0 && lng == 0.0) && addrText != null && addrText.contains(",")) {
                String[] parts = addrText.split(",");
                if (parts.length >= 2) {
                    try {
                        lat = Double.parseDouble(parts[0].trim());
                        lng = Double.parseDouble(parts[1].trim());
                        addrText = null;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            addressDto.setAddressText(addrText);
            addressDto.setLatitude(lat == 0.0 ? null : lat);
            addressDto.setLongitude(lng == 0.0 ? null : lng);
        }
         gymRequest.setAddress(addressDto);

         gymRequest.setPhone(request.getPhone());
         gymRequest.setEmail(request.getEmail());
         gymRequest.setCategoryIds(new java.util.HashSet<>(request.getCategoryIdsList().stream()
                 .map(Long::valueOf).collect(java.util.stream.Collectors.toList())));
         return gymRequest;
     }
}
