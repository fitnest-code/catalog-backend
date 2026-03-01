/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springdoc.core.properties.SpringDocConfigProperties
 *  org.springdoc.webmvc.api.OpenApiWebMvcResource
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.context.event.ApplicationReadyEvent
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.context.event.EventListener
 *  org.springframework.scheduling.annotation.Async
 */
package az.fitnest.catalog.configuration;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@Configuration
public class OpenApiWarmupConfig {
    private final SpringDocConfigProperties springDocConfigProperties;
    private final OpenApiWebMvcResource openApiResource;
    @Value(value = "${server.port:8080}")
    private int serverPort;

    @Autowired
    public OpenApiWarmupConfig(SpringDocConfigProperties springDocConfigProperties, @Autowired(required = false) OpenApiWebMvcResource openApiResource) {
        this.springDocConfigProperties = springDocConfigProperties;
        this.openApiResource = openApiResource;
    }

    @EventListener(value = {ApplicationReadyEvent.class})
    @Async
    public void warmUpOpenApi() {
        if (!this.springDocConfigProperties.getApiDocs().isEnabled()) {
            return;
        }
        try {
            if (this.openApiResource != null) {
                try {
                    this.openApiResource.openapiJson(null, "", Locale.getDefault());
                    return;
                } catch (Exception exception) {
                    // empty catch block
                }
            }
            this.warmupViaHttp();
        } catch (Exception exception) {
            // empty catch block
        }
    }

    private void warmupViaHttp() {
        try {
            String apiDocsPath = this.springDocConfigProperties.getApiDocs().getPath();
            if (apiDocsPath == null || apiDocsPath.isEmpty()) {
                apiDocsPath = "/v3/api-docs";
            }
            URL url = new URL("http://localhost:" + this.serverPort + apiDocsPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                connection.getInputStream().readAllBytes();
            }
            connection.disconnect();
        } catch (Exception exception) {
            // empty catch block
        }
    }
}

