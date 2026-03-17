package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Gym;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class GymRepositoryTest {
    @Autowired
    private GymRepository gymRepository;

    @Test
    void findWithDetailsById_shouldEagerlyLoadAllAssociations() {
        // Given: a gym with all associations (assume DataInitializer or test data is present)
        Optional<Gym> gymOpt = gymRepository.findAll().stream().findFirst();
        assertThat(gymOpt).isPresent();
        Long gymId = gymOpt.get().getId();

        // When
        Gym gym = gymRepository.findWithDetailsById(gymId).orElseThrow();

        // Then: all associations should be loaded and accessible
        assertThat(gym.getSubscriptions()).isNotNull();
        gym.getSubscriptions().forEach(sub -> {
            assertThat(sub.getBenefits()).isNotNull();
            sub.getBenefits().forEach(benefit -> assertThat(benefit.getBenefit()).isNotNull());
        });
        assertThat(gym.getRooms()).isNotNull();
        assertThat(gym.getWorkHours()).isNotNull();
        assertThat(gym.getWorkHoursWoman()).isNotNull();
        assertThat(gym.getWorkHoursMan()).isNotNull();
    }
}

