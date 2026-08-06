package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.StorageGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ForbiddenException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.exception.UnauthorizedException;
import az.fitnest.catalog.model.entity.*;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.repository.*;
import az.fitnest.catalog.service.GymEntranceService;
import az.fitnest.catalog.service.GymQrCodeService;
import az.fitnest.catalog.util.PlatformUtil;
import az.fitnest.catalog.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymEntranceServiceImpl implements GymEntranceService {

    private static final java.util.concurrent.ConcurrentHashMap<Long, String> PACKAGE_NAME_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    static {
        PACKAGE_NAME_CACHE.put(1L, "Bronze");
        PACKAGE_NAME_CACHE.put(2L, "Silver");
        PACKAGE_NAME_CACHE.put(3L, "Gold");
        PACKAGE_NAME_CACHE.put(4L, "Platinum");
    }

    private final GymRepository gymRepository;
    private final GymEntranceHistoryRepository gymEntranceHistoryRepository;
    private final GymAdminRepository gymAdminRepository;
    private final ReservationRepository reservationRepository;
    private final OrderServiceGrpcClient orderServiceGrpcClient;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final StorageGrpcClient storageGrpcClient;
    private final GymQrCodeService gymQrCodeService;
    private final MessageSource messageSource;
    private final az.fitnest.catalog.service.TranslationService translationService;

    private String getUserLanguage(Long userId) {
        try {
            var requestAttributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
                String acceptLanguage = request.getHeader("Accept-Language");
                if (acceptLanguage != null && !acceptLanguage.trim().isEmpty()) {
                    String upper = acceptLanguage.trim().split("[,;-]")[0].toUpperCase();
                    if (upper.equals("EN") || upper.equals("RU") || upper.equals("AZ")) {
                        return upper;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (userId != null) {
            try {
                az.fitnest.catalog.client.CachedUser user = userServiceGrpcClient.getUserById(userId);
                if (user != null && user.getLanguage() != null && !user.getLanguage().isBlank()) {
                    String lang = user.getLanguage().toUpperCase().trim();
                    if (lang.equals("EN") || lang.equals("RU") || lang.equals("AZ")) {
                        return lang;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return az.fitnest.catalog.util.UserContext.getUserLanguage();
    }

    private String getLocalizedGymName(Gym gym, String userLanguage) {
        if (gym == null)
            return null;
        return gym.getName();
    }

    private double calculateDistanceRaw(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    private void verifyGymOwnership(Long gymId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        Long userId = UserContext.getCurrentUserId();
        if (userId == null || !gymAdminRepository.existsByGymIdAndUserId(gymId, userId)) {
            throw new ForbiddenException("You do not have access to this gym");
        }
    }

    private int getPackageRank(String packageName) {
        if (packageName == null) {
            return 0;
        }
        String lower = packageName.toLowerCase();
        if (lower.contains("platinum")) {
            return 4;
        }
        if (lower.contains("gold")) {
            return 3;
        }
        if (lower.contains("silver")) {
            return 2;
        }
        if (lower.contains("bronze")) {
            return 1;
        }
        return 0;
    }

    private boolean isWithinWorkingHours(Gym gym, String gender) {
        boolean noHours = (gym.getGeneralWorkHours() == null || gym.getGeneralWorkHours().isEmpty()) &&
                (gym.getWorkHoursMan() == null || gym.getWorkHoursMan().isEmpty()) &&
                (gym.getWorkHoursWoman() == null || gym.getWorkHoursWoman().isEmpty());

        if (noHours) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Baku"));
        java.time.DayOfWeek today = now.getDayOfWeek();
        java.time.LocalTime currentTime = now.toLocalTime();

        boolean allowedInGeneral = isTimeWithinSlots(gym.getGeneralWorkHours(), today, currentTime);
        if (allowedInGeneral) {
            return true;
        }

        if ("MALE".equalsIgnoreCase(gender)) {
            return isTimeWithinSlots(gym.getWorkHoursMan(), today, currentTime);
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            return isTimeWithinSlots(gym.getWorkHoursWoman(), today, currentTime);
        }

        return false;
    }

    private boolean isTimeWithinSlots(java.util.Collection<GymWorkHour> slots, java.time.DayOfWeek today, java.time.LocalTime currentTime) {
        if (slots == null || slots.isEmpty()) {
            return false;
        }

        az.fitnest.catalog.model.enums.GymWorkHourPeriod periodToday = az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(today.name());
        az.fitnest.catalog.model.enums.GymWorkHourPeriod periodYesterday = az.fitnest.catalog.model.enums.GymWorkHourPeriod.valueOf(today.minus(1).name());

        // Check if there is a slot starting today that covers current time
        boolean matchesToday = slots.stream()
                .filter(h -> h.getPeriod() == periodToday)
                .anyMatch(h -> {
                    java.time.LocalTime from = h.getFromTime();
                    java.time.LocalTime to = h.getToTime();
                    if (from == null && to == null) return true;
                    if (from == null) return !currentTime.isAfter(to);
                    if (to == null) return !currentTime.isBefore(from);
                    if (!from.isAfter(to)) {
                        return !currentTime.isBefore(from) && !currentTime.isAfter(to);
                    } else {
                        return !currentTime.isBefore(from);
                    }
                });

        if (matchesToday) {
            return true;
        }

        // Check if there is a slot starting yesterday that crossed midnight into today and covers current time
        boolean matchesYesterday = slots.stream()
                .filter(h -> h.getPeriod() == periodYesterday)
                .anyMatch(h -> {
                    java.time.LocalTime from = h.getFromTime();
                    java.time.LocalTime to = h.getToTime();
                    if (from != null && to != null && from.isAfter(to)) {
                        return !currentTime.isAfter(to);
                    }
                    return false;
                });

        return matchesYesterday;
    }

    private Long extractGymIdFromQr(String qrCodeValue) {
        if (qrCodeValue == null || qrCodeValue.isBlank())
            return null;
        try {
            return Long.parseLong(qrCodeValue.trim());
        } catch (NumberFormatException e) {
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/gym/(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(qrCodeValue);
                if (matcher.find()) {
                    return Long.parseLong(matcher.group(1));
                }
                return null;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GymEntranceResponse checkProximity(Double lat, Double lng, Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        Address address = gym.getAddress();
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            return GymEntranceResponse.builder()
                    .allowed(false)
                    .build();
        }
        double distance = calculateDistanceRaw(lat, lng, address.getLatitude(), address.getLongitude());
        double allowedRadiusKm = 0.2;
        if (distance > allowedRadiusKm) {
            return GymEntranceResponse.builder()
                    .allowed(false)
                    .build();
        }
        return GymEntranceResponse.builder()
                .allowed(true)
                .build();
    }

    /**
     * Check if a subscription status from the order-backend gRPC service represents an active subscription.
     * The gRPC response may contain translated status values (e.g. "Aktiv" in AZ, "Активный" in RU)
     * or special statuses like "last_7_days" or "changed", all of which mean the subscription IS active.
     * Only "none", "cancelled", "expired" (case-insensitive) mean NOT active.
     */
    private boolean isSubscriptionStatusActive(String status) {
        if (status == null || status.isEmpty()) return false;
        String lower = status.toLowerCase().trim();
        return !lower.equals("none") && !lower.equals("cancelled") && !lower.equals("expired");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkGymEntranceEligibilitySimple(Object principal) {
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("error.unauthorized");
        }
        az.fitnest.order.grpc.ActiveSubscriptionResponse subResp;
        try {
            subResp = orderServiceGrpcClient.getActiveSubscription(userId);
        } catch (Exception e) {
            throw new IllegalStateException("error.subscription_fetch_failed");
        }
        String status = subResp.getSubscriptionStatus();
        if (!isSubscriptionStatusActive(status)) {
            return false;
        }
        int visitLimitRemaining = subResp.getRemainingLimit();
        return visitLimitRemaining > 0;
    }

    @Override
    @Transactional
    public String getGymQrUrl(Long gymId) {
        Gym gym = gymRepository.findById(gymId).orElseThrow(() ->
                new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        String qrCodeUrl = gym.getQrCodeUrl();
        boolean needsRegenerate = false;

        if (qrCodeUrl == null || qrCodeUrl.trim().isEmpty() || qrCodeUrl.contains("PENDING") || qrCodeUrl.contains("/qr") ||
                gym.getQrCodeValue() == null || !gym.getQrCodeValue().equals(gymId.toString())) {
            needsRegenerate = true;
        } else {
            try {
                String fileId = qrCodeUrl;
                if (qrCodeUrl.contains("/stream/")) {
                    fileId = qrCodeUrl.substring(qrCodeUrl.lastIndexOf("/stream/") + 8);
                } else if (qrCodeUrl.contains("/")) {
                    fileId = qrCodeUrl.substring(qrCodeUrl.lastIndexOf("/") + 1);
                }
                storageGrpcClient.downloadFile(fileId, response -> {});
            } catch (Exception e) {
                needsRegenerate = true;
            }
        }

        if (needsRegenerate) {
            qrCodeUrl = gymQrCodeService.generateAndSaveQrCodeSync(gymId);
        }
        return qrCodeUrl;
    }

    @Override
    @Transactional
    public GymEntranceScanResponse scanGymQrEntrance(Object principal, String qrCodeValue, Double lat, Double lng,
                                                     String userAgent) {
        String platform = PlatformUtil.detectPlatform(userAgent);
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("error.unauthorized");
        }
        Long gymId = extractGymIdFromQr(qrCodeValue);
        if (gymId == null) {
            throw new IllegalArgumentException("error.invalid_qr_code");
        }
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        boolean allowed = true;
        String reason = null;
        String status = "ELIGIBLE";

        Double amount = 0.0;
        Long packageId = null;
        az.fitnest.order.grpc.ActiveSubscriptionResponse subResp = null;
        boolean isTestUser = UserContext.isTestUser();
        if (isTestUser) {
            allowed = true;
            status = "ELIGIBLE";
        } else {
            try {
                subResp = orderServiceGrpcClient.getActiveSubscription(userId);
            String subStatus = subResp.getSubscriptionStatus();
            if (!isSubscriptionStatusActive(subStatus)) {
                allowed = false;
                reason = "NO_ACTIVE_SUBSCRIPTION";
            } else if (subResp.getRemainingLimit() <= 0) {
                allowed = false;
                reason = "VISIT_LIMIT_EXCEEDED";
            } else {
                long userPackageId = subResp.getPackageId();
                packageId = userPackageId;
                boolean checkHierarchySuccess = false;
                try {
                    List<Long> allPackageIds = new ArrayList<>();
                    allPackageIds.add(userPackageId);
                    if (gym.getSubscriptions() != null) {
                        for (var sub : gym.getSubscriptions()) {
                            if (sub.getPackageId() != null) {
                                allPackageIds.add(sub.getPackageId());
                            }
                        }
                    }

                    Map<Long, String> packageNamesMap = new java.util.HashMap<>();
                    List<Long> missingPackageIds = new ArrayList<>();
                    for (Long pkgId : allPackageIds) {
                        String cachedName = PACKAGE_NAME_CACHE.get(pkgId);
                        if (cachedName != null) {
                            packageNamesMap.put(pkgId, cachedName);
                        } else {
                            missingPackageIds.add(pkgId);
                        }
                    }

                    if (!missingPackageIds.isEmpty()) {
                        List<az.fitnest.order.grpc.PackageNameInfo> packageNames = orderServiceGrpcClient.getPackageNamesByIds(missingPackageIds);
                        for (var info : packageNames) {
                            PACKAGE_NAME_CACHE.put(info.getPackageId(), info.getName());
                            packageNamesMap.put(info.getPackageId(), info.getName());
                        }
                    }

                    String userPackageName = packageNamesMap.get(userPackageId);
                    int userRank = getPackageRank(userPackageName);

                    List<GymSubscription> eligibleSubscriptions = new ArrayList<>();
                    if (gym.getSubscriptions() != null) {
                        for (var sub : gym.getSubscriptions()) {
                            if (sub.getPackageId() != null) {
                                String gymPackageName = packageNamesMap.get(sub.getPackageId());
                                if (gymPackageName != null) {
                                    int gymRank = getPackageRank(gymPackageName);
                                    if (userRank >= gymRank) {
                                        eligibleSubscriptions.add(sub);
                                    }
                                }
                            }
                        }
                    }

                    if (eligibleSubscriptions.isEmpty()) {
                        allowed = false;
                        reason = "GYM_NOT_SUPPORTED";
                        checkHierarchySuccess = true;
                    } else {
                        GymSubscription bestSub = null;
                        int bestRank = Integer.MAX_VALUE;
                        for (var sub : eligibleSubscriptions) {
                            String name = packageNamesMap.get(sub.getPackageId());
                            int rank = getPackageRank(name);
                            if (rank < bestRank) {
                                bestRank = rank;
                                bestSub = sub;
                            }
                        }
                        if (bestSub != null) {
                            amount = bestSub.getDailyPrice() != null ? bestSub.getDailyPrice() : 0.0;
                            packageId = bestSub.getPackageId();
                            checkHierarchySuccess = true;
                        }
                    }
                } catch (Exception e) {
                    // Fallback to exact matching
                }

                if (!checkHierarchySuccess) {
                    var matchedSub = gym.getSubscriptions() != null ? gym.getSubscriptions().stream()
                            .filter(sub -> sub.getPackageId() != null && sub.getPackageId().equals(userPackageId))
                            .findFirst() : java.util.Optional.<GymSubscription>empty();

                    if (matchedSub.isEmpty()) {
                        allowed = false;
                        reason = "GYM_NOT_SUPPORTED";
                    } else {
                        amount = matchedSub.get().getDailyPrice() != null ? matchedSub.get().getDailyPrice() : 0.0;
                        packageId = matchedSub.get().getPackageId();
                    }
                }
            }
        } catch (Exception e) {
            allowed = false;
            reason = "NO_ACTIVE_SUBSCRIPTION";
            }
        }

        if (allowed && !isTestUser) {
            java.time.LocalDateTime nowBaku = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Baku"));
            java.time.LocalDateTime twoHoursAgo = nowBaku.minusHours(2);
            boolean alreadyScanned = gymEntranceHistoryRepository.existsByUserIdAndGymIdAndStatusInAndScanDateBetween(
                    userId, gymId, java.util.List.of("ELIGIBLE", "Uğurlu"), twoHoursAgo, nowBaku);
            if (alreadyScanned) {
                allowed = false;
                reason = "ALREADY_SCANNED_TODAY";
            }
        }

        if (allowed && !isTestUser) {
            Address address = gym.getAddress();
            if (address != null && address.getLatitude() != null && address.getLongitude() != null && lat != null
                    && lng != null) {
                double distance = calculateDistanceRaw(lat, lng, address.getLatitude(), address.getLongitude());
                if (distance > 0.2) {
                    allowed = false;
                    reason = "TOO_FAR_FROM_GYM";
                }
            }
        }

        if (allowed && !isTestUser) {
            try {
                // Check if there is an active reservation at this gym today
                List<Reservation> activeReservations = reservationRepository.findActiveReservationsForCheckIn(
                        userId, gymId, java.time.LocalDate.now(), java.time.LocalTime.now(),
                        List.of(ReservationStatus.APPROVED, ReservationStatus.PENDING)
                );

                boolean consumeFrozen = false;
                Reservation targetReservation = null;
                if (!activeReservations.isEmpty()) {
                    consumeFrozen = true;
                    targetReservation = activeReservations.get(0);
                }

                orderServiceGrpcClient.checkIn(userId, gymId, consumeFrozen);

                if (targetReservation != null) {
                    targetReservation.setAttended(true);
                    if (targetReservation.getStatus() == ReservationStatus.PENDING) {
                        targetReservation.setStatus(ReservationStatus.APPROVED);
                        targetReservation.setApprovedAt(LocalDateTime.now());
                    }
                    reservationRepository.save(targetReservation);
                }
            } catch (Exception e) {
                allowed = false;
                status = "UNSUCCESSFUL";
                reason = "CHECKIN_FAILED";
            }
        }

        if (!allowed) {
            status = "UNSUCCESSFUL";
        }

        GymEntranceHistory history = GymEntranceHistory.builder()
                .userId(userId)
                .gymId(gymId)
                .scanDate(LocalDateTime.now(ZoneId.of("Asia/Baku")))
                .status(status)
                .reason(reason)
                .platform(platform)
                .amount(amount)
                .packageId(packageId)
                .build();
        gymEntranceHistoryRepository.save(history);

        String userLanguage = getUserLanguage(userId);
        String localizedName = getLocalizedGymName(gym, userLanguage);
        Address addr = gym.getAddress();
        String gymAddress = addr != null ? addr.getAddressText() : null;

        return GymEntranceScanResponse.builder()
                .gymName(allowed ? localizedName : null)
                .gymAddress(allowed ? gymAddress : null)
                .enterDate(allowed ? java.time.LocalDate.now(ZoneId.of("Asia/Baku")).toString() : null)
                .enterHour(allowed
                        ? java.time.LocalTime.now(ZoneId.of("Asia/Baku")).withSecond(0).withNano(0).toString()
                        : null)
                .isAllowed(allowed)
                .status(status)
                .reason(reason)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GymEntranceHistoryAdminResponse> getGymEntranceHistory(Long gymId) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        List<GymEntranceHistory> historyList = gymEntranceHistoryRepository.findByGymIdOrderByScanDateDesc(gymId);
        return historyList.stream().map(h -> {
            String firstName = "";
            String lastName = "";
            String phone = "";
            String profilePhotoUrl = "";
            try {
                az.fitnest.catalog.client.CachedUser user = userServiceGrpcClient.getUserById(h.getUserId());
                if (user != null) {
                    firstName = user.getFirstName();
                    lastName = user.getLastName();
                    phone = user.getMobile();
                    profilePhotoUrl = user.getProfileImageUrl();
                }
            } catch (Exception e) {
                firstName = "User";
                lastName = String.valueOf(h.getUserId());
            }
            String formattedDate = h.getScanDate()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String displayStatus = "Uğursuz";
            if ("ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus())) {
                displayStatus = "Uğurlu";
            }
            String userLang = getUserLanguage(h.getUserId());
            String translatedStatus = translationService.getTranslatedValue("ENTRANCE_STATUS", displayStatus, "name", userLang);
            if (translatedStatus != null && !translatedStatus.isEmpty()) {
                displayStatus = translatedStatus;
            }
            return GymEntranceHistoryAdminResponse.builder()
                    .id(h.getId())
                    .userId(h.getUserId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .phone(phone)
                    .scanDateTime(formattedDate)
                    .status(displayStatus)
                    .reason(h.getReason())
                    .amount(h.getAmount() != null ? h.getAmount() : 0.0)
                    .profilePhotoUrl(profilePhotoUrl)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GymEntranceEligibilityResponse checkGymEntranceEligibility(Object principal) {
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("error.unauthorized");
        }
        if (UserContext.isTestUser()) {
            return GymEntranceEligibilityResponse.builder()
                    .allowed(true)
                    .status("ELIGIBLE")
                    .build();
        }
        az.fitnest.order.grpc.ActiveSubscriptionResponse subResp = null;
        try {
            subResp = orderServiceGrpcClient.getActiveSubscription(userId);
            String status = subResp.getSubscriptionStatus();
            if (!isSubscriptionStatusActive(status)) {
                return GymEntranceEligibilityResponse.builder()
                        .allowed(false)
                        .status("INELIGIBLE")
                        .reason(localizeEligibilityReason("NO_ACTIVE_SUBSCRIPTION"))
                        .build();
            }
            int visitLimitRemaining = subResp.getRemainingLimit();
            if (visitLimitRemaining <= 0) {
                return GymEntranceEligibilityResponse.builder()
                        .allowed(false)
                        .status("INELIGIBLE")
                        .reason(localizeEligibilityReason("VISIT_LIMIT_EXCEEDED"))
                        .build();
            }
            return GymEntranceEligibilityResponse.builder()
                    .allowed(true)
                    .status("ELIGIBLE")
                    .build();
        } catch (Exception e) {
            return GymEntranceEligibilityResponse.builder()
                    .allowed(false)
                    .status("INELIGIBLE")
                    .reason(localizeEligibilityReason("NO_ACTIVE_SUBSCRIPTION"))
                    .build();
        }
    }

    private String localizeEligibilityReason(String reasonCode) {
        String messageKey = switch (reasonCode) {
            case "NO_ACTIVE_SUBSCRIPTION" -> "error.no_active_subscription";
            case "VISIT_LIMIT_EXCEEDED" -> "error.visit_limit_exceeded";
            default -> null;
        };
        if (messageKey == null) {
            return reasonCode;
        }
        try {
            return messageSource.getMessage(messageKey, null, reasonCode, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return reasonCode;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminQrScanHistoryResponse> getUserQrScanHistoryAdmin(Long userId, String query, String sort) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        List<GymEntranceHistory> historyList = gymEntranceHistoryRepository.findAllByUserIdOrderByScanDateDesc(userId);
        if (historyList.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> gymIds = historyList.stream().map(GymEntranceHistory::getGymId).distinct().toList();
        Map<Long, String> gymNames = gymRepository.findAllById(gymIds).stream()
                .collect(Collectors.toMap(Gym::getId, Gym::getName));

        java.util.stream.Stream<AdminQrScanHistoryResponse> stream = historyList.stream()
                .map(h -> {
                    String status = h.getStatus();
                    if ("ELIGIBLE".equalsIgnoreCase(status))
                        status = "Uğurlu";
                    else if ("UNSUCCESSFUL".equalsIgnoreCase(status))
                        status = "Uğursuz";

                    return AdminQrScanHistoryResponse.builder()
                            .dateTime(h.getScanDate() != null ? h.getScanDate().format(formatter) : "N/A")
                            .gymName(gymNames.getOrDefault(h.getGymId(), "Unknown Gym"))
                            .status(status)
                            .failedReason(h.getReason())
                            .platform(h.getPlatform() != null ? h.getPlatform() : "N/A")
                            .rawDate(h.getScanDate())
                            .build();
                });

        if (query != null && !query.isBlank()) {
            String lowerQuery = query.toLowerCase();
            stream = stream.filter(res -> res.gymName().toLowerCase().contains(lowerQuery));
        }

        List<AdminQrScanHistoryResponse> result = stream.collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "gymName_asc":
                    result.sort(
                            Comparator.comparing(AdminQrScanHistoryResponse::gymName, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "gymName_desc":
                    result.sort(Comparator.comparing(AdminQrScanHistoryResponse::gymName, String.CASE_INSENSITIVE_ORDER)
                            .reversed());
                    break;
                case "date_asc":
                    result.sort(Comparator.comparing(AdminQrScanHistoryResponse::rawDate,
                            Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case "date_desc":
                    result.sort(Comparator.comparing(AdminQrScanHistoryResponse::rawDate,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                    break;
                case "status_asc":
                    result.sort(
                            Comparator.comparing(AdminQrScanHistoryResponse::status, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "status_desc":
                    result.sort(Comparator.comparing(AdminQrScanHistoryResponse::status, String.CASE_INSENSITIVE_ORDER)
                            .reversed());
                    break;
                case "platform_asc":
                    result.sort(
                            Comparator.comparing(AdminQrScanHistoryResponse::platform, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "platform_desc":
                    result.sort(Comparator
                            .comparing(AdminQrScanHistoryResponse::platform, String.CASE_INSENSITIVE_ORDER).reversed());
                    break;
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public GymAnalyticsResponse getGymAnalytics(Long gymId, LocalDateTime startDate, LocalDateTime endDate,
                                                 String statusFilter, String sort, int page, int pageSize) {
        verifyGymOwnership(gymId);
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        List<GymEntranceHistory> historyList;
        if (startDate != null && endDate != null) {
            historyList = gymEntranceHistoryRepository.findByGymIdAndScanDateBetweenOrderByScanDateDesc(gymId,
                    startDate, endDate);
        } else {
            historyList = gymEntranceHistoryRepository.findByGymIdOrderByScanDateDesc(gymId);
        }

        long successfulScans = 0;
        long failedScans = 0;
        double totalProfit = 0.0;

        for (GymEntranceHistory h : historyList) {
            if ("ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus())) {
                successfulScans++;
                if (h.getAmount() != null) {
                    totalProfit += h.getAmount();
                }
            } else {
                failedScans++;
            }
        }

        java.util.stream.Stream<GymEntranceHistory> stream = historyList.stream();
        if (statusFilter != null && !statusFilter.isBlank()) {
            if ("SUCCESSFUL".equalsIgnoreCase(statusFilter) || "Uğurlu".equalsIgnoreCase(statusFilter)
                    || "ELIGIBLE".equalsIgnoreCase(statusFilter)) {
                stream = stream.filter(
                        h -> "ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus()));
            } else if ("UNSUCCESSFUL".equalsIgnoreCase(statusFilter) || "Uğursuz".equalsIgnoreCase(statusFilter)
                    || "Xəta".equalsIgnoreCase(statusFilter)) {
                stream = stream.filter(h -> "UNSUCCESSFUL".equalsIgnoreCase(h.getStatus())
                        || "Uğursuz".equalsIgnoreCase(h.getStatus()) || "Xəta".equalsIgnoreCase(h.getStatus()));
            }
        }

        List<GymEntranceHistory> filteredList = stream.collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "date_asc":
                    filteredList.sort(Comparator.comparing(GymEntranceHistory::getScanDate,
                            Comparator.nullsLast(Comparator.naturalOrder())));
                    break;
                case "date_desc":
                    filteredList.sort(Comparator
                            .comparing(GymEntranceHistory::getScanDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .reversed());
                    break;
                case "status_asc":
                    filteredList
                            .sort(Comparator.comparing(GymEntranceHistory::getStatus, String.CASE_INSENSITIVE_ORDER));
                    break;
                case "status_desc":
                    filteredList.sort(Comparator.comparing(GymEntranceHistory::getStatus, String.CASE_INSENSITIVE_ORDER)
                            .reversed());
                    break;
            }
        }

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(filteredList.size(), from + pageSize);
        List<GymEntranceHistory> pageItems = from >= filteredList.size() ? new ArrayList<>()
                : new ArrayList<>(filteredList.subList(from, to));

        List<GymEntranceHistoryAdminResponse> paginatedDtos = pageItems.stream().map(h -> {
            String firstName = "";
            String lastName = "";
            String phone = "";
            String profilePhotoUrl = "";
            try {
                az.fitnest.catalog.client.CachedUser user = userServiceGrpcClient.getUserById(h.getUserId());
                if (user != null) {
                    firstName = user.getFirstName();
                    lastName = user.getLastName();
                    phone = user.getMobile();
                    profilePhotoUrl = user.getProfileImageUrl();
                }
            } catch (Exception e) {
                firstName = "User";
                lastName = String.valueOf(h.getUserId());
            }
            String formattedDate = h.getScanDate()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String displayStatus = "Uğursuz";
            if ("ELIGIBLE".equalsIgnoreCase(h.getStatus()) || "Uğurlu".equalsIgnoreCase(h.getStatus())) {
                displayStatus = "Uğurlu";
            }
            return GymEntranceHistoryAdminResponse.builder()
                    .id(h.getId())
                    .userId(h.getUserId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .phone(phone)
                    .scanDateTime(formattedDate)
                    .status(displayStatus)
                    .reason(h.getReason())
                    .amount(h.getAmount() != null ? h.getAmount() : 0.0)
                    .profilePhotoUrl(profilePhotoUrl)
                    .build();
        }).collect(Collectors.toList());

        PaginatedResponse<GymEntranceHistoryAdminResponse> paginatedResponse = PaginatedResponse.<GymEntranceHistoryAdminResponse>builder()
                .items(paginatedDtos)
                .total((long) filteredList.size())
                .page(page)
                .pageSize(pageSize)
                .build();

        // Apply admin manual overrides for the summary cards when present.
        Gym gym = gymRepository.findById(gymId).orElse(null);
        double effectiveProfit = totalProfit;
        long effectiveSuccessful = successfulScans;
        long effectiveFailed = failedScans;
        if (gym != null) {
            if (gym.getAnalyticsProfitOverride() != null) {
                effectiveProfit = gym.getAnalyticsProfitOverride();
            }
            if (gym.getAnalyticsSuccessfulScansOverride() != null) {
                effectiveSuccessful = gym.getAnalyticsSuccessfulScansOverride();
            }
            if (gym.getAnalyticsFailedScansOverride() != null) {
                effectiveFailed = gym.getAnalyticsFailedScansOverride();
            }
        }

        return GymAnalyticsResponse.builder()
                .totalProfit(effectiveProfit)
                .successfulScans(effectiveSuccessful)
                .failedScans(effectiveFailed)
                .history(paginatedResponse)
                .build();
    }

    @Override
    @Transactional
    public void updateGymAnalyticsOverrides(Long gymId, az.fitnest.catalog.dto.request.UpdateGymAnalyticsRequest request) {
        verifyGymOwnership(gymId);
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (request.totalProfit() != null) {
            gym.setAnalyticsProfitOverride(request.totalProfit());
        }
        if (request.successfulScans() != null) {
            gym.setAnalyticsSuccessfulScansOverride(request.successfulScans());
        }
        if (request.failedScans() != null) {
            gym.setAnalyticsFailedScansOverride(request.failedScans());
        }

        gymRepository.save(gym);
    }
}
