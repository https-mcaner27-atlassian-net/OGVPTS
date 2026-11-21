package edu.maltepe.ogvpts.controller;

import edu.maltepe.ogvpts.dto.AuthDtos.ChangePasswordRequest;
import edu.maltepe.ogvpts.dto.AuthDtos.LoginRequest;
import edu.maltepe.ogvpts.dto.AuthDtos.LoginResponse;
import edu.maltepe.ogvpts.dto.AuthDtos.MeResponse;
import edu.maltepe.ogvpts.security.CurrentUserProvider;
import edu.maltepe.ogvpts.service.AuthService;
import edu.maltepe.ogvpts.service.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "1. Giriş ve hesap", description = "FR-01, NFR-03")
public class AuthController {

    private final AuthService authService;
    private final PrivacyService privacyService;
    private final CurrentUserProvider currentUser;

    public AuthController(AuthService authService, PrivacyService privacyService, CurrentUserProvider currentUser) {
        this.authService = authService;
        this.privacyService = privacyService;
        this.currentUser = currentUser;
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Giriş yap (FR-01)", description = "Başarılıysa JWT ve kullanıcı bilgisi döner.")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/auth/me")
    @Operation(summary = "Giriş yapmış kullanıcının bilgisi")
    public MeResponse me() {
        return authService.me(currentUser.get());
    }

    @PutMapping("/auth/password")
    @Operation(summary = "Kendi şifreni değiştir")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(currentUser.get(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/data-deletion-request")
    @Operation(summary = "KVKK: kişisel verilerin silinmesini talep et (NFR-03)")
    public ResponseEntity<Void> dataDeletionRequest() {
        privacyService.requestDataDeletion(currentUser.get());
        return ResponseEntity.accepted().build();
    }
}
