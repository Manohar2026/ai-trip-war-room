package com.manohar.aitripwarroom.auth;

import com.manohar.aitripwarroom.auth.dto.AuthResponse;
import com.manohar.aitripwarroom.auth.dto.LoginRequest;
import com.manohar.aitripwarroom.auth.dto.RefreshTokenRequest;
import com.manohar.aitripwarroom.auth.dto.RegisterRequest;
import com.manohar.aitripwarroom.user.User;
import com.manohar.aitripwarroom.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-21T08:00:00Z");

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private Clock clock;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);

        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                clock,
                Duration.ofDays(7)
        );
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndReturnsTokenPair() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCase("manohar@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password-123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", userId);
            return saved;
        });
        when(jwtService.issueAccessToken(any(User.class)))
                .thenReturn(new JwtService.IssuedAccessToken("signed-access-token", 900));

        AuthResponse response = authService.register(new RegisterRequest(
                " Manohar ",
                " MANOHAR@example.com ",
                "strong-password-123"
        ));

        assertEquals(userId, response.user().id());
        assertEquals("manohar@example.com", response.user().email());
        assertEquals("Manohar", response.user().displayName());
        assertEquals("signed-access-token", response.accessToken());
        assertEquals(900, response.expiresInSeconds());
        assertNotNull(response.refreshToken());
        assertEquals(7 * 24 * 60 * 60, response.refreshTokenExpiresInSeconds());

        verify(passwordEncoder).encode("strong-password-123");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void registerRejectsAnAlreadyRegisteredEmail() {
        when(userRepository.existsByEmailIgnoreCase("manohar@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> authService.register(
                new RegisterRequest("Manohar", "MANOHAR@example.com", "strong-password-123")
        ));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginReturnsTokenPairForValidCredentials() {
        User user = userWithId(UUID.randomUUID());
        when(userRepository.findByEmailIgnoreCase("manohar@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("strong-password-123", "stored-hash")).thenReturn(true);
        when(jwtService.issueAccessToken(user))
                .thenReturn(new JwtService.IssuedAccessToken("signed-access-token", 900));

        AuthResponse response = authService.login(new LoginRequest(
                "MANOHAR@example.com",
                "strong-password-123"
        ));

        assertEquals(user.getId(), response.user().id());
        assertEquals("signed-access-token", response.accessToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginUsesSameErrorForUnknownEmailAndWrongPassword() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> authService.login(
                new LoginRequest("unknown@example.com", "wrong-password")
        ));
    }

    @Test
    void refreshRotatesTokenAndKeepsTokenFamily() throws Exception {
        User user = userWithId(UUID.randomUUID());
        String rawRefreshToken = "old-refresh-token-value";
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = new RefreshToken(
                user,
                sha256(rawRefreshToken),
                familyId,
                NOW.plus(Duration.ofDays(2))
        );

        when(refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken)))
                .thenReturn(Optional.of(existing));
        when(jwtService.issueAccessToken(user))
                .thenReturn(new JwtService.IssuedAccessToken("new-access-token", 900));

        AuthResponse response = authService.refresh(new RefreshTokenRequest(rawRefreshToken));

        assertNotEquals(rawRefreshToken, response.refreshToken());
        assertEquals(NOW, existing.getRevokedAt());
        ArgumentCaptor<RefreshToken> savedTokens = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(savedTokens.capture());

        assertEquals(existing, savedTokens.getAllValues().get(0));
        RefreshToken rotatedToken = savedTokens.getAllValues().get(1);
        assertEquals(familyId, rotatedToken.getFamilyId());
        assertNotEquals(sha256(rawRefreshToken), rotatedToken.getTokenHash());
    }

    @Test
    void refreshRejectsARevokedToken() throws Exception {
        User user = userWithId(UUID.randomUUID());
        String rawRefreshToken = "already-used-refresh-token";
        RefreshToken revoked = new RefreshToken(
                user,
                sha256(rawRefreshToken),
                UUID.randomUUID(),
                NOW.plus(Duration.ofDays(2))
        );
        revoked.revokeAt(NOW.minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken)))
                .thenReturn(Optional.of(revoked));

        assertThrows(ResponseStatusException.class, () -> authService.refresh(
                new RefreshTokenRequest(rawRefreshToken)
        ));
    }

    private User userWithId(UUID id) {
        User user = new User("Manohar", "manohar@example.com", "stored-hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
