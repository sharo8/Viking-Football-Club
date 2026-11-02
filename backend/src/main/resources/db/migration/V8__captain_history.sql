ALTER TABLE players ADD COLUMN address VARCHAR(200);
ALTER TABLE team_captains ADD COLUMN started_at TIMESTAMP;
ALTER TABLE team_captains ADD COLUMN ended_at TIMESTAMP;
CREATE INDEX idx_captain_season ON team_captains (season_id);
CREATE INDEX idx_captain_player ON team_captains (player_id);
ALTER TABLE team_captains DROP CONSTRAINT uq_captain_rank;
ALTER TABLE team_captains DROP CONSTRAINT uq_captain_player;
