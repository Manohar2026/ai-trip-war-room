package com.manohar.aitripwarroom.trip;

import com.manohar.aitripwarroom.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TripTest {

    @Test
    void newTripStartsInDraftingAndCreatorIsInitialLeader() {
        User creator = new User("Manohar", "manohar@example.com", "encoded-password");

        Trip trip = new Trip(
                creator,
                "Lisbon, Portugal",
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 14),
                new BigDecimal("1200.00"),
                "EUR",
                "AB-7Q2K"
        );

        assertEquals(TripStatus.DRAFTING, trip.getStatus());
        assertSame(creator, trip.getCreatorUser());
        assertSame(creator, trip.getLeaderUser());
        assertEquals(new BigDecimal("1200.00"), trip.getTotalBudget());
    }

    @Test
    void leadershipCanBeTransferredToAnotherUser() {
        User creator = new User("Creator", "creator@example.com", "encoded-password");
        User nextLeader = new User("Planner", "planner@example.com", "encoded-password");
        Trip trip = new Trip(
                creator,
                "Lisbon, Portugal",
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 14),
                new BigDecimal("1200.00"),
                "EUR",
                "AB-7Q2K"
        );

        trip.transferLeadershipTo(nextLeader);

        assertSame(nextLeader, trip.getLeaderUser());
    }
}
