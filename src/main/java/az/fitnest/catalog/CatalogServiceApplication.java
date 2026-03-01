/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.data.jpa.repository.config.EnableJpaAuditing
 *  org.springframework.kafka.annotation.EnableKafka
 *  org.springframework.scheduling.annotation.EnableAsync
 */
package az.fitnest.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
@EnableKafka
@EnableJpaRepositories(basePackages = "az.fitnest.catalog.repository")
public class CatalogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, (String[]) args);
    }
}

