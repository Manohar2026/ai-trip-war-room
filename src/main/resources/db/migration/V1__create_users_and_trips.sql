CREATE TABLE users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    platform_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_platform_role CHECK (platform_role IN ('USER', 'ADMIN'))
);

CREATE TABLE trips (
    id UUID PRIMARY KEY,
    creator_user_id UUID NOT NULL,
    leader_user_id UUID NOT NULL,
    destination VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_budget NUMERIC(12, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    invite_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFTING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trips_creator FOREIGN KEY (creator_user_id) REFERENCES users (id),
    CONSTRAINT fk_trips_leader FOREIGN KEY (leader_user_id) REFERENCES users (id),
    CONSTRAINT uk_trips_invite_code UNIQUE (invite_code),
    CONSTRAINT ck_trips_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_trips_total_budget CHECK (total_budget >= 0),
    CONSTRAINT ck_trips_currency_code CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_trips_status CHECK (status IN ('DRAFTING', 'LIVE', 'FINALIZED'))
);

CREATE TABLE trip_memberships (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trip_memberships_trip FOREIGN KEY (trip_id)
        REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_memberships_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_trip_memberships_trip_user UNIQUE (trip_id, user_id),
    CONSTRAINT ck_trip_memberships_role CHECK (role IN ('VIEWER', 'PLANNER', 'LEADER'))
);

CREATE INDEX idx_trips_destination_dates
    ON trips (destination, start_date, end_date);

CREATE INDEX idx_trip_memberships_user
    ON trip_memberships (user_id);
