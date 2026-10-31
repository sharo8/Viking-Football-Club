INSERT INTO training_sessions (season_id, session_date, weekday, week_number, session_type, start_time, location, notes)
SELECT s.id, DATE '2026-10-01', 'THURSDAY', 1, 'PITCH', TIME '16:00:00', 'Viking FC Training Ground, Kigali', 'Day 1'
FROM seasons s WHERE s.code = '2026/2027';

INSERT INTO training_sessions (season_id, session_date, weekday, week_number, session_type, start_time, location, notes)
SELECT s.id, DATE '2026-10-02', 'FRIDAY', 1, 'PITCH', TIME '16:00:00', 'Viking FC Training Ground, Kigali', 'Day 2'
FROM seasons s WHERE s.code = '2026/2027';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'P', 0, FALSE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('Jean Marie', 'Sakade', 'Damour', 'Kagenza', 'Enock', 'Samuel', 'Lewis', 'Faustin', 'Eric Fils', 'Kawaye')
WHERE s.session_date = DATE '2026-10-01';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'A', 0, FALSE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('Mussa', 'Kavatiri')
WHERE s.session_date = DATE '2026-10-01';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'P', 0, FALSE, 'system'
FROM training_sessions s JOIN players p ON p.known_as = 'Hussein'
WHERE s.session_date = DATE '2026-10-01';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'A', 0, FALSE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('Mariza', 'Pazzo', 'James', 'Matic')
WHERE s.session_date = DATE '2026-10-01';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'E', 0, TRUE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('Zaidu', 'Keko', 'Esly')
WHERE s.session_date = DATE '2026-10-01';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'A', 0, FALSE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('Shimeyi', 'Olivier')
WHERE s.session_date = DATE '2026-10-01';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'P', 0, FALSE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('KDB', 'Captain')
WHERE s.session_date = DATE '2026-10-01';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'E', 0, TRUE, 'system'
FROM training_sessions s JOIN players p ON p.known_as = 'Juvenal'
WHERE s.session_date = DATE '2026-10-01';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'P', 0, FALSE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('Jean Marie', 'Sakade', 'Damour', 'Kagenza', 'Enock', 'Samuel', 'Lewis', 'Faustin', 'Eric Fils', 'Kawaye', 'Mussa', 'Kavatiri')
WHERE s.session_date = DATE '2026-10-02';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'A', 0, FALSE, 'system'
FROM training_sessions s JOIN players p ON p.known_as = 'Hussein'
WHERE s.session_date = DATE '2026-10-02';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'A', 0, FALSE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('Mariza', 'Pazzo', 'James', 'Matic')
WHERE s.session_date = DATE '2026-10-02';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'E', 0, TRUE, 'system'
FROM training_sessions s
JOIN players p ON p.known_as IN ('Zaidu', 'Keko', 'Esly', 'Shimeyi', 'Olivier', 'KDB', 'Captain')
WHERE s.session_date = DATE '2026-10-02';

INSERT INTO attendance_records (session_id, player_id, status, minutes_late, justified, recorded_by)
SELECT s.id, p.id, 'P', 0, FALSE, 'system'
FROM training_sessions s JOIN players p ON p.known_as = 'Juvenal'
WHERE s.session_date = DATE '2026-10-02';
