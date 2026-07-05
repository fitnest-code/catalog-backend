package az.fitnest.catalog.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public WebConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Register MappingJackson2HttpMessageConverter for APPLICATION_OCTET_STREAM
        // to allow Spring MVC to deserialize JSON from octet-stream request parts.
        MappingJackson2HttpMessageConverter octetStreamConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        octetStreamConverter.setSupportedMediaTypes(mediaTypes);
        converters.add(octetStreamConverter);
    }
}
