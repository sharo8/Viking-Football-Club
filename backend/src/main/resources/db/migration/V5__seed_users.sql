-- Temporary password for every demo account: ChangeMe@2026
-- BCrypt strength 12. must_change_password forces a change after the first login.
INSERT INTO app_users (email, password_hash, full_name, role, active, must_change_password, failed_attempts) VALUES
('president@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Victor Jacker', 'PRESIDENT', TRUE, TRUE, 0),
('generalmanager@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Joseph Courbon', 'GENERAL_MANAGER', TRUE, TRUE, 0),
('finance@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Ardent Matabaro', 'FINANCE_MANAGER', TRUE, TRUE, 0),
('assistantmanager@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Jean Paul Habimana', 'ASSISTANT_MANAGER', TRUE, TRUE, 0),
('doctor@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Rolande', 'DOCTOR', TRUE, TRUE, 0),
('socialmedia@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Nadia Uwera', 'SOCIAL_MEDIA_MANAGER', TRUE, TRUE, 0),
('marketing@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Aline Keza', 'MARKETING_MANAGER', TRUE, TRUE, 0),
('coach@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Michel Nzeyimana', 'HEAD_COACH', TRUE, TRUE, 0),
('assistantcoach@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Thierry Niyonshuti', 'ASSISTANT_COACH', TRUE, TRUE, 0),
('gkcoach@vikingfc.rw', '$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq', 'Samuel Bizimana', 'GOALKEEPER_COACH', TRUE, TRUE, 0);

UPDATE staff_members SET user_id = (SELECT id FROM app_users WHERE email = 'coach@vikingfc.rw') WHERE email = 'coach@vikingfc.rw';
UPDATE staff_members SET user_id = (SELECT id FROM app_users WHERE email = 'assistantcoach@vikingfc.rw') WHERE email = 'assistantcoach@vikingfc.rw';
UPDATE staff_members SET user_id = (SELECT id FROM app_users WHERE email = 'gkcoach@vikingfc.rw') WHERE email = 'gkcoach@vikingfc.rw';
UPDATE staff_members SET user_id = (SELECT id FROM app_users WHERE email = 'assistantmanager@vikingfc.rw') WHERE email = 'assistantmanager@vikingfc.rw';
UPDATE staff_members SET user_id = (SELECT id FROM app_users WHERE email = 'finance@vikingfc.rw') WHERE email = 'finance@vikingfc.rw';
UPDATE staff_members SET user_id = (SELECT id FROM app_users WHERE email = 'doctor@vikingfc.rw') WHERE email = 'doctor@vikingfc.rw';
