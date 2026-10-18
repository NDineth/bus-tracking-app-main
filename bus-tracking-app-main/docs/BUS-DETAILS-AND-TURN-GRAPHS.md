# Turn selection and driving metrics fixes

Based on `ui-enhancement` commit `773aadc696991c6ef402434d7e0df3b63a00835b`.

## Problems fixed

The expanded alert had a Dangerous acceleration row but the parser did not return its value, producing `undefined events`. The committed backend also omitted the acceleration metrics expected by the graphs, and behavior warnings did not include their triggering event count.

- A visible **Select turn** control opens a searchable list with daily turn numbers, dates, departure times, routes and statuses. The active turn is initially selected, otherwise the latest available turn. Cancelled and lost turns are excluded.
- Every graph uses the complete GPS history for the selected schedule assignment. Both bus and assignment identifiers are checked. Invalid timestamps are discarded. Switching turns hides previous-turn readings immediately; stale HTTP responses cannot replace the current selection.
- Dangerous acceleration is read from the existing `Aggressive Acceleration` notification text, its dangerous/sudden aliases, or structured count fields. Missing values remain unknown and zero remains zero.
- The backend GPS responses now return sudden braking, dangerous acceleration, dangerous turns and safety score from persisted acceleration batches. Dangerous acceleration has its own graph.
- New behavior warnings include the recorded triggering count, including `High Speed: N`. This is the warning cycle count, not a full-turn total. Historical high-speed warnings without a count show “Detected · count unavailable”. Counts are not guessed from the notification title or from acceleration.
- Expanded driving alerts remain data-only and include all six requested metrics.

## Graph definitions

All readings for the selected turn contribute to up to 32 time intervals.

| Graph | Value per interval |
| --- | --- |
| High speed | Peak recorded speed in km/h. This is the speed profile; no overspeed limit is configured in the project. |
| Sudden braking | Number of acceleration readings with X < -3.0. |
| Dangerous acceleration | Number of acceleration readings with X > 3.0. |
| Dangerous turns | Number of acceleration readings with absolute Y > 2.5. |
| Safety score | Lowest complete sensor-batch safety score. |

Safety uses the existing backend formula: `max(0, 100 - 5 × braking - 4 × acceleration - 3 × turning)`. The summary average is the mean of available scored batches. Counts describe sensor samples, not deduplicated physical maneuvers. Null and incomplete sensor batches do not become zero events or perfect scores; grey graph intervals indicate incomplete data.

## Apply and run

Apply both frontend and backend files. No new dependencies or database migration are needed. Restart the backend, then restart the frontend with `npx expo start -c`.

Existing API paths are retained:

- `GET /tracking/trips/history?busId={busId}`: available turns, defaulting to the last 30 days.
- `GET /tracking/schedule-assignment/{assignmentId}/gps-logs`: full turn, without the recent-100 limit.

GPS responses add nullable `suddenBrakingCount`, `dangerousAccelerationCount`, `dangerousTurnCount` and `safetyScore`. Historical samples can be graphed when their acceleration data was retained. GPS readings without a schedule assignment cannot be attributed to a turn. Updating only the frontend cannot supply metrics omitted by an old backend.

## Validation

- 43 frontend tests passed, including distinct speed/acceleration values, old and new high-speed warnings, zero/missing values, daily turn labels, complete-assignment isolation, and graph aggregation.
- 15 backend tests passed, including the actual GPS response mapping for a 150-reading turn, missing acceleration, boundary values, score formula and warning counts.
- Web and Android bundle exports passed.
- Mocked browser checks passed for searching/selecting turns, different speed/acceleration values per turn, stale-response isolation, both expanded alert counts and runtime errors.
- A live sensor/backend session and physical mobile device were not available for end-to-end verification.
