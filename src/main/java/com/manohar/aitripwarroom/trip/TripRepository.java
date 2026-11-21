package com.manohar.aitripwarroom.trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    Optional<Trip> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
