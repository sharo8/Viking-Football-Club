ALTER TABLE training_sessions ADD COLUMN audience VARCHAR(20) DEFAULT 'PLAYERS' NOT NULL;
ALTER TABLE training_sessions ADD COLUMN kind VARCHAR(20) DEFAULT 'TRAINING' NOT NULL;
ALTER TABLE training_sessions ADD COLUMN desk_status VARCHAR(20) DEFAULT 'OPEN' NOT NULL;
ALTER TABLE training_sessions ADD COLUMN match_id BIGINT;
ALTER TABLE training_sessions ADD COLUMN closed_by VARCHAR(160);
ALTER TABLE training_sessions ADD COLUMN closed_at TIMESTAMP;

ALTER TABLE training_sessions ADD CONSTRAINT fk_session_match FOREIGN KEY (match_id) REFERENCES matches (id);

ALTER TABLE attendance_records ADD COLUMN staff_id BIGINT;
ALTER TABLE attendance_records ADD COLUMN person_type VARCHAR(20) DEFAULT 'PLAYER' NOT NULL;
ALTER TABLE attendance_records ADD COLUMN arrival_time TIME;
ALTER TABLE attendance_records ADD COLUMN recorded_at TIMESTAMP;
ALTER TABLE attendance_records ADD COLUMN modified_by VARCHAR(160);
ALTER TABLE attendance_records ADD COLUMN modified_at TIMESTAMP;
ALTER TABLE attendance_records ADD COLUMN edit_reason VARCHAR(400);
ALTER TABLE attendance_records MODIFY player_id BIGINT NULL;
ALTER TABLE attendance_records ADD CONSTRAINT fk_att_staff FOREIGN KEY (staff_id) REFERENCES staff_members (id);

CREATE UNIQUE INDEX uq_attendance_staff ON attendance_records (session_id, staff_id);

CREATE TABLE IF NOT EXISTS attendance_record_history (
    id ${identity} PRIMARY KEY,
    record_id BIGINT NOT NULL,
    old_status VARCHAR(2),
    new_status VARCHAR(2) NOT NULL,
    minutes_late INT,
    reason VARCHAR(400),
    changed_by VARCHAR(160) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_att_hist FOREIGN KEY (record_id) REFERENCES attendance_records (id)
);

CREATE INDEX idx_att_hist_record ON attendance_record_history (record_id);

CREATE TABLE IF NOT EXISTS attendance_justifications (
    id ${identity} PRIMARY KEY,
    person_type VARCHAR(20) NOT NULL,
    person_id BIGINT NOT NULL,
    session_id BIGINT,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    evidence_name VARCHAR(200),
    submitted_by VARCHAR(160),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_att_just_session FOREIGN KEY (session_id) REFERENCES training_sessions (id)
);

CREATE INDEX idx_att_just_person ON attendance_justifications (person_type, person_id);

UPDATE club_settings SET match_late_fine = 5000, match_absence_fine = 5000 WHERE match_late_fine = 2000;
UPDATE fine_type_configs SET default_amount = 5000 WHERE code IN ('MATCH_LATE', 'MATCH_ABSENCE');
