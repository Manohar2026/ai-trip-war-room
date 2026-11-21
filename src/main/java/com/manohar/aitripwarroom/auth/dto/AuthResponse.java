package com.manohar.aitripwarroom.auth.dto;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds,
        UserProfileResponse user
) {
}
