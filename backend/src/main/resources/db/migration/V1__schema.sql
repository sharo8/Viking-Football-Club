-- Viking FC club schema. Money columns are NUMERIC(14,0) RWF.
-- Identifiers are unquoted so H2 (dev) and MySQL (prod) keep them lower case.

CREATE TABLE app_users (
    id ${identity} PRIMARY KEY,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    `role` VARCHAR(40) NOT NULL,
    phone VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_app_users_email UNIQUE (email)
);

CREATE TABLE refresh_tokens (
    id ${identity} PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_hash VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_refresh_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE TABLE password_reset_tokens (
    id ${identity} PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_reset_hash UNIQUE (token_hash),
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE TABLE seasons (
    id ${identity} PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(80) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    current_season BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_seasons_code UNIQUE (code)
);

CREATE TABLE club_settings (
    id ${identity} PRIMARY KEY,
    season_id BIGINT NOT NULL,
    club_name VARCHAR(120) NOT NULL,
    motto VARCHAR(200) NOT NULL,
    competition_name VARCHAR(200) NOT NULL,
    currency_code VARCHAR(8) NOT NULL,
    player_contract_months INT NOT NULL,
    player_first_phase_months INT NOT NULL,
    player_social_fund NUMERIC(14, 0) NOT NULL,
    staff_contract_months INT NOT NULL,
    staff_first_phase_months INT NOT NULL,
    staff_social_fund NUMERIC(14, 0) NOT NULL,
    attendance_on_track_pct INT NOT NULL,
    attendance_watch_pct INT NOT NULL,
    training_late_fine NUMERIC(14, 0) NOT NULL,
    training_late_minutes INT NOT NULL,
    training_absence_fine NUMERIC(14, 0) NOT NULL,
    match_late_fine NUMERIC(14, 0) NOT NULL,
    match_late_minutes INT NOT NULL,
    match_absence_fine NUMERIC(14, 0) NOT NULL,
    rule6_text VARCHAR(800) NOT NULL,
    rule7_text VARCHAR(800) NOT NULL,
    first_training_monday DATE NOT NULL,
    max_substitutes INT NOT NULL,
    match_length_minutes INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_settings_season UNIQUE (season_id),
    CONSTRAINT fk_settings_season FOREIGN KEY (season_id) REFERENCES seasons (id)
);

CREATE TABLE position_line_mappings (
    id ${identity} PRIMARY KEY,
    position_code VARCHAR(40) NOT NULL,
    position_label VARCHAR(60) NOT NULL,
    line_name VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_position_code UNIQUE (position_code)
);

CREATE TABLE salary_bands (
    id ${identity} PRIMARY KEY,
    label VARCHAR(40) NOT NULL,
    min_amount NUMERIC(14, 0) NOT NULL,
    max_amount NUMERIC(14, 0) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE staff_role_departments (
    id ${identity} PRIMARY KEY,
    role_name VARCHAR(80) NOT NULL,
    department VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_staff_role UNIQUE (role_name)
);

CREATE TABLE fine_type_configs (
    id ${identity} PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    label VARCHAR(120) NOT NULL,
    default_amount NUMERIC(14, 0) NOT NULL,
    late_after_minutes INT,
    category VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_fine_code UNIQUE (code)
);

CREATE TABLE players (
    id ${identity} PRIMARY KEY,
    register_no INT NOT NULL,
    jersey_number INT,
    full_name VARCHAR(160) NOT NULL,
    known_as VARCHAR(80) NOT NULL,
    player_position VARCHAR(40) NOT NULL,
    line_name VARCHAR(40) NOT NULL,
    date_of_birth DATE,
    nationality VARCHAR(80),
    phone VARCHAR(40),
    email VARCHAR(180),
    preferred_foot VARCHAR(20),
    height_cm INT,
    weight_kg INT,
    photo_url VARCHAR(400),
    status VARCHAR(20) NOT NULL,
    new_signing BOOLEAN NOT NULL DEFAULT FALSE,
    signed_on DATE,
    emergency_contact_name VARCHAR(160),
    emergency_contact_phone VARCHAR(40),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_players_register UNIQUE (register_no)
);

CREATE TABLE player_photos (
    id ${identity} PRIMARY KEY,
    player_id BIGINT NOT NULL,
    photo_type VARCHAR(20) NOT NULL,
    url VARCHAR(400) NOT NULL,
    thumbnail_url VARCHAR(400),
    uploaded_by VARCHAR(120),
    taken_on DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_photo_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE player_contracts (
    id ${identity} PRIMARY KEY,
    player_id BIGINT NOT NULL,
    season_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration_years INT NOT NULL,
    duration_months INT NOT NULL,
    remaining_months INT NOT NULL,
    seasonal_pay NUMERIC(14, 0) NOT NULL,
    sf_exempt BOOLEAN NOT NULL DEFAULT FALSE,
    advance_at_signing NUMERIC(14, 0) NOT NULL DEFAULT 0,
    release_clause_amount NUMERIC(14, 0),
    release_clause_currency VARCHAR(8) NOT NULL DEFAULT 'RWF',
    release_clause_active BOOLEAN NOT NULL DEFAULT FALSE,
    market_value NUMERIC(14, 0),
    agent_name VARCHAR(160),
    agent_commission NUMERIC(14, 0),
    contract_status VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_player_season_contract UNIQUE (player_id, season_id),
    CONSTRAINT fk_pcon_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_pcon_season FOREIGN KEY (season_id) REFERENCES seasons (id)
);

CREATE TABLE team_captains (
    id ${identity} PRIMARY KEY,
    season_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    captain_rank VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_captain_rank UNIQUE (season_id, captain_rank),
    CONSTRAINT uq_captain_player UNIQUE (season_id, player_id),
    CONSTRAINT fk_cap_season FOREIGN KEY (season_id) REFERENCES seasons (id),
    CONSTRAINT fk_cap_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE staff_members (
    id ${identity} PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    role_name VARCHAR(80) NOT NULL,
    department VARCHAR(40) NOT NULL,
    phone VARCHAR(40),
    email VARCHAR(180),
    photo_url VARCHAR(400),
    user_id BIGINT,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_staff_user UNIQUE (user_id),
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE TABLE staff_contracts (
    id ${identity} PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    season_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration_months INT NOT NULL,
    remaining_months INT NOT NULL,
    seasonal_pay NUMERIC(14, 0) NOT NULL,
    sf_exempt BOOLEAN NOT NULL DEFAULT FALSE,
    advance_at_signing NUMERIC(14, 0) NOT NULL DEFAULT 0,
    contract_status VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_staff_season_contract UNIQUE (staff_id, season_id),
    CONSTRAINT fk_scon_staff FOREIGN KEY (staff_id) REFERENCES staff_members (id),
    CONSTRAINT fk_scon_season FOREIGN KEY (season_id) REFERENCES seasons (id)
);

CREATE TABLE payroll_instalments (
    id ${identity} PRIMARY KEY,
    player_id BIGINT,
    staff_id BIGINT,
    season_id BIGINT NOT NULL,
    kind VARCHAR(32) NOT NULL,
    due_date DATE NOT NULL,
    amount_due NUMERIC(14, 0) NOT NULL,
    amount_paid NUMERIC(14, 0) NOT NULL DEFAULT 0,
    amount_deducted NUMERIC(14, 0) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    payment_date DATE,
    method VARCHAR(30),
    reference_no VARCHAR(80),
    paid_by VARCHAR(120),
    receipt_number VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inst_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_inst_staff FOREIGN KEY (staff_id) REFERENCES staff_members (id),
    CONSTRAINT fk_inst_season FOREIGN KEY (season_id) REFERENCES seasons (id)
);

CREATE TABLE payments (
    id ${identity} PRIMARY KEY,
    instalment_id BIGINT NOT NULL,
    amount NUMERIC(14, 0) NOT NULL,
    method VARCHAR(30) NOT NULL,
    reference_no VARCHAR(80),
    paid_at TIMESTAMP NOT NULL,
    paid_by_user_id BIGINT,
    receipt_number VARCHAR(40),
    reversed BOOLEAN NOT NULL DEFAULT FALSE,
    reversed_at TIMESTAMP,
    reverse_reason VARCHAR(400),
    reversed_by VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_inst FOREIGN KEY (instalment_id) REFERENCES payroll_instalments (id),
    CONSTRAINT fk_payment_user FOREIGN KEY (paid_by_user_id) REFERENCES app_users (id)
);

CREATE TABLE bonus_rules (
    id ${identity} PRIMARY KEY,
    player_id BIGINT,
    season_id BIGINT,
    bonus_type VARCHAR(40) NOT NULL,
    amount NUMERIC(14, 0) NOT NULL,
    valid_from DATE,
    valid_to DATE,
    rule_active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rule_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_rule_season FOREIGN KEY (season_id) REFERENCES seasons (id)
);

CREATE TABLE bonuses_earned (
    id ${identity} PRIMARY KEY,
    player_id BIGINT NOT NULL,
    match_id BIGINT,
    rule_id BIGINT,
    bonus_type VARCHAR(40) NOT NULL,
    amount NUMERIC(14, 0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    approved_by VARCHAR(120),
    approved_at TIMESTAMP,
    payment_id BIGINT,
    manual_entry BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500),
    source_key VARCHAR(140),
    earned_on DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_bonus_source UNIQUE (source_key),
    CONSTRAINT fk_bonus_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_bonus_rule FOREIGN KEY (rule_id) REFERENCES bonus_rules (id),
    CONSTRAINT fk_bonus_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE TABLE opponents (
    id ${identity} PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    short_name VARCHAR(40),
    logo_url VARCHAR(400),
    city VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_opponent_name UNIQUE (name)
);

CREATE TABLE training_sessions (
    id ${identity} PRIMARY KEY,
    season_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    weekday VARCHAR(12) NOT NULL,
    week_number INT NOT NULL,
    session_type VARCHAR(20) NOT NULL,
    start_time TIME,
    location VARCHAR(200),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_session_season FOREIGN KEY (season_id) REFERENCES seasons (id)
);

CREATE TABLE attendance_records (
    id ${identity} PRIMARY KEY,
    session_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    status VARCHAR(2) NOT NULL,
    minutes_late INT,
    justified BOOLEAN NOT NULL DEFAULT FALSE,
    note VARCHAR(500),
    recorded_by VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_attendance UNIQUE (session_id, player_id),
    CONSTRAINT fk_att_session FOREIGN KEY (session_id) REFERENCES training_sessions (id),
    CONSTRAINT fk_att_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE matches (
    id ${identity} PRIMARY KEY,
    season_id BIGINT NOT NULL,
    competition VARCHAR(160) NOT NULL,
    round_label VARCHAR(80),
    matchday INT,
    opponent_id BIGINT NOT NULL,
    kickoff TIMESTAMP NOT NULL,
    venue VARCHAR(160),
    home_match BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    goals_for INT NOT NULL DEFAULT 0,
    goals_against INT NOT NULL DEFAULT 0,
    crowd_attendance INT,
    referee VARCHAR(160),
    notes VARCHAR(2000),
    stoppage_minutes INT NOT NULL DEFAULT 0,
    formation VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_match_season FOREIGN KEY (season_id) REFERENCES seasons (id),
    CONSTRAINT fk_match_opponent FOREIGN KEY (opponent_id) REFERENCES opponents (id)
);

ALTER TABLE bonuses_earned
    ADD CONSTRAINT fk_bonus_match FOREIGN KEY (match_id) REFERENCES matches (id);

CREATE TABLE lineup_versions (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    revision INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    formation VARCHAR(20),
    published_at TIMESTAMP,
    published_by VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lineup_revision UNIQUE (match_id, revision),
    CONSTRAINT fk_lineup_match FOREIGN KEY (match_id) REFERENCES matches (id)
);

CREATE TABLE lineup_slots (
    id ${identity} PRIMARY KEY,
    lineup_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    selection VARCHAR(20) NOT NULL,
    position_slot VARCHAR(40),
    sort_order INT NOT NULL,
    captain BOOLEAN NOT NULL DEFAULT FALSE,
    override_reason VARCHAR(400),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lineup_player UNIQUE (lineup_id, player_id),
    CONSTRAINT fk_slot_lineup FOREIGN KEY (lineup_id) REFERENCES lineup_versions (id),
    CONSTRAINT fk_slot_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE match_events (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    event_minute INT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    player_id BIGINT,
    related_player_id BIGINT,
    note VARCHAR(400),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_event_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_event_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_event_related FOREIGN KEY (related_player_id) REFERENCES players (id)
);

CREATE TABLE match_player_records (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    participation VARCHAR(20) NOT NULL,
    minutes_played INT NOT NULL DEFAULT 0,
    rating NUMERIC(4, 1),
    goals INT NOT NULL DEFAULT 0,
    assists INT NOT NULL DEFAULT 0,
    yellow_cards INT NOT NULL DEFAULT 0,
    red_cards INT NOT NULL DEFAULT 0,
    late_to_match BOOLEAN NOT NULL DEFAULT FALSE,
    minutes_late INT NOT NULL DEFAULT 0,
    participated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_match_player UNIQUE (match_id, player_id),
    CONSTRAINT fk_mpr_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_mpr_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE fines (
    id ${identity} PRIMARY KEY,
    player_id BIGINT,
    staff_id BIGINT,
    fine_type VARCHAR(40) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    training_session_id BIGINT,
    attendance_record_id BIGINT,
    match_id BIGINT,
    fine_date DATE NOT NULL,
    week_number INT,
    minutes_late INT,
    justified BOOLEAN NOT NULL DEFAULT FALSE,
    amount NUMERIC(14, 0) NOT NULL,
    amount_paid NUMERIC(14, 0) NOT NULL DEFAULT 0,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    waived_by VARCHAR(120),
    waive_reason VARCHAR(400),
    auto_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_fine_attendance UNIQUE (attendance_record_id),
    CONSTRAINT fk_fine_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_fine_staff FOREIGN KEY (staff_id) REFERENCES staff_members (id),
    CONSTRAINT fk_fine_session FOREIGN KEY (training_session_id) REFERENCES training_sessions (id),
    CONSTRAINT fk_fine_attendance FOREIGN KEY (attendance_record_id) REFERENCES attendance_records (id),
    CONSTRAINT fk_fine_match FOREIGN KEY (match_id) REFERENCES matches (id)
);

CREATE TABLE fine_payments (
    id ${identity} PRIMARY KEY,
    fine_id BIGINT NOT NULL,
    amount NUMERIC(14, 0) NOT NULL,
    method VARCHAR(30) NOT NULL,
    reference_no VARCHAR(80),
    paid_at TIMESTAMP NOT NULL,
    received_by VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_finepay_fine FOREIGN KEY (fine_id) REFERENCES fines (id)
);

CREATE TABLE pay_deductions (
    id ${identity} PRIMARY KEY,
    fine_id BIGINT NOT NULL,
    instalment_id BIGINT NOT NULL,
    amount NUMERIC(14, 0) NOT NULL,
    reason VARCHAR(400),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_ded_fine FOREIGN KEY (fine_id) REFERENCES fines (id),
    CONSTRAINT fk_ded_inst FOREIGN KEY (instalment_id) REFERENCES payroll_instalments (id)
);

CREATE TABLE injuries (
    id ${identity} PRIMARY KEY,
    player_id BIGINT NOT NULL,
    injury_type VARCHAR(120) NOT NULL,
    diagnosis VARCHAR(500),
    body_part VARCHAR(80),
    side VARCHAR(20),
    severity VARCHAR(20) NOT NULL,
    cause VARCHAR(40) NOT NULL,
    match_id BIGINT,
    training_session_id BIGINT,
    injury_date DATE NOT NULL,
    expected_return_date DATE,
    status VARCHAR(20) NOT NULL,
    treatment_plan VARCHAR(2000),
    doctor_user_id BIGINT,
    confidential_notes VARCHAR(4000),
    cleared_on DATE,
    cleared_by VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inj_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_inj_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_inj_session FOREIGN KEY (training_session_id) REFERENCES training_sessions (id),
    CONSTRAINT fk_inj_doctor FOREIGN KEY (doctor_user_id) REFERENCES app_users (id)
);

CREATE TABLE medical_checks (
    id ${identity} PRIMARY KEY,
    injury_id BIGINT NOT NULL,
    check_date DATE NOT NULL,
    note VARCHAR(2000) NOT NULL,
    status_after VARCHAR(20),
    doctor_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_check_injury FOREIGN KEY (injury_id) REFERENCES injuries (id),
    CONSTRAINT fk_check_doctor FOREIGN KEY (doctor_user_id) REFERENCES app_users (id)
);

CREATE TABLE clearance_notes (
    id ${identity} PRIMARY KEY,
    injury_id BIGINT NOT NULL,
    note VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_clear_injury FOREIGN KEY (injury_id) REFERENCES injuries (id)
);

CREATE TABLE injury_attachments (
    id ${identity} PRIMARY KEY,
    injury_id BIGINT NOT NULL,
    file_url VARCHAR(400) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_attach_injury FOREIGN KEY (injury_id) REFERENCES injuries (id)
);

CREATE TABLE partners (
    id ${identity} PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    logo_url VARCHAR(400),
    category VARCHAR(30) NOT NULL,
    contact_name VARCHAR(160),
    contact_email VARCHAR(180),
    contact_phone VARCHAR(40),
    contract_start DATE,
    contract_end DATE,
    contract_value NUMERIC(14, 0) NOT NULL DEFAULT 0,
    benefits VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE partner_payments (
    id ${identity} PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    amount_due NUMERIC(14, 0) NOT NULL,
    amount_paid NUMERIC(14, 0) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    reference_no VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_ppay_partner FOREIGN KEY (partner_id) REFERENCES partners (id)
);

CREATE TABLE transfers (
    id ${identity} PRIMARY KEY,
    player_id BIGINT,
    player_name VARCHAR(160) NOT NULL,
    transfer_type VARCHAR(30) NOT NULL,
    from_club VARCHAR(160),
    to_club VARCHAR(160),
    fee NUMERIC(14, 0) NOT NULL DEFAULT 0,
    agent_fee NUMERIC(14, 0) NOT NULL DEFAULT 0,
    transfer_date DATE,
    contract_months INT,
    seasonal_pay NUMERIC(14, 0),
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_transfer_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE transfer_documents (
    id ${identity} PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    file_url VARCHAR(400) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tdoc_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id)
);

CREATE TABLE media_assets (
    id ${identity} PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    url VARCHAR(400) NOT NULL,
    thumbnail_url VARCHAR(400),
    tags VARCHAR(400),
    owner_id BIGINT,
    player_id BIGINT,
    match_id BIGINT,
    caption VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_media_owner FOREIGN KEY (owner_id) REFERENCES app_users (id),
    CONSTRAINT fk_media_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_media_match FOREIGN KEY (match_id) REFERENCES matches (id)
);

CREATE TABLE content_posts (
    id ${identity} PRIMARY KEY,
    platform VARCHAR(20) NOT NULL,
    caption VARCHAR(2000) NOT NULL,
    scheduled_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    owner_id BIGINT,
    match_id BIGINT,
    player_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_post_owner FOREIGN KEY (owner_id) REFERENCES app_users (id),
    CONSTRAINT fk_post_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_post_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE content_post_assets (
    id ${identity} PRIMARY KEY,
    post_id BIGINT NOT NULL,
    media_asset_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cpa_post FOREIGN KEY (post_id) REFERENCES content_posts (id),
    CONSTRAINT fk_cpa_media FOREIGN KEY (media_asset_id) REFERENCES media_assets (id)
);

CREATE TABLE campaigns (
    id ${identity} PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    budget NUMERIC(14, 0) NOT NULL DEFAULT 0,
    start_date DATE,
    end_date DATE,
    goal VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    results_note VARCHAR(2000),
    reach BIGINT,
    engagement BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE matchday_tasks (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    label VARCHAR(200) NOT NULL,
    done BOOLEAN NOT NULL DEFAULT FALSE,
    owner_role VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_task_match FOREIGN KEY (match_id) REFERENCES matches (id)
);

CREATE TABLE announcements (
    id ${identity} PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE announcement_audiences (
    announcement_id BIGINT NOT NULL,
    `role` VARCHAR(40) NOT NULL,
    CONSTRAINT pk_announcement_audience PRIMARY KEY (announcement_id, `role`),
    CONSTRAINT fk_aud_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id)
);

CREATE TABLE notifications (
    id ${identity} PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    type VARCHAR(40) NOT NULL,
    link VARCHAR(300),
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE TABLE audit_logs (
    id ${identity} PRIMARY KEY,
    actor_email VARCHAR(180),
    actor_role VARCHAR(40),
    action VARCHAR(40) NOT NULL,
    entity_name VARCHAR(80) NOT NULL,
    entity_id VARCHAR(40),
    before_json VARCHAR(4000),
    after_json VARCHAR(4000),
    ip_address VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE standings (
    id ${identity} PRIMARY KEY,
    season_id BIGINT NOT NULL,
    position_no INT NOT NULL,
    team_name VARCHAR(160) NOT NULL,
    played INT NOT NULL DEFAULT 0,
    won INT NOT NULL DEFAULT 0,
    drawn INT NOT NULL DEFAULT 0,
    lost INT NOT NULL DEFAULT 0,
    goals_for INT NOT NULL DEFAULT 0,
    goals_against INT NOT NULL DEFAULT 0,
    points INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_standing_team UNIQUE (season_id, team_name),
    CONSTRAINT fk_standing_season FOREIGN KEY (season_id) REFERENCES seasons (id)
);

CREATE INDEX idx_players_jersey ON players (jersey_number);
CREATE INDEX idx_players_position ON players (player_position);
CREATE INDEX idx_contracts_player ON player_contracts (player_id);
CREATE INDEX idx_attendance_player ON attendance_records (player_id);
CREATE INDEX idx_attendance_session ON attendance_records (session_id);
CREATE INDEX idx_fines_player ON fines (player_id);
CREATE INDEX idx_fines_week ON fines (week_number);
CREATE INDEX idx_inst_player ON payroll_instalments (player_id);
CREATE INDEX idx_inst_staff ON payroll_instalments (staff_id);
CREATE INDEX idx_inst_status ON payroll_instalments (status);
CREATE INDEX idx_matches_kickoff ON matches (kickoff);
CREATE INDEX idx_events_match ON match_events (match_id);
CREATE INDEX idx_mpr_player ON match_player_records (player_id);
CREATE INDEX idx_injuries_player ON injuries (player_id);
CREATE INDEX idx_notif_user ON notifications (user_id);
CREATE INDEX idx_audit_entity ON audit_logs (entity_name, entity_id);
