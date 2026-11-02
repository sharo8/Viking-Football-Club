ALTER TABLE club_settings ADD COLUMN lineup_deadline_hours INT NOT NULL DEFAULT 48;
ALTER TABLE club_settings ADD COLUMN lineup_max_bench INT NOT NULL DEFAULT 9;
ALTER TABLE club_settings ADD COLUMN lineup_min_squad INT NOT NULL DEFAULT 16;
ALTER TABLE club_settings ADD COLUMN lineup_max_squad INT NOT NULL DEFAULT 23;
ALTER TABLE club_settings ADD COLUMN rating_coach_pct INT NOT NULL DEFAULT 70;
ALTER TABLE club_settings ADD COLUMN rating_base NUMERIC(3, 1) NOT NULL DEFAULT 6.0;
ALTER TABLE club_settings ADD COLUMN rating_goal NUMERIC(3, 1) NOT NULL DEFAULT 1.0;
ALTER TABLE club_settings ADD COLUMN rating_assist NUMERIC(3, 1) NOT NULL DEFAULT 0.7;
ALTER TABLE club_settings ADD COLUMN rating_clean NUMERIC(3, 1) NOT NULL DEFAULT 0.5;
ALTER TABLE club_settings ADD COLUMN rating_motm NUMERIC(3, 1) NOT NULL DEFAULT 0.5;
ALTER TABLE club_settings ADD COLUMN rating_yellow NUMERIC(3, 1) NOT NULL DEFAULT 0.3;
ALTER TABLE club_settings ADD COLUMN rating_red NUMERIC(3, 1) NOT NULL DEFAULT 2.0;
ALTER TABLE club_settings ADD COLUMN public_lineup BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE club_settings ADD COLUMN yellow_suspension INT NOT NULL DEFAULT 5;

ALTER TABLE lineup_versions ADD COLUMN deadline_at TIMESTAMP;
ALTER TABLE lineup_versions ADD COLUMN submitted_at TIMESTAMP;
ALTER TABLE lineup_versions ADD COLUMN submitted_by VARCHAR(120);
ALTER TABLE lineup_versions ADD COLUMN approved_at TIMESTAMP;
ALTER TABLE lineup_versions ADD COLUMN approved_by VARCHAR(120);
ALTER TABLE lineup_versions ADD COLUMN locked_at TIMESTAMP;
ALTER TABLE lineup_versions ADD COLUMN review_comment VARCHAR(800);
ALTER TABLE lineup_versions ADD COLUMN override_reason VARCHAR(400);
ALTER TABLE lineup_versions ADD COLUMN snapshot_json VARCHAR(4000);
ALTER TABLE lineup_versions ADD COLUMN editing_by VARCHAR(120);
ALTER TABLE lineup_versions ADD COLUMN editing_until TIMESTAMP;
ALTER TABLE lineup_versions ADD COLUMN opponent_formation VARCHAR(20);
ALTER TABLE lineup_versions ADD COLUMN opponent_names VARCHAR(2000);
ALTER TABLE lineup_versions ADD COLUMN meeting_at TIMESTAMP;
ALTER TABLE lineup_versions ADD COLUMN meeting_place VARCHAR(200);
ALTER TABLE lineup_versions ADD COLUMN kit_note VARCHAR(200);
ALTER TABLE lineup_versions ADD COLUMN travel_note VARCHAR(400);
ALTER TABLE lineup_versions ADD COLUMN callup_message VARCHAR(2000);
ALTER TABLE lineup_versions ADD COLUMN ratings_closed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE lineup_versions ADD COLUMN gk_suggestion_id BIGINT;

ALTER TABLE lineup_slots ADD COLUMN coordx INT;
ALTER TABLE lineup_slots ADD COLUMN coordy INT;

CREATE TABLE IF NOT EXISTS convocations (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    called BOOLEAN NOT NULL DEFAULT FALSE,
    response VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(40),
    reason_note VARCHAR(400),
    token VARCHAR(80),
    token_expires TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0,
    notified_at TIMESTAMP,
    read_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lineup_call UNIQUE (match_id, player_id),
    CONSTRAINT uq_lineup_call_token UNIQUE (token),
    CONSTRAINT fk_lineup_call_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_lineup_call_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE IF NOT EXISTS match_ratings (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    coach_rating NUMERIC(3, 1),
    system_score NUMERIC(3, 1),
    final_rating NUMERIC(3, 1),
    minutes_played INT NOT NULL DEFAULT 0,
    comment VARCHAR(400),
    rated_by VARCHAR(120),
    motm BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_match_rating UNIQUE (match_id, player_id),
    CONSTRAINT fk_rating_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_rating_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE IF NOT EXISTS lineup_transitions (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    revision INT,
    action VARCHAR(40) NOT NULL,
    actor VARCHAR(120),
    comment VARCHAR(800),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_transition_match FOREIGN KEY (match_id) REFERENCES matches (id)
);

CREATE TABLE IF NOT EXISTS lineup_flags (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    note VARCHAR(400) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_flag_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_flag_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE IF NOT EXISTS lineup_reminders (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lineup_reminder UNIQUE (match_id, kind),
    CONSTRAINT fk_reminder_match FOREIGN KEY (match_id) REFERENCES matches (id)
);
