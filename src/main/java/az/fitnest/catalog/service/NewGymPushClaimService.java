package az.fitnest.catalog.service;

import az.fitnest.catalog.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Compare-and-set for the one-time new-gym push. {@link #release} uses a new
 * transaction because it runs after the activation transaction has committed,
 * from the thread that called notifications.
 */
@Service
@RequiredArgsConstructor
public class NewGymPushClaimService {

    private final GymRepository gymRepository;

    @Transactional
    public LocalDateTime claim(Long gymId) {
        LocalDateTime claimedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        int updated = gymRepository.claimNewGymPush(gymId, claimedAt);
        return updated == 1 ? claimedAt : null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(Long gymId, LocalDateTime claimedAt) {
        gymRepository.releaseNewGymPush(gymId, claimedAt);
    }
}
