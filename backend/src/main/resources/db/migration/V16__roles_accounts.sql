ALTER TABLE app_users ADD COLUMN role_code VARCHAR(40);
ALTER TABLE app_users ADD COLUMN last_login_at TIMESTAMP;
UPDATE app_users SET role_code = `role` WHERE role_code IS NULL;

ALTER TABLE staff_members ADD COLUMN listed_in VARCHAR(20) DEFAULT 'STAFF' NOT NULL;
UPDATE staff_members SET listed_in = 'ADMINISTRATION'
WHERE UPPER(role_name) LIKE '%FINANCE%'
   OR UPPER(role_name) LIKE '%PRESIDENT%'
   OR UPPER(role_name) LIKE '%GENERAL MANAGER%'
   OR UPPER(role_name) LIKE '%ASSISTANT MANAGER%'
   OR UPPER(role_name) LIKE '%MARKETING%'
   OR UPPER(role_name) LIKE '%SOCIAL MEDIA%';

ALTER TABLE otp_codes ADD COLUMN challenge_id VARCHAR(64);

ALTER TABLE players ADD COLUMN user_id BIGINT;

CREATE TABLE IF NOT EXISTS club_roles (
    id ${identity} PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    category VARCHAR(20) NOT NULL,
    name_en VARCHAR(80) NOT NULL,
    name_fr VARCHAR(80) NOT NULL,
    name_rw VARCHAR(80) NOT NULL,
    description VARCHAR(400),
    color VARCHAR(12) NOT NULL DEFAULT '#a3283a',
    otp_required BOOLEAN NOT NULL DEFAULT FALSE,
    system_role BOOLEAN NOT NULL DEFAULT TRUE,
    max_seats INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    permissions_json VARCHAR(4000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, max_seats, color)
SELECT 'PRESIDENT', 'ADMINISTRATION', 'President', 'Président', 'Perezida', TRUE, TRUE, 1, '#a3283a'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'PRESIDENT');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'GENERAL_MANAGER', 'ADMINISTRATION', 'General Manager', 'Directeur général', 'Umuyobozi mukuru', TRUE, TRUE, '#111111'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'GENERAL_MANAGER');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'FINANCE_MANAGER', 'ADMINISTRATION', 'Finance Manager', 'Responsable finances', 'Umuyobozi w''imari', TRUE, TRUE, '#1d4e89'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'FINANCE_MANAGER');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'ASSISTANT_MANAGER', 'ADMINISTRATION', 'Assistant Manager', 'Adjoint', 'Umuyobozi wungirije', FALSE, TRUE, '#5c3b3b'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'ASSISTANT_MANAGER');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'SOCIAL_MEDIA_MANAGER', 'ADMINISTRATION', 'Social Media Manager', 'Réseaux sociaux', 'Umuyobozi w''imbuga', FALSE, TRUE, '#2457a6'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'SOCIAL_MEDIA_MANAGER');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'MARKETING_MANAGER', 'ADMINISTRATION', 'Marketing Manager', 'Marketing', 'Umuyobozi w''kwamamaza', FALSE, TRUE, '#8a5a10'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'MARKETING_MANAGER');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'HEAD_COACH', 'STAFF', 'Head Coach', 'Entraîneur principal', 'Umutoza mukuru', TRUE, TRUE, '#a3283a'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'HEAD_COACH');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'ASSISTANT_COACH', 'STAFF', 'Assistant Coach', 'Entraîneur adjoint', 'Umutoza wungirije', FALSE, TRUE, '#1f6b3a'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'ASSISTANT_COACH');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'GOALKEEPER_COACH', 'STAFF', 'Goalkeeper Coach', 'Entraîneur des gardiens', 'Umutoza w''abazamu', FALSE, TRUE, '#1d4e89'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'GOALKEEPER_COACH');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'FITNESS_COACH', 'STAFF', 'Fitness Coach', 'Préparateur physique', 'Umutoza w''imbaraga', FALSE, TRUE, '#1f6b3a'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'FITNESS_COACH');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'DOCTOR', 'STAFF', 'Doctor', 'Médecin', 'Muganga', TRUE, TRUE, '#a3283a'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'DOCTOR');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'PHYSIOTHERAPIST', 'STAFF', 'Physiotherapist', 'Kinésithérapeute', 'Umuforomo w''imikino', FALSE, TRUE, '#2457a6'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'PHYSIOTHERAPIST');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'KIT_MANAGER', 'STAFF', 'Kit Manager', 'Intendant', 'Umuyobozi w''imyambaro', TRUE, TRUE, '#5c3b3b'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'KIT_MANAGER');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'TEAM_MANAGER', 'STAFF', 'Team Manager', 'Manager d''équipe', 'Umuyobozi w''ikipe', FALSE, TRUE, '#111111'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'TEAM_MANAGER');
INSERT INTO club_roles (code, category, name_en, name_fr, name_rw, otp_required, system_role, color)
SELECT 'PLAYER', 'PLAYER', 'Player', 'Joueur', 'Umukinnyi', FALSE, TRUE, '#a3283a'
WHERE NOT EXISTS (SELECT 1 FROM club_roles WHERE code = 'PLAYER');
