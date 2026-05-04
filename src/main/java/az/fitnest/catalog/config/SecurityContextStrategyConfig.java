package az.fitnest.catalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import javax.annotation.PostConstruct;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityContextStrategyConfig {

    @PostConstruct
    public void init() {
        try {
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        } catch (Exception e) {
        }
    }
}
