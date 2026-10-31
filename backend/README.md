# Viking FC club management API

Season **2026/2027**. 3rd Division, Kigali, Rwanda. Motto: **ONE CLUB | ONE FAMILY | ONE MISSION**.

The API reproduces the player-finance workbook (payroll, attendance, fines) and adds squad, contracts, captains, matches, lineups, medical, bonuses, partners, transfers, media and role dashboards. Money is RWF, stored as `BigDecimal` with scale 0.

## Run in IntelliJ

1. Open the `backend` folder as a Maven project (JDK 21 or newer).
2. Run `rw.vikingfc.club.VikingFcApplication`. The default profile is `dev`.
3. Open Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). Use **Authorize** with `Bearer <accessToken>`.
4. H2 console (dev only): [http://localhost:8080/h2-console](http://localhost:8080/h2-console). JDBC URL `jdbc:h2:mem:vikingfc`, user `sa`, empty password.
5. Requests are also in `docs/vikingfc.http` and `docs/VikingFC.postman_collection.json`.

`mvn clean verify` runs the workbook tests and boots the application against in-memory H2.

## MySQL

The `dev` profile stays on in-memory H2 so tests run without a database. The `prod` profile uses MySQL.

In IntelliJ, edit the run configuration of `VikingFcApplication` and set **Active profiles** to `prod`. Also set `JWT_SECRET` to a private value of at least 32 characters (the dev placeholder is rejected on `prod`).

Default connection, overridable with `DATABASE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD`:

- Host `localhost`, port `3308`
- Database `vikingfc` (created if missing)
- User `root`

`docker compose up -d` starts MySQL 8 on the same port when you do not already have a server there.

## Demo logins

**Temporary password (every seeded account): `ChangeMe@2026`.**

`mustChangePassword` is true, so the first session can only open `POST /api/auth/change-password`, `GET /api/auth/me` and `POST /api/auth/logout`. Every other route answers **403** `PASSWORD_CHANGE_REQUIRED` until the password is changed. A wrong current password answers **400** `INVALID_CURRENT_PASSWORD` (not 401). The dev profile prints this password once in the startup log.

Restore a seeded account without restarting:

`POST /api/dev/users/reset-password` with `{ "email": "president@vikingfc.rw" }`. This route exists only on the `dev` profile.

Each person has a separate account. President, General Manager and Finance Manager share the authority `ROLE_EXECUTIVE`, so they can manage the same things. The audit log still shows which account acted.

| Email | Role |
|---|---|
| president@vikingfc.rw | President |
| generalmanager@vikingfc.rw | General Manager |
| finance@vikingfc.rw | Finance Manager |
| assistantmanager@vikingfc.rw | Assistant Manager |
| doctor@vikingfc.rw | Doctor |
| socialmedia@vikingfc.rw | Social media |
| marketing@vikingfc.rw | Marketing |
| coach@vikingfc.rw | Head coach |
| assistantcoach@vikingfc.rw | Assistant coach |
| gkcoach@vikingfc.rw | Goalkeeper coach |

An account locks for 15 minutes after 5 failed logins. `POST /api/auth/forgot-password` returns a reset token in the response because no mail server is configured. Change the password with `POST /api/auth/change-password` or `POST /api/auth/reset-password`.

## Workbook figures (verified by tests)

25 players. Season payroll **14,040,000**. Monthly wage bill **1,755,000**. Average **561,600**, median **480,000**. Advances **1,960,000** (17 players). Envelopes **3,041,000**. Social fund **576,000** (Pazzo exempt). Net balance due **8,463,000**. Paid to date **5,001,000** (35.6%). Day 1 and Day 2 attendance: squad **52%**, **15** players at risk, absence fines **52,000** across **9** players. Permission (`E`) is never fined. A late mark is fined 2,000 RWF only when lateness is over 15 minutes and not justified.

## Assumptions

- The original `.xlsx` is not in this repository. Player rows, attendance marks and the payroll rules come from the workbook specification. Staff seasonal pay is a planning assumption (all divisible by 8, advance 0) so the staff register can be calculated with the same rules. Club payroll on the executive dashboard is players plus staff; the player payroll total stays 14,040,000.
- Captains for 2026/2027: Mucyo Kevin (captain), Kubwimana Olivier (vice), Nkundineza Diogene (third), Safari Gilbert (fourth).
- Nationality defaults to Rwanda. Contracts run 28 Sep 2026 to 27 May 2027 (8 months). Release clauses stay empty until an executive sets them.
- Signing instalments (advance and envelope) are recorded as paid on the contract start date so statements match the workbook "paid to date". Months 4 to 8 stay unpaid. A player who owes the club (Damour, Lewis, 12,000 RWF each) is not given a negative instalment; the debt is the `owedByPlayer` figure.
- Global bonus amounts are planning defaults (goal 20,000, assist 10,000, and so on). A player-specific rule overrides the global amount. Bonus payouts are tracked on the bonus (`PENDING / APPROVED / PAID`) and are not mixed into salary instalments.
- Sample opponents and two scheduled fixtures are placeholders and have no result until a coach records one.
- Marketing user Aline Keza is a demo account. Access tokens last 8 hours. Refresh tokens last 14 days.
- Jersey 9 is shared by Shimeyi and Damour. The database does not reject it. `GET /api/players/data-warnings` reports it.
- Photos (jpg, png, webp, max 5 MB) are stored under `./uploads` and served from `/files/**`.
- The workbook importer expects header rows on sheets named `Player Register`, `Attendance` and `Sanctions`. It updates players it can match by name and does not invent rows it cannot recognise.

## Modules

`auth`, `user`, `season`, `settings`, `player`, `contract`, `finance`, `bonus`, `staff`, `training`, `attendance`, `sanction`, `match`, `lineup`, `medical`, `partner`, `transfer`, `media`, `marketing`, `dashboard`, `report`, `audit`, `common`.
