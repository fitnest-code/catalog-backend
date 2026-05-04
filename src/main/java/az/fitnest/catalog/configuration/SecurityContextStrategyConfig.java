package az.fitnest.catalog.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import javax.annotation.PostConstruct;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityContextStrategyConfig {

    @PostConstruct
    public void init() {
        try {
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
            log.info("SecurityContextHolder strategy set to MODE_INHERITABLETHREADLOCAL to allow child threads to inherit SecurityContext");
        } catch (Exception e) {
            log.warn("Failed to set SecurityContextHolder strategy to MODE_INHERITABLETHREADLOCAL", e);
        }
    }
}
