package com.manohar.aitripwarroom.auth;

import com.manohar.aitripwarroom.auth.dto.AuthResponse;
import com.manohar.aitripwarroom.auth.dto.LoginRequest;
import com.manohar.aitripwarroom.auth.dto.RefreshTokenRequest;
import com.manohar.aitripwarroom.auth.dto.RegisterRequest;
import com.manohar.aitripwarroom.auth.dto.UserProfileResponse;
import com.manohar.aitripwarroom.user.User;
import com.manohar.aitripwarroom.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            Clock clock,
            @Value("${security.jwt.refresh-token-ttl}") Duration refreshTokenTtl
    ) {
        if (refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalArgumentException("Refresh-token TTL must be greater than zero");
        }

        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        validateBcryptPasswordLength(request.password());

        User user = new User(
                request.displayName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.save(user);
        return issueTokenPair(savedUser, UUID.randomUUID());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> invalidCredentials());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return issueTokenPair(user, UUID.randomUUID());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Instant now = clock.instant();
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidRefreshToken);

        if (!currentToken.isActiveAt(now)) {
            throw invalidRefreshToken();
        }

        currentToken.revokeAt(now);
        refreshTokenRepository.save(currentToken);

        return issueTokenPair(currentToken.getUser(), currentToken.getFamilyId());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revokeAt(clock.instant());
            refreshTokenRepository.save(token);
        });
    }

    private AuthResponse issueTokenPair(User user, UUID familyId) {
        JwtService.IssuedAccessToken accessToken = jwtService.issueAccessToken(user);

        String rawRefreshToken = generateRefreshToken();
        Instant refreshExpiresAt = clock.instant().plus(refreshTokenTtl);

        RefreshToken refreshToken = new RefreshToken(
                user,
                hashToken(rawRefreshToken),
                familyId,
                refreshExpiresAt
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                "Bearer",
                accessToken.value(),
                accessToken.expiresInSeconds(),
                rawRefreshToken,
                refreshTokenTtl.toSeconds(),
                new UserProfileResponse(
                        user.getId(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getPlatformRole()
                )
        );
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void validateBcryptPasswordLength(String password) {
        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes when using BCrypt"
            );
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid, expired or revoked");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
