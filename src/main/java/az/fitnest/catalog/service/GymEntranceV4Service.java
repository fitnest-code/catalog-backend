package az.fitnest.catalog.service;

import az.fitnest.catalog.repository.GymRepository;
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

  public GymEntranceV4Service(
      GymRepository gyms,
      @Value("${order.backend.url:http://order-backend:8080}") String base,
      @Value("${primary-gym.internal-key:}") String key) {
    this.gyms = gyms;
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
      @NotBlank @Size(max = 100) String requestId) {}

  public Object scan(Scan input) {
    Long userId = UserContext.getCurrentUserId();
    if (userId == null) throw error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    if (key.isBlank()) throw error(HttpStatus.SERVICE_UNAVAILABLE, "PRIMARY_GYM_NOT_CONFIGURED");
    var gym =
        gyms.findById(input.gymId())
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
    var headers = new HttpHeaders();
    headers.set("X-User-Id", userId.toString());
    headers.set("X-Primary-Gym-Key", key);
    try {
      return http.exchange(
              base + "/api/v4/internal/primary-gym/check-ins",
              HttpMethod.POST,
              new HttpEntity<>(
                  Map.of("gymId", input.gymId(), "requestId", input.requestId()), headers),
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
