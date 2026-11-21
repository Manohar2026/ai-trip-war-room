package com.manohar.aitripwarroom.trip;

import com.manohar.aitripwarroom.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "trips",
        uniqueConstraints = @UniqueConstraint(name = "uk_trips_invite_code", columnNames = "invite_code")
)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creatorUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_user_id", nullable = false)
    private User leaderUser;

    @Column(nullable = false, length = 120)
    private String destination;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "invite_code", nullable = false, length = 20)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status = TripStatus.DRAFTING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Trip() {
        // Required by JPA.
    }

    public Trip(
            User creatorUser,
            String destination,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalBudget,
            String currencyCode,
            String inviteCode
    ) {
        this.creatorUser = creatorUser;
        this.leaderUser = creatorUser;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalBudget = totalBudget;
        this.currencyCode = currencyCode;
        this.inviteCode = inviteCode;
        this.status = TripStatus.DRAFTING;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public void transferLeadershipTo(User newLeader) {
        this.leaderUser = newLeader;
    }

    public void changeStatus(TripStatus newStatus) {
        this.status = newStatus;
    }

    public UUID getId() {
        return id;
    }

    public User getCreatorUser() {
        return creatorUser;
    }

    public User getLeaderUser() {
        return leaderUser;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public TripStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
