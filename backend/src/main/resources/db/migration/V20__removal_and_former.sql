ALTER TABLE players ADD COLUMN exit_reason VARCHAR(40);
ALTER TABLE players ADD COLUMN exit_date DATE;
ALTER TABLE players ADD COLUMN exit_note VARCHAR(500);
ALTER TABLE players ADD COLUMN anonymized BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS deleted_users (
    id ${identity} PRIMARY KEY,
    user_id BIGINT,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(180),
    role_code VARCHAR(40),
    reason_code VARCHAR(40) NOT NULL,
    reason_text VARCHAR(500) NOT NULL,
    snapshot_json VARCHAR(4000),
    deleted_by VARCHAR(180),
    hard_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    restore_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS player_deletion_requests (
    id ${identity} PRIMARY KEY,
    player_id BIGINT NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    requested_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    has_history BOOLEAN NOT NULL,
    history_summary VARCHAR(2000),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS player_deletion_votes (
    id ${identity} PRIMARY KEY,
    request_id BIGINT NOT NULL,
    voter_user_id BIGINT NOT NULL,
    voter_role VARCHAR(40) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    decided_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_deletion_vote UNIQUE (request_id, voter_role)
);
