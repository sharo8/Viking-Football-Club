UPDATE app_users
SET email = LOWER(TRIM(email))
WHERE email <> LOWER(TRIM(email));
