CREATE TABLE IF NOT EXISTS competitions (
    id ${identity} PRIMARY KEY,
    season_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    competition_type VARCHAR(20) NOT NULL,
    logo_url VARCHAR(400),
    official BOOLEAN NOT NULL DEFAULT TRUE,
    counts_for_table BOOLEAN NOT NULL DEFAULT FALSE,
    win_points INT NOT NULL DEFAULT 3,
    draw_points INT NOT NULL DEFAULT 1,
    loss_points INT NOT NULL DEFAULT 0,
    color VARCHAR(16) NOT NULL DEFAULT '#a3283a',
    squad_size INT NOT NULL DEFAULT 18,
    max_bench INT NOT NULL DEFAULT 9,
    min_goalkeepers INT NOT NULL DEFAULT 2,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    tie_breakers VARCHAR(80) NOT NULL DEFAULT 'POINTS,GD,GF',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_competition_name UNIQUE (season_id, name),
    CONSTRAINT fk_competition_season FOREIGN KEY (season_id) REFERENCES seasons (id)
);

CREATE TABLE IF NOT EXISTS competition_teams (
    id ${identity} PRIMARY KEY,
    competition_id BIGINT NOT NULL,
    opponent_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_competition_team UNIQUE (competition_id, opponent_id),
    CONSTRAINT fk_comp_team_competition FOREIGN KEY (competition_id) REFERENCES competitions (id),
    CONSTRAINT fk_comp_team_opponent FOREIGN KEY (opponent_id) REFERENCES opponents (id)
);

ALTER TABLE matches ADD COLUMN IF NOT EXISTS competition_id BIGINT;
ALTER TABLE matches ADD COLUMN IF NOT EXISTS stage VARCHAR(40);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS official BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE matches ADD COLUMN IF NOT EXISTS alert_hours INT NOT NULL DEFAULT 48;

INSERT INTO competitions (season_id, name, competition_type, official, counts_for_table, win_points, draw_points, loss_points, color, squad_size, max_bench, min_goalkeepers, archived, tie_breakers, created_at, updated_at, version)
SELECT DISTINCT season_id, competition, 'LEAGUE', TRUE, TRUE, 3, 1, 0, '#a3283a', 18, 9, 2, FALSE, 'POINTS,GD,GF', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM matches
WHERE NOT EXISTS (
    SELECT 1 FROM competitions c WHERE c.season_id = matches.season_id AND c.name = matches.competition
);

INSERT INTO competitions (season_id, name, competition_type, official, counts_for_table, win_points, draw_points, loss_points, color, squad_size, max_bench, min_goalkeepers, archived, tie_breakers, created_at, updated_at, version)
SELECT s.id, 'Friendly', 'FRIENDLY', FALSE, FALSE, 3, 1, 0, '#5c6570', 18, 9, 2, FALSE, 'POINTS,GD,GF', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM seasons s
WHERE NOT EXISTS (
    SELECT 1 FROM competitions c WHERE c.season_id = s.id AND c.competition_type = 'FRIENDLY'
);

UPDATE matches SET competition_id = (
    SELECT c.id FROM competitions c WHERE c.season_id = matches.season_id AND c.name = matches.competition
) WHERE competition_id IS NULL;

CREATE TABLE IF NOT EXISTS league_fixtures (
    id ${identity} PRIMARY KEY,
    competition_id BIGINT NOT NULL,
    matchday INT,
    stage VARCHAR(40),
    home_id BIGINT NOT NULL,
    away_id BIGINT NOT NULL,
    kickoff TIMESTAMP NOT NULL,
    venue VARCHAR(160),
    status VARCHAR(20) NOT NULL,
    goals_home INT NOT NULL DEFAULT 0,
    goals_away INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_fixture_competition FOREIGN KEY (competition_id) REFERENCES competitions (id),
    CONSTRAINT fk_fixture_home FOREIGN KEY (home_id) REFERENCES opponents (id),
    CONSTRAINT fk_fixture_away FOREIGN KEY (away_id) REFERENCES opponents (id)
);

ALTER TABLE standings ADD COLUMN IF NOT EXISTS competition_id BIGINT;
ALTER TABLE standings ADD COLUMN IF NOT EXISTS form_text VARCHAR(12);
ALTER TABLE standings ADD COLUMN IF NOT EXISTS club_row BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE standings ADD COLUMN IF NOT EXISTS source_kind VARCHAR(20) NOT NULL DEFAULT 'COMPUTED';

CREATE TABLE IF NOT EXISTS standing_adjustments (
    id ${identity} PRIMARY KEY,
    competition_id BIGINT NOT NULL,
    team_name VARCHAR(160) NOT NULL,
    points_delta INT NOT NULL DEFAULT 0,
    reason VARCHAR(400) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_standing_adjust UNIQUE (competition_id, team_name),
    CONSTRAINT fk_adjust_competition FOREIGN KEY (competition_id) REFERENCES competitions (id)
);

CREATE TABLE IF NOT EXISTS alert_log (
    id ${identity} PRIMARY KEY,
    match_id BIGINT NOT NULL,
    window_key VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(180) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_alert_window UNIQUE (match_id, window_key, recipient_email, channel),
    CONSTRAINT fk_alert_match FOREIGN KEY (match_id) REFERENCES matches (id)
);

ALTER TABLE convocations ADD COLUMN IF NOT EXISTS confirmed_by_coach_at TIMESTAMP;
ALTER TABLE convocations ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP;
ALTER TABLE convocations ADD COLUMN IF NOT EXISTS send_mode VARCHAR(20);
ALTER TABLE convocations ADD COLUMN IF NOT EXISTS list_version INT NOT NULL DEFAULT 1;
