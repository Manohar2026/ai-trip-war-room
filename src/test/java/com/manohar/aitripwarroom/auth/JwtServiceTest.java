package com.manohar.aitripwarroom.auth;

import com.manohar.aitripwarroom.user.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final Instant NOW = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    private static final SecretKey SECRET_KEY = new SecretKeySpec(
            "01234567890123456789012345678901".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "HmacSHA256"
    );

    @Test
    void accessTokenIsSignedAndContainsExpectedIdentityClaims() {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(SECRET_KEY));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(SECRET_KEY)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtService service = new JwtService(
                encoder,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(15)
        );

        UUID userId = UUID.randomUUID();
        User user = new User("Manohar", "manohar@example.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", userId);

        JwtService.IssuedAccessToken issued = service.issueAccessToken(user);
        Jwt decoded = decoder.decode(issued.value());

        assertEquals("manohar@example.com", decoded.getSubject());
        assertEquals(userId.toString(), decoded.getClaimAsString("userId"));
        assertEquals("USER", decoded.getClaimAsString("platformRole"));
        assertEquals("https://ai-trip-war-room.local", decoded.getIssuer().toString());
        assertEquals(NOW, decoded.getIssuedAt());
        assertEquals(NOW.plus(Duration.ofMinutes(15)), decoded.getExpiresAt());
        assertEquals(900, issued.expiresInSeconds());
    }

    @Test
    void accessTokenLifetimeCannotExceedOneHour() {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(SECRET_KEY));

        assertThrows(IllegalArgumentException.class, () -> new JwtService(
                encoder,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofHours(2)
        ));
    }
}
