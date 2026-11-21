package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.dto.AuthDtos.ChangePasswordRequest;
import edu.maltepe.ogvpts.dto.AuthDtos.LoginRequest;
import edu.maltepe.ogvpts.dto.AuthDtos.LoginResponse;
import edu.maltepe.ogvpts.dto.AuthDtos.MeResponse;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.repository.GroupMemberRepository;
import edu.maltepe.ogvpts.repository.UserRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import edu.maltepe.ogvpts.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-01: giriş, profil ve şifre değiştirme. */
@Service
public class AuthService {

    private final UserRepository users;
    private final GroupMemberRepository groupMembers;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository users, GroupMemberRepository groupMembers,
                       PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.groupMembers = groupMembers;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        // Güvenlik notu: e-posta mı şifre mi yanlış, ayırt etmeden aynı mesajı veriyoruz.
        User user = users.findByEmail(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                        "E-posta veya şifre hatalı."));
        return new LoginResponse(tokenService.issue(user), tokenService.getExpirationHours(), toMe(user));
    }

    @Transactional(readOnly = true)
    public MeResponse me(CurrentUser cu) {
        return toMe(users.findById(cu.id()).orElseThrow(() -> ApiException.notFound("Kullanıcı")));
    }

    @Transactional
    public void changePassword(CurrentUser cu, ChangePasswordRequest req) {
        User user = users.findById(cu.id()).orElseThrow(() -> ApiException.notFound("Kullanıcı"));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Mevcut şifre hatalı.");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }

    private MeResponse toMe(User u) {
        ProjectGroup group = groupMembers.findByUser(u.getId()).map(gm -> gm.getGroup()).orElse(null);
        return new MeResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                group == null ? null : group.getId(),
                group == null ? null : group.getName(),
                group == null || group.getAdvisor() == null ? null : group.getAdvisor().getFullName());
    }
}
