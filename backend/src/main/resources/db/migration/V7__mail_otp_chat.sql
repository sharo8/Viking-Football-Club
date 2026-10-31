ALTER TABLE players ADD COLUMN preferred_language VARCHAR(8) DEFAULT 'en';
ALTER TABLE players ADD COLUMN emergency_contact_relationship VARCHAR(80);
ALTER TABLE players ADD COLUMN emergency_contact_email VARCHAR(180);
ALTER TABLE staff_members ADD COLUMN date_of_birth DATE;
ALTER TABLE app_users ADD COLUMN preferred_language VARCHAR(8) DEFAULT 'en';
ALTER TABLE app_users ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE app_users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE notifications ADD COLUMN payload VARCHAR(2000);

CREATE TABLE email_outbox (
    id ${identity} PRIMARY KEY,
    to_email VARCHAR(180) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body_html VARCHAR(8000) NOT NULL,
    template_name VARCHAR(80),
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    related_type VARCHAR(40),
    related_id VARCHAR(40),
    language_code VARCHAR(8),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE otp_codes (
    id ${identity} PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    used_at TIMESTAMP,
    ip_address VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE TABLE birthday_notification_log (
    id ${identity} PRIMARY KEY,
    player_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    notice_type VARCHAR(8) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_birthday_notice UNIQUE (player_id, year_number, notice_type),
    CONSTRAINT fk_bday_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE conversations (
    id ${identity} PRIMARY KEY,
    kind VARCHAR(20) NOT NULL,
    title VARCHAR(160) NOT NULL,
    player_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_conv_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE conversation_members (
    id ${identity} PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    muted BOOLEAN NOT NULL DEFAULT FALSE,
    last_read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_conv_member UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_member_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE TABLE chat_messages (
    id ${identity} PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT,
    body VARCHAR(4000) NOT NULL,
    reply_to_id BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES app_users (id)
);

CREATE TABLE player_messages (
    id ${identity} PRIMARY KEY,
    player_id BIGINT NOT NULL,
    sender_email VARCHAR(180) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    language_code VARCHAR(8) NOT NULL,
    status VARCHAR(20) NOT NULL,
    outbox_id BIGINT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_pmsg_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE notification_preferences (
    id ${identity} PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    in_app BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_pref UNIQUE (user_id, type),
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);
