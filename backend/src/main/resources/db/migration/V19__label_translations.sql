ALTER TABLE club_roles ADD COLUMN source_lang VARCHAR(2) DEFAULT 'en';
ALTER TABLE competitions ADD COLUMN source_lang VARCHAR(2) DEFAULT 'en';
ALTER TABLE competitions ADD COLUMN name_fr VARCHAR(160);
ALTER TABLE competitions ADD COLUMN name_rw VARCHAR(160);
ALTER TABLE bonus_rules ADD COLUMN custom_source_lang VARCHAR(2);
ALTER TABLE bonus_rules ADD COLUMN custom_name_fr VARCHAR(80);
ALTER TABLE bonus_rules ADD COLUMN custom_name_rw VARCHAR(80);
