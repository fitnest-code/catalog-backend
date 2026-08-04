package az.fitnest.catalog.controller;

import az.fitnest.catalog.service.AppQrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/public/app-qr")
@RequiredArgsConstructor
@Tag(name = "App QR Public", description = "Public endpoints for mobile app download QR code scanning")
public class AppQrPublicController {

    private final AppQrCodeService appQrCodeService;

    @Operation(summary = "QR kod skan edildikdə sayğacı artır və cihaz növünə uyğun mağazaya yönləndir")
    @GetMapping("/scan/{mode}")
    public ResponseEntity<Void> scanAppQrCode(
            @PathVariable String mode,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {

        String targetUrl = appQrCodeService.incrementAndGetRedirectUrl(mode, userAgent);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .build();
    }
}
