package com.manohar.aitripwarroom.trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripMembershipRepository extends JpaRepository<TripMembership, UUID> {

    Optional<TripMembership> findByTrip_IdAndUser_Id(UUID tripId, UUID userId);

    List<TripMembership> findAllByTrip_Id(UUID tripId);

    boolean existsByTrip_IdAndUser_Id(UUID tripId, UUID userId);
}
