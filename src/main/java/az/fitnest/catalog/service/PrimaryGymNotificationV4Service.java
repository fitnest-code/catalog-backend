package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.v4.*;
import az.fitnest.catalog.model.entity.PrimaryGymOutboxV4;
import az.fitnest.catalog.repository.PrimaryGymOutboxV4Repository;
import az.fitnest.catalog.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrimaryGymNotificationV4Service {
  private final PrimaryGymOutboxV4Repository outbox;
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final RestTemplate http;
  private final String base, key;

  public PrimaryGymNotificationV4Service(
      PrimaryGymOutboxV4Repository outbox,
      @Value("${order.backend.url:http://order-backend:8080}") String base,
      @Value("${primary-gym.internal-key:}") String key) {
    this.outbox = outbox;
    this.base = base;
    this.key = key;
    var f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(Duration.ofSeconds(2));
    f.setReadTimeout(Duration.ofSeconds(5));
    http = new RestTemplate(f);
  }

  public void record(Long gymId, Set<Long> disabledPackages, GymPolicyUpdateV4 input) {
    if (!input.notifySubscribers()) return;
    if (input.notification() == null
        || !input.notification().keySet().equals(Set.of("AZ", "EN", "RU")))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "THREE_TRANSLATIONS_REQUIRED");
    if (disabledPackages.isEmpty()) return;
    if (key.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "PRIMARY_GYM_NOT_CONFIGURED");
    List<Recipient> affected = affected(gymId);
    var users =
        affected.stream()
            .filter(a -> disabledPackages.contains(a.packageId()))
            .map(Recipient::userId)
            .distinct()
            .toList();
    var event = new PrimaryGymOutboxV4();
    event.setId(UUID.randomUUID().toString());
    try {
      event.setPayload(
          mapper.writeValueAsString(
              Map.of(
                  "eventId",
                  event.getId(),
                  "gymId",
                  gymId,
                  "userIds",
                  users,
                  "contents",
                  input.notification())));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    outbox.save(event);
  }

  public List<Recipient> affected(Long gymId) {
    if (key.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "PRIMARY_GYM_NOT_CONFIGURED");
    var headers = new HttpHeaders();
    headers.set("X-User-Id", UserContext.getCurrentUserId().toString());
    headers.set("X-Primary-Gym-Key", key);
    List<Recipient> affected =
        http.exchange(
                base + "/api/v4/internal/primary-gym/gyms/" + gymId + "/subscribers",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<Recipient>>() {})
            .getBody();
    if (affected == null)
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "SUBSCRIBER_DIRECTORY_UNAVAILABLE");
    return affected;
  }

  public record Recipient(
      Long userId,
      Long subscriptionId,
      Long packageId,
      Long gymId,
      java.time.LocalDateTime selectedAt,
      java.time.LocalDateTime expiresAt,
      long cycleVisits) {}
}
