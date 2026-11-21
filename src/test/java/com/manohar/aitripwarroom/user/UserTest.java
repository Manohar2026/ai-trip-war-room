package com.manohar.aitripwarroom.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {

    @Test
    void constructorNormalizesEmailAndDefaultsToRegularUser() {
        User user = new User("Manohar", "  MANOHAR@example.com  ", "encoded-password");

        assertEquals("manohar@example.com", user.getEmail());
        assertEquals(PlatformRole.USER, user.getPlatformRole());
        assertEquals("Manohar", user.getDisplayName());
    }
}
