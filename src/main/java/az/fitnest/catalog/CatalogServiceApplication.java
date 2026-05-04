package az.fitnest.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
@EnableKafka
@EnableJpaRepositories(basePackages = "az.fitnest.catalog.repository")
@EnableRedisRepositories(basePackages = {})
public class CatalogServiceApplication {
    public static void main(String[] args) {
        try {
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        } catch (Throwable t) {
            System.err.println("Warning: unable to set SecurityContextHolder strategy: " + t.getMessage());
        }
        SpringApplication.run(CatalogServiceApplication.class, (String[]) args);
    }
}
