package az.fitnest.catalog.service;

import az.fitnest.catalog.repository.PrimaryGymOutboxV4Repository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrimaryGymOutboxV4Relay {
  private final PrimaryGymOutboxV4Repository outbox;
  private final KafkaTemplate<String, Object> kafka;
  private final ObjectMapper mapper = new ObjectMapper();

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void relay() {
    for (var event : outbox.pending(PageRequest.of(0, 25))) {
      try {
        Map<String, Object> payload =
            mapper.readValue(event.getPayload(), new TypeReference<>() {});
        kafka
            .send("primary-gym-disabled-v4", event.getId(), payload)
            .get(5, java.util.concurrent.TimeUnit.SECONDS);
        event.setPublished(true);
        outbox.save(event);
      } catch (Exception e) {
        log.warn(
            "Primary gym notification {} will retry: {}",
            event.getId(),
            e.getClass().getSimpleName());
        break;
      }
    }
  }
}
