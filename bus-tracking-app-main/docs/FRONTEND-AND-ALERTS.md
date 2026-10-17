# Frontend and alert management

## Run locally

Use Node 22.13 or later and Expo Go for SDK 57. In `bus-tracking-app-main`:

```powershell
npm ci
Copy-Item .env.example .env
npm run web
```

Set `EXPO_PUBLIC_API_URL` in `.env` to the backend URL ending in `/api`. The backend runs on a separate machine at `http://172.20.10.2:8080/api`; this is also the frontend fallback when the environment variable is absent. For a physical phone, use the backend machine's reachable LAN address. `localhost` on the phone refers to the phone itself. Restart Expo with `npx expo start --clear` after changing this value.

Registration returns directly to the login screen after a successful response, with a confirmation message and the registered email filled in. No page refresh is needed. A new driver with no bus assigned sees a normal empty state. Server failures appear inline with retry controls and identify connection, permission or missing endpoint errors.

Start the updated Spring Boot backend with your configured PostgreSQL database. The existing `ddl-auto: update` setting creates the `alert_policy` table and adds nullable `safety_score` and `severity` notification columns. Review schema changes using your usual deployment process before production rollout.

Web development origins allowed by the backend default to `http://localhost:8081`, `http://localhost:4173` and `http://localhost:19006`. Set Spring property `app.cors.allowed-origins` (environment variable `APP_CORS_ALLOWED_ORIGINS`) to a comma-separated list for other frontend origins. Native clients do not use browser CORS.

## Workspaces

- **Overview:** fleet and availability summaries, bus/route/driver search, fleet filters, 30-day financial breakdowns, recent notifications and the latest scored alert.
- **Drivers:** assigned bus, start/stop driving, today's running schedule, three-day agenda, daily net report, bus browsing and approval requests.
- **Owners:** owned fleet, schedule and agenda links, financial reports, emergency/schedule requests and request history.
- **Alerts:** the 20 most recent notifications, unread and severity filters, search, newest/priority sorting, expandable event details, score bars and individual/bulk read acknowledgement.
- **Settings:** account sign-out, current policy, an interactive score preview and administrator policy editing. Other roles have read-only threshold access.

Schedules and request history include summary counts. Data failures are shown with retry controls where available. A read acknowledgement does not resolve a safety incident. Notifications poll every 30 seconds while the app is active; this implementation does not add operating-system push notifications.

## Safety score policy

Braking notifications show severity text and guidance in the preview. Tap a notification to expand a scrollable details area with its numeric X value, event count (when provided), safety score and other event data. Braking severity is independent of the overall alert/safety-score severity.

| Braking X | Braking severity |
| --- | --- |
| `-2 < X <= 0` | Safe |
| `-3.5 < X <= -2` | Moderate |
| `-5 < X <= -3.5` | Harsh |
| `-7 <= X <= -5` | Severe |
| `X < -7` | Critical |

Shared boundaries enter the stronger band, except -7 remains Severe because Critical is strictly below -7. The frontend can read explicit `brakingValueX`, `brakingX`, `accelerationX`, `acceleration.x` or `x` fields on a braking notification, or an explicitly labeled X value in its message. Older notifications that supply only a harsh-braking count cannot be classified by X: the preview says severity is unavailable, and expanded details show the count and mark X as not provided. The backend has not been changed to add readings.

Higher scores mean safer driving. The default bands are:

| Score | Severity |
| --- | --- |
| 0–39 | Critical |
| 40–69 | High risk |
| 70–89 | Caution |
| 90–100 | Low risk |

The boundaries are exclusive: a score of 40 enters high risk, 70 enters caution, and 90 enters low risk. Administrators can save fleet-wide boundaries in Settings. All values must be integers with `0 < criticalBelow < highRiskBelow < cautionBelow <= 100`.

The backend retains the existing per-sensor-batch formula:

```text
score = max(0, 100 - 5 × harshBrakingReadings
                   - 4 × aggressiveAccelerationReadings
                   - 3 × dangerousTurningReadings)
```

Scored notifications are generated below the caution boundary. Logs are retained even when no scored notification is sent. A displayed score describes the latest scored notification, not a trip average, a current driver rating or a fleet-wide score.

Behavior count warnings are separate. The frontend loads high speed, braking, acceleration and turning counts from the saved fleet policy without numeric fallback values. Missing or unavailable counts display as empty fields. Administrators can set each count from 1 to 100; all counts are required before saving. Resetting score thresholds leaves event counts unchanged. Crash detection always triggers an immediate critical warning. Existing event counters remain per-backend-process memory: they start a new counting cycle after a warning, reset on restart, and are not shared across server replicas.

Policy changes affect future events. New notifications store their score and severity so historical classifications remain stable. Older notifications without these fields use the score in their message and the currently loaded policy as a compatibility fallback. If policy loading fails, the app explicitly labels default/last-loaded values.

## Backend contract

- `GET /api/alert-policy`: authenticated users can read the active policy; defaults are returned before the first saved policy.
- `PUT /api/alert-policy`: administrator only; saves the singleton policy in PostgreSQL.
- Existing notification responses now include optional `safetyScore` and `severity`.
- Notification reads and acknowledgements are scoped to the signed-in recipient (administrators can access other recipients). PATCH uses `notificationId`, not the recipient row `id`.
- Driving status events now pass through one persistence path to avoid duplicate log entries.

Example policy body:

```json
{
  "criticalBelow": 40,
  "highRiskBelow": 70,
  "cautionBelow": 90,
  "highSpeedEvents": 3,
  "brakingEvents": 2,
  "accelerationEvents": 2,
  "turningEvents": 2
}
```

## Verification

```powershell
npm test
npm run build
npx expo export --platform android --output-dir dist-android
```

Backend unit tests can run without connecting to the application's database or IoT integration:

```powershell
.\mvnw.cmd '-Dtest=AlertPolicyTest,DrivingBehaviorServiceImplTest,AlertPolicyAuthorizationTest,NotificationServiceImplTest,ApprovalServiceImplTest,ScheduleAssignmentServiceTest' test
```

Manual verification with your local backend:

1. Sign in as an owner and check fleet search, availability filters, financial reports and approval links.
2. Sign in as a driver and check scheduled departure gating, start/stop, schedule, agenda and daily net report.
3. Review scored notifications at 35, 65 and 85, filter by severity, and mark individual/recent notifications read. Refresh and confirm read states persist.
4. As an administrator, edit and save boundaries; reload and confirm persistence. Invalid ordering and zero event counts must be rejected.
5. Confirm non-administrators can preview scores but cannot change the policy, and inspect narrow phone and desktop widths.

No live database/IoT end-to-end run or visual browser inspection was performed in this session. Production bundles and the targeted automated tests are the verification evidence.
