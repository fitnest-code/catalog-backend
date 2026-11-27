package az.fitnest.catalog.service;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.category.CategoryEntryPriceResolver;
import az.fitnest.catalog.service.category.GymCategoryOfferingsService;
import az.fitnest.catalog.util.UserContext;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * v6 primary QR entrance: resolves MAIN/SUB, single-entry price for SUB, then calls order
 * {@code /api/v6/internal/primary-gym/check-ins}. Leaves v4/v5 entrance services untouched.
 */
@Service
public class GymEntranceV6Service {

  private final GymRepository gyms;
  private final CategoryEntryPriceResolver categoryResolver;
  private final GymMembershipPolicyV4Service membershipPolicies;
  private final OrderServiceGrpcClient orderClient;
  private final RestTemplate http;
  private final String orderBase;
  private final String primaryKey;

  public GymEntranceV6Service(
      GymRepository gyms,
      CategoryEntryPriceResolver categoryResolver,
      GymMembershipPolicyV4Service membershipPolicies,
      OrderServiceGrpcClient orderClient,
      @Value("${order.backend.url:http://order-backend:8080}") String orderBase,
      @Value("${primary-gym.internal-key:}") String primaryKey) {
    this.gyms = gyms;
    this.categoryResolver = categoryResolver;
    this.membershipPolicies = membershipPolicies;
    this.orderClient = orderClient;
    this.orderBase = orderBase;
    this.primaryKey = primaryKey;
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(8));
    this.http = new RestTemplate(factory);
  }

  public record Scan(
      @NotNull @Positive Long gymId,
      @NotBlank String qrCodeValue,
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
      @NotBlank @Size(max = 100) String requestId,
      @NotNull Boolean isFirstVisitToGym,
      @Positive Long categoryId) {}

  public Object primaryScan(Scan input) {
    Long userId = UserContext.getCurrentUserId();
    if (userId == null) {
      throw error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
    if (primaryKey.isBlank()) {
      throw error(HttpStatus.SERVICE_UNAVAILABLE, "PRIMARY_GYM_NOT_CONFIGURED");
    }

    Gym gym =
        gyms.findWithDetailsById(input.gymId())
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "GYM_NOT_FOUND"));
    if (!Objects.equals(input.qrCodeValue(), gym.getQrCodeValue())
        && !Objects.equals(input.qrCodeValue(), gym.getQrCodeToken())) {
      throw error(HttpStatus.FORBIDDEN, "INVALID_QR_CODE");
    }
    var address = gym.getAddress();
    if (!Double.isFinite(input.lat())
        || !Double.isFinite(input.lng())
        || address == null
        || address.getLatitude() == null
        || address.getLongitude() == null) {
      throw error(HttpStatus.FORBIDDEN, "LOCATION_REQUIRED");
    }
    if (GymEntranceV4Service.distanceMeters(
            input.lat(), input.lng(), address.getLatitude(), address.getLongitude())
        > 200) {
      throw error(HttpStatus.FORBIDDEN, "TOO_FAR_FROM_GYM");
    }

    if (input.categoryId() != null) {
      categoryResolver.requireOwnedCategory(gym, input.categoryId());
    } else if (GymCategoryOfferingsService.categorySelectionRequired(gym)) {
      throw error(HttpStatus.BAD_REQUEST, "CATEGORY_ID_REQUIRED");
    }

    String role = resolveRole(gym, input.categoryId());
    var active = orderClient.getActiveSubscription(userId);
    String status = active == null ? null : active.getSubscriptionStatus();
    if (active == null
        || active.getPackageId() <= 0
        || status == null
        || !(status.equalsIgnoreCase("ACTIVE") || status.equalsIgnoreCase("FINISHED"))) {
      throw error(HttpStatus.CONFLICT, "NO_ACTIVE_SUBSCRIPTION");
    }
    long packageId = active.getPackageId();

    BigDecimal entryPrice;
    BigDecimal monthlyPrice = null;
    if ("SUB".equals(role)) {
      var prices = categoryResolver.resolve(gym, packageId, input.categoryId());
      if (prices.singlePrice() == null) {
        throw error(HttpStatus.CONFLICT, "CATEGORY_PRICE_NOT_CONFIGURED");
      }
      entryPrice = BigDecimal.valueOf(prices.singlePrice());
    } else {
      var quote =
          membershipPolicies.quote(
              gym.getId(), packageId, LocalDateTime.now(GymMembershipPolicyV4Service.ZONE));
      if (quote == null || !quote.allowed() || quote.entryPrice() == null) {
        throw error(HttpStatus.CONFLICT, "PRICE_NOT_CONFIGURED");
      }
      entryPrice = quote.entryPrice();
      monthlyPrice = quote.primaryMonthlyPrice();
    }

    var headers = new HttpHeaders();
    headers.set("X-User-Id", userId.toString());
    headers.set("X-Primary-Gym-Key", primaryKey);
    var payload = new LinkedHashMap<String, Object>();
    payload.put("gymId", input.gymId());
    payload.put("requestId", input.requestId());
    if (input.categoryId() != null) {
      payload.put("categoryId", input.categoryId());
    }
    payload.put("categoryRole", role);
    payload.put("entryPrice", entryPrice);
    if (monthlyPrice != null) {
      payload.put("monthlyPrice", monthlyPrice);
    }

    try {
      return http.exchange(
              orderBase + "/api/v6/internal/primary-gym/check-ins",
              HttpMethod.POST,
              new HttpEntity<>(payload, headers),
              Object.class)
          .getBody();
    } catch (HttpStatusCodeException e) {
      String reason = "CHECKIN_REJECTED";
      try {
        var body =
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(e.getResponseBodyAsString());
        reason = body.path("error").path("code").asText(reason);
      } catch (Exception ignored) {
      }
      throw new ResponseStatusException(e.getStatusCode(), reason, e);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CHECKIN_UNAVAILABLE", e);
    }
  }

  static String resolveRole(Gym gym, Long categoryId) {
    if (categoryId == null) {
      return "MAIN";
    }
    if (CategoryEntryPriceResolver.subCategoryIds(gym).contains(categoryId)) {
      return "SUB";
    }
    return "MAIN";
  }

  private static ResponseStatusException error(HttpStatus status, String reason) {
    return new ResponseStatusException(status, reason);
  }
}
