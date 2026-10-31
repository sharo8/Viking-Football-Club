INSERT INTO seasons (code, name, start_date, end_date, current_season)
VALUES ('2026/2027', 'Season 2026/2027', DATE '2026-09-28', DATE '2027-05-27', TRUE);

INSERT INTO club_settings (
    season_id, club_name, motto, competition_name, currency_code,
    player_contract_months, player_first_phase_months, player_social_fund,
    staff_contract_months, staff_first_phase_months, staff_social_fund,
    attendance_on_track_pct, attendance_watch_pct,
    training_late_fine, training_late_minutes, training_absence_fine,
    match_late_fine, match_late_minutes, match_absence_fine,
    rule6_text, rule7_text, first_training_monday, max_substitutes, match_length_minutes
)
SELECT s.id, 'Viking FC', 'ONE CLUB | ONE FAMILY | ONE MISSION', '3rd Division, Kigali, Rwanda', 'RWF',
       8, 3, 12000,
       8, 3, 12000,
       80, 60,
       2000, 15, 4000,
       2000, 15, 4000,
       'Rule 6: more than 15 minutes late to training without a valid justification is fined 2,000 RWF.',
       'Rule 7: any absence without a valid justified reason is fined 4,000 RWF. Permission (E) is never fined.',
       DATE '2026-09-28', 9, 90
FROM seasons s WHERE s.code = '2026/2027';

INSERT INTO position_line_mappings (position_code, position_label, line_name) VALUES
('GOALKEEPER', 'Goalkeeper', 'Goalkeepers'),
('CENTER_BACK', 'Center-Back', 'Defenders'),
('LEFT_BACK', 'Left-Back', 'Defenders'),
('RIGHT_BACK', 'Right-Back', 'Defenders'),
('DEFENSIVE_MIDFIELDER', 'Defensive Midfielder', 'Midfielders'),
('CENTRAL_MIDFIELDER', 'Central Midfielder', 'Midfielders'),
('OFFENSIVE_MIDFIELDER', 'Offensive Midfielder', 'Midfielders'),
('WINGER', 'Winger', 'Attackers'),
('FORWARD', 'Forward', 'Attackers');

INSERT INTO salary_bands (label, min_amount, max_amount, sort_order) VALUES
('Under 300K', 0, 299999, 1),
('300K to 499K', 300000, 499999, 2),
('500K to 799K', 500000, 799999, 3),
('800K to 1.19M', 800000, 1199999, 4),
('1.2M and above', 1200000, 100000000, 5);

INSERT INTO staff_role_departments (role_name, department) VALUES
('Head Coach', 'Technical'),
('Assistant Coach', 'Technical'),
('Goalkeeper Coach', 'Technical'),
('Fitness Coach', 'Technical'),
('Team Manager', 'Administration'),
('Finance Manager', 'Administration'),
('Physiotherapist', 'Medical'),
('Doctor', 'Medical'),
('Kit Manager', 'Support'),
('Other', 'Support');

INSERT INTO fine_type_configs (code, label, default_amount, late_after_minutes, category, active) VALUES
('TRAINING_LATE', 'Late to training', 2000, 15, 'TRAINING', TRUE),
('TRAINING_ABSENCE', 'Unexcused training absence', 4000, NULL, 'TRAINING', TRUE),
('MATCH_LATE', 'Late to match', 2000, 15, 'MATCH', TRUE),
('MATCH_ABSENCE', 'Unexcused match absence', 4000, NULL, 'MATCH', TRUE),
('DISCIPLINE', 'Discipline', 0, NULL, 'OTHER', TRUE),
('OTHER', 'Other', 0, NULL, 'OTHER', TRUE);

INSERT INTO bonus_rules (season_id, bonus_type, amount, valid_from, valid_to, rule_active, description)
SELECT s.id, v.bonus_type, v.amount, DATE '2026-09-28', DATE '2027-05-27', TRUE, v.description
FROM seasons s
CROSS JOIN (
    SELECT 'APPEARANCE' AS bonus_type, 5000 AS amount, 'Global appearance prime' AS description
    UNION ALL SELECT 'STARTING_XI', 10000, 'Global starting XI prime'
    UNION ALL SELECT 'GOAL', 20000, 'Global goal prime'
    UNION ALL SELECT 'ASSIST', 10000, 'Global assist prime'
    UNION ALL SELECT 'CLEAN_SHEET', 15000, 'Global clean sheet prime'
    UNION ALL SELECT 'MATCH_WIN', 10000, 'Global win prime'
    UNION ALL SELECT 'MATCH_DRAW', 5000, 'Global draw prime'
    UNION ALL SELECT 'MAN_OF_THE_MATCH', 25000, 'Global man of the match prime'
) v
WHERE s.code = '2026/2027';

INSERT INTO opponents (name, short_name, city) VALUES
('RCK', 'RCK', 'Kigali'),
('Marines FC', 'MAR', 'Kigali'),
('Police FC', 'POL', 'Kigali'),
('Nyamirambo City', 'NYC', 'Kigali');

INSERT INTO matches (season_id, competition, round_label, matchday, opponent_id, kickoff, venue, home_match, status, goals_for, goals_against, stoppage_minutes)
SELECT s.id, '3rd Division, Kigali, Rwanda', 'Matchday 1', 1, o.id, TIMESTAMP '2026-10-31 15:00:00', 'Ejo Heza Stadium', TRUE, 'SCHEDULED', 0, 0, 0
FROM seasons s, opponents o WHERE s.code = '2026/2027' AND o.name = 'RCK';

INSERT INTO matches (season_id, competition, round_label, matchday, opponent_id, kickoff, venue, home_match, status, goals_for, goals_against, stoppage_minutes)
SELECT s.id, '3rd Division, Kigali, Rwanda', 'Matchday 2', 2, o.id, TIMESTAMP '2026-11-07 15:30:00', 'Nyamirambo', FALSE, 'SCHEDULED', 0, 0, 0
FROM seasons s, opponents o WHERE s.code = '2026/2027' AND o.name = 'Marines FC';
