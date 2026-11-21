package com.manohar.aitripwarroom.auth.dto;

import com.manohar.aitripwarroom.user.PlatformRole;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String displayName,
        String email,
        PlatformRole platformRole
) {
}
