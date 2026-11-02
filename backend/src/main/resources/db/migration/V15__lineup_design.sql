ALTER TABLE matches ADD COLUMN squad_size_override INT;

ALTER TABLE club_settings ADD COLUMN kit_primary VARCHAR(7) NOT NULL DEFAULT '#a3283a';
ALTER TABLE club_settings ADD COLUMN kit_secondary VARCHAR(7) NOT NULL DEFAULT '#ffffff';
ALTER TABLE club_settings ADD COLUMN kit_number_color VARCHAR(7) NOT NULL DEFAULT '#ffffff';
ALTER TABLE club_settings ADD COLUMN kit_gk_primary VARCHAR(7) NOT NULL DEFAULT '#1d4e89';
ALTER TABLE club_settings ADD COLUMN kit_away_primary VARCHAR(7) NOT NULL DEFAULT '#111111';
