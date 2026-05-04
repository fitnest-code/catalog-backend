package az.fitnest.catalog.controller;

import az.fitnest.catalog.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@RestController
@Slf4j
@RequestMapping(value = {"/api/v1/media"})
@Tag(name = "Media", description = "Media məzmununu yayımlamaq üçün ucluqlar")
public class MediaController {
    private final FileStorageService fileStorageService;

    public MediaController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "Media faylını yayımlayın", description = "Media faylını (şəkli) birbaşa yaddaşdan yayımlayır.")
    @GetMapping(value = {"/stream/{fsId}"})
    public ResponseEntity<StreamingResponseBody> streamFile(@Parameter(description = "Medianın fayl sistemi ID-si") @PathVariable String fsId) {
        log.info("[MediaController] stream request received for fsId={}", fsId);

        Authentication authentication = SecurityContextHolder.getContext() != null ? SecurityContextHolder.getContext().getAuthentication() : null;
        log.debug("[MediaController] authentication (pre-check) = {}", authentication);
        if (authentication == null || !authentication.isAuthenticated() || !hasAnyRole(authentication, "SUPER_ADMIN", "ADMIN", "USER")) {
            log.warn("[MediaController] access denied for fsId={} principal={}", fsId, authentication != null ? authentication.getPrincipal() : "<none>");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null) {
            log.info("[MediaController] current principal={}, authorities={}", securityContext.getAuthentication().getPrincipal(), securityContext.getAuthentication().getAuthorities());
        } else {
            log.warn("[MediaController] no authentication present when handling stream request for fsId={}", fsId);
        }

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(outputStream -> {
                    SecurityContext previous = SecurityContextHolder.getContext();
                    try {
                        SecurityContextHolder.setContext(securityContext);

                        fileStorageService.streamFileToOutput(fsId, outputStream);

                        try {
                            outputStream.flush();
                        } catch (java.io.IOException e) {
                            log.warn("[MediaController] flush failed for fsId={}", fsId, e);
                        }

                    } finally {
                        SecurityContextHolder.setContext(previous);
                    }
                });
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null) return false;
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) return false;

        return Arrays.stream(roles)
                .filter(Objects::nonNull)
                .anyMatch(role -> authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(auth -> auth.equals(role) || auth.equals("ROLE_" + role)));
    }
}
