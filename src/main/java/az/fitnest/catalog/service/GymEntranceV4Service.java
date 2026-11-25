package az.fitnest.catalog.service;

import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.category.CategoryEntryPriceResolver;
import az.fitnest.catalog.service.category.GymCategoryOfferingsService;
import az.fitnest.catalog.util.UserContext;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GymEntranceV4Service {
  private final GymRepository gyms;
  private final RestTemplate http;
  private final String base;
  private final String key;
  private final CategoryEntryPriceResolver categoryResolver;

  public GymEntranceV4Service(
      GymRepository gyms,
      CategoryEntryPriceResolver categoryResolver,
      @Value("${order.backend.url:http://order-backend:8080}") String base,
      @Value("${primary-gym.internal-key:}") String key) {
    this.gyms = gyms;
    this.categoryResolver = categoryResolver;
    this.base = base;
    this.key = key;
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(8));
    http = new RestTemplate(factory);
  }

  public record Scan(
      @NotNull @Positive Long gymId,
      @NotBlank String qrCodeValue,
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
      @NotBlank @Size(max = 100) String requestId,
      @NotNull Boolean isFirstVisitToGym,
      /** v5 category picker; null for v4 clients (body field is optional). */
      @Positive Long categoryId) {

    /** Keeps the original v4 six-argument shape for existing callers. */
    public Scan(
        Long gymId,
        String qrCodeValue,
        Double lat,
        Double lng,
        String requestId,
        Boolean isFirstVisitToGym) {
      this(gymId, qrCodeValue, lat, lng, requestId, isFirstVisitToGym, null);
    }
  }

  public Object scan(Scan input) {
    return scan(input, false);
  }

  /**
   * @param requireCategoryWhenNeeded v5 primary-scan: gyms with subcategories / several categories must
   *     receive a categoryId. v4 passes false so its behaviour is unchanged.
   */
  public Object scan(Scan input, boolean requireCategoryWhenNeeded) {
    Long userId = UserContext.getCurrentUserId();
    if (userId == null) throw error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    if (key.isBlank()) throw error(HttpStatus.SERVICE_UNAVAILABLE, "PRIMARY_GYM_NOT_CONFIGURED");
    var gym =
        gyms.findWithDetailsById(input.gymId())
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "GYM_NOT_FOUND"));
    // Match the QR value stored by the existing gym QR generator.
    if (!Objects.equals(input.qrCodeValue(), gym.getQrCodeValue())
        && !Objects.equals(input.qrCodeValue(), gym.getQrCodeToken()))
      throw error(HttpStatus.FORBIDDEN, "INVALID_QR_CODE");
    var address = gym.getAddress();
    if (!Double.isFinite(input.lat())
        || !Double.isFinite(input.lng())
        || address == null
        || address.getLatitude() == null
        || address.getLongitude() == null) throw error(HttpStatus.FORBIDDEN, "LOCATION_REQUIRED");
    if (distanceMeters(input.lat(), input.lng(), address.getLatitude(), address.getLongitude())
        > 200) throw error(HttpStatus.FORBIDDEN, "TOO_FAR_FROM_GYM");
    if (input.categoryId() != null) {
      categoryResolver.requireOwnedCategory(gym, input.categoryId());
    } else if (requireCategoryWhenNeeded && GymCategoryOfferingsService.categorySelectionRequired(gym)) {
      throw error(HttpStatus.BAD_REQUEST, "CATEGORY_ID_REQUIRED");
    }
    var headers = new HttpHeaders();
    headers.set("X-User-Id", userId.toString());
    headers.set("X-Primary-Gym-Key", key);
    // categoryId is only forwarded when present so v4 order-backend payloads stay identical.
    var payload = new LinkedHashMap<String, Object>();
    payload.put("gymId", input.gymId());
    payload.put("requestId", input.requestId());
    if (input.categoryId() != null) payload.put("categoryId", input.categoryId());
    try {
      return http.exchange(
              base + "/api/v4/internal/primary-gym/check-ins",
              HttpMethod.POST,
              new HttpEntity<>(payload, headers),
              Object.class)
          .getBody();
    } catch (HttpStatusCodeException e) {
      String reason = "CHECKIN_REJECTED";
      try {
        var body =
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
        reason = body.path("error").path("code").asText(reason);
      } catch (Exception ignored) {
      }
      throw new ResponseStatusException(e.getStatusCode(), reason, e);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CHECKIN_UNAVAILABLE", e);
    }
  }

  public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
    double a =
        Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2);
    return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0, 1 - a)));
  }

  private static ResponseStatusException error(HttpStatus s, String reason) {
    return new ResponseStatusException(s, reason);
  }
}
