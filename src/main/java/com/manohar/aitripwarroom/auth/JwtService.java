package com.manohar.aitripwarroom.auth;

import com.manohar.aitripwarroom.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration accessTokenTtl;

    public JwtService(
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl
    ) {
        if (accessTokenTtl.isNegative() || accessTokenTtl.isZero()
                || accessTokenTtl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("JWT access-token TTL must be greater than zero and no more than one hour");
        }

        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.accessTokenTtl = accessTokenTtl;
    }

    public IssuedAccessToken issueAccessToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ai-trip-war-room")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("platformRole", user.getPlatformRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedAccessToken(token, accessTokenTtl.toSeconds());
    }

    public record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
