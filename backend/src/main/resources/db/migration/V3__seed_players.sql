INSERT INTO players (register_no, jersey_number, full_name, known_as, player_position, line_name, status, new_signing, signed_on, nationality, active) VALUES
(1, 1, 'Kwizera Jean-Marie', 'Jean Marie', 'GOALKEEPER', 'Goalkeepers', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(2, 2, 'Hitimana Mussa', 'Mussa', 'RIGHT_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(3, 3, 'Ishimwe Zaidu', 'Zaidu', 'LEFT_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(4, 4, 'Mariza Innocent', 'Mariza', 'CENTER_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(5, 5, 'Safari Gilbert', 'Sakade', 'CENTER_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(6, 6, 'Ishimwe Habyarimana Patrick', 'Pazzo', 'CENTRAL_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(7, 7, 'Kwizera James', 'James', 'OFFENSIVE_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(8, 8, 'Nkundineza Diogene', 'Keko', 'CENTRAL_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(9, 9, 'Mwizerwa Shimeyi', 'Shimeyi', 'FORWARD', 'Attackers', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(10, 9, 'Ntwali Ishimwe D''Amour', 'Damour', 'FORWARD', 'Attackers', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(11, 10, 'Kavatiri Dan', 'Kavatiri', 'OFFENSIVE_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(12, 12, 'Kagenza Thierry David', 'Kagenza', 'LEFT_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(13, 14, 'Enock Musinguzi', 'Enock', 'CENTRAL_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(14, 15, 'Mugisha Charles', 'Matic', 'DEFENSIVE_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(15, 16, 'Gikundiro Samuel', 'Samuel', 'GOALKEEPER', 'Goalkeepers', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(16, 17, 'Mucyo Kevin', 'KDB', 'FORWARD', 'Attackers', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(17, 18, 'Juvenal Martin', 'Juvenal', 'CENTER_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(18, 19, 'Kubwimana Olivier', 'Olivier', 'CENTER_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(19, 21, 'Mugisha Lewis', 'Lewis', 'OFFENSIVE_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(20, 22, 'Niyomukiza Faustin', 'Faustin', 'LEFT_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(21, 24, 'Tangishaka Hussein', 'Hussein', 'DEFENSIVE_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(22, 25, 'Tuyubahe Eric Fils', 'Eric Fils', 'WINGER', 'Attackers', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(23, 26, 'Kawaye Martin', 'Kawaye', 'GOALKEEPER', 'Goalkeepers', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(24, 27, 'Longalonga Ushindi', 'Esly', 'CENTER_BACK', 'Defenders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE),
(25, 30, 'Onuchukwu Captain', 'Captain', 'OFFENSIVE_MIDFIELDER', 'Midfielders', 'ACTIVE', TRUE, DATE '2026-09-20', 'Rwanda', TRUE);

INSERT INTO player_contracts (player_id, season_id, start_date, end_date, duration_years, duration_months, remaining_months, seasonal_pay, sf_exempt, advance_at_signing, release_clause_currency, release_clause_active, contract_status, active)
SELECT p.id, s.id, DATE '2026-09-28', DATE '2027-05-27', 0, 8, 7, v.pay, v.exempt, v.advance, 'RWF', FALSE, 'ACTIVE', TRUE
FROM seasons s
JOIN (
    SELECT 1 AS register_no, 240000 AS pay, FALSE AS exempt, 0 AS advance
    UNION ALL SELECT 2, 500000, FALSE, 75000
    UNION ALL SELECT 3, 480000, FALSE, 80000
    UNION ALL SELECT 4, 650000, FALSE, 0
    UNION ALL SELECT 5, 800000, FALSE, 150000
    UNION ALL SELECT 6, 320000, TRUE, 0
    UNION ALL SELECT 7, 500000, FALSE, 100000
    UNION ALL SELECT 8, 1300000, FALSE, 150000
    UNION ALL SELECT 9, 600000, FALSE, 100000
    UNION ALL SELECT 10, 400000, FALSE, 150000
    UNION ALL SELECT 11, 900000, FALSE, 75000
    UNION ALL SELECT 12, 550000, FALSE, 75000
    UNION ALL SELECT 13, 240000, FALSE, 0
    UNION ALL SELECT 14, 500000, FALSE, 100000
    UNION ALL SELECT 15, 400000, FALSE, 100000
    UNION ALL SELECT 16, 1500000, FALSE, 150000
    UNION ALL SELECT 17, 240000, FALSE, 0
    UNION ALL SELECT 18, 1200000, FALSE, 200000
    UNION ALL SELECT 19, 400000, FALSE, 150000
    UNION ALL SELECT 20, 800000, FALSE, 150000
    UNION ALL SELECT 21, 400000, FALSE, 80000
    UNION ALL SELECT 22, 400000, FALSE, 75000
    UNION ALL SELECT 23, 240000, FALSE, 0
    UNION ALL SELECT 24, 240000, FALSE, 0
    UNION ALL SELECT 25, 240000, FALSE, 0
) v ON 1 = 1
JOIN players p ON p.register_no = v.register_no
WHERE s.code = '2026/2027';

-- Four captains for 2026/2027. Names are a starting assignment the club can change.
INSERT INTO team_captains (season_id, player_id, captain_rank)
SELECT s.id, p.id, 'CAPTAIN' FROM seasons s, players p WHERE s.code = '2026/2027' AND p.register_no = 16;
INSERT INTO team_captains (season_id, player_id, captain_rank)
SELECT s.id, p.id, 'VICE_CAPTAIN' FROM seasons s, players p WHERE s.code = '2026/2027' AND p.register_no = 18;
INSERT INTO team_captains (season_id, player_id, captain_rank)
SELECT s.id, p.id, 'THIRD_CAPTAIN' FROM seasons s, players p WHERE s.code = '2026/2027' AND p.register_no = 8;
INSERT INTO team_captains (season_id, player_id, captain_rank)
SELECT s.id, p.id, 'FOURTH_CAPTAIN' FROM seasons s, players p WHERE s.code = '2026/2027' AND p.register_no = 5;

-- Staff pay figures are operational seed data. The workbook staff sheet was not in the repository.
INSERT INTO staff_members (full_name, role_name, department, email, status, active) VALUES
('Michel Nzeyimana', 'Head Coach', 'Technical', 'coach@vikingfc.rw', 'ACTIVE', TRUE),
('Thierry Niyonshuti', 'Assistant Coach', 'Technical', 'assistantcoach@vikingfc.rw', 'ACTIVE', TRUE),
('Samuel Bizimana', 'Goalkeeper Coach', 'Technical', 'gkcoach@vikingfc.rw', 'ACTIVE', TRUE),
('Diane Mukamana', 'Fitness Coach', 'Technical', 'diane.mukamana@vikingfc.rw', 'ACTIVE', TRUE),
('Jean Paul Habimana', 'Team Manager', 'Administration', 'assistantmanager@vikingfc.rw', 'ACTIVE', TRUE),
('Ardent Matabaro', 'Finance Manager', 'Administration', 'finance@vikingfc.rw', 'ACTIVE', TRUE),
('Rolande', 'Doctor', 'Medical', 'doctor@vikingfc.rw', 'ACTIVE', TRUE),
('Grace Ingabire', 'Physiotherapist', 'Medical', 'grace.ingabire@vikingfc.rw', 'ACTIVE', TRUE),
('Paul Habineza', 'Kit Manager', 'Support', 'paul.habineza@vikingfc.rw', 'ACTIVE', TRUE);

INSERT INTO staff_contracts (staff_id, season_id, start_date, end_date, duration_months, remaining_months, seasonal_pay, sf_exempt, advance_at_signing, contract_status, active)
SELECT m.id, s.id, DATE '2026-09-28', DATE '2027-05-27', 8, 7, v.pay, FALSE, 0, 'ACTIVE', TRUE
FROM seasons s
JOIN (
    SELECT 'coach@vikingfc.rw' AS email, 2400000 AS pay
    UNION ALL SELECT 'assistantcoach@vikingfc.rw', 1440000
    UNION ALL SELECT 'gkcoach@vikingfc.rw', 960000
    UNION ALL SELECT 'diane.mukamana@vikingfc.rw', 800000
    UNION ALL SELECT 'assistantmanager@vikingfc.rw', 720000
    UNION ALL SELECT 'finance@vikingfc.rw', 1200000
    UNION ALL SELECT 'doctor@vikingfc.rw', 1200000
    UNION ALL SELECT 'grace.ingabire@vikingfc.rw', 720000
    UNION ALL SELECT 'paul.habineza@vikingfc.rw', 480000
) v ON 1 = 1
JOIN staff_members m ON m.email = v.email
WHERE s.code = '2026/2027';
