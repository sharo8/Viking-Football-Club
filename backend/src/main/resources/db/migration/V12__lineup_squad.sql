ALTER TABLE club_settings ADD COLUMN IF NOT EXISTS lineup_default_squad INT NOT NULL DEFAULT 18;
ALTER TABLE club_settings ADD COLUMN IF NOT EXISTS squad_size_by_competition VARCHAR(2000);
UPDATE club_settings SET lineup_min_squad = 12 WHERE lineup_min_squad = 16;

ALTER TABLE lineup_versions ADD COLUMN IF NOT EXISTS squad_size INT;
ALTER TABLE lineup_versions ADD COLUMN IF NOT EXISTS squad_plan_json VARCHAR(800);

CREATE TABLE IF NOT EXISTS lineup_squad_members (
    id ${identity} PRIMARY KEY,
    lineup_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    line_code VARCHAR(8) NOT NULL,
    called BOOLEAN NOT NULL DEFAULT FALSE,
    bench_order INT,
    reason_not_called VARCHAR(40),
    override_reason VARCHAR(400),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lineup_squad_player UNIQUE (lineup_id, player_id),
    CONSTRAINT fk_lineup_squad_lineup FOREIGN KEY (lineup_id) REFERENCES lineup_versions (id),
    CONSTRAINT fk_lineup_squad_player FOREIGN KEY (player_id) REFERENCES players (id)
);

CREATE TABLE IF NOT EXISTS competition_squad_rules (
    id ${identity} PRIMARY KEY,
    competition VARCHAR(160) NOT NULL,
    squad_size INT NOT NULL,
    max_bench INT NOT NULL,
    min_goalkeepers INT NOT NULL DEFAULT 2,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_competition_squad UNIQUE (competition)
);
