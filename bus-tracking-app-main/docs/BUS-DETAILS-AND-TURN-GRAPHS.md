# Alert details and full-turn driving graphs

Based on `ui-enhancement` at `189a7b597f23efd936f591b593cb5611c58242f3`.

## Changes

- Expanded driving alerts display high-speed event count, sudden braking count, dangerous turns, safety score and driver rating. Instructions and aggressive acceleration text are removed from the expanded view. Missing values display “Not provided”; old alerts do not contain high-speed counts.
- Tap the assigned bus header on the driver home screen to open bus details. Fleet users can tap the bus header or “Details & graphs”. The existing bus modal also links to graphs.
- Bus details show registration, route, driver, capacity, status and schedule access.
- Select an assignment from the last 30 days. The active turn is selected first, otherwise the latest turn. Graphs use the entire assignment-specific GPS history, without the bus endpoint's recent-100 limit.
- Active-turn graphs refresh every 30 seconds. Pull down or use Refresh to reload bus details and the turn list.

## Graph definitions

Every GPS reading for the selected assignment contributes to one of up to 32 time intervals. Tap a bar for its time and value.

| Graph | Data shown per interval |
| --- | --- |
| High speed | Maximum recorded speed in km/h. No overspeed limit is inferred; none is configured in this project. |
| Sudden braking | Sum of acceleration readings where X < -3.0. |
| Dangerous turns | Sum of acceleration readings where absolute Y > 2.5. |
| Safety score | Lowest sensor-batch safety score in the interval. |

Safety scores retain the existing backend formula: `max(0, 100 - 5 × braking - 4 × aggressive acceleration - 3 × dangerous turns)`. Aggressive acceleration uses X > 3.0 and remains part of the existing score calculation. The summary average is the mean of available scored batches, not a new trip-wide scoring formula.

Null or incomplete readings are never presented as zero events or a perfect score. A grey interval indicates missing or incomplete data. These counts represent sensor readings, matching the existing alert calculation; they are not deduplicated physical maneuvers.

## Backend integration

Deploy the included backend changes together with the frontend. No database schema changes or frontend dependencies are needed.

The existing GPS response now includes nullable `suddenBrakingCount`, `dangerousTurnCount` and `safetyScore`, reconstructed from the stored acceleration samples attached to each GPS log. This also makes historical readings available when their acceleration was retained.

- `GET /tracking/trips/history?busId={busId}` lists assignments, using the existing default last-30-day range.
- `GET /tracking/schedule-assignment/{assignmentId}/gps-logs` returns the complete selected turn.

Readings sent without an active assignment cannot be attributed to a turn and do not appear in its graphs. An older backend still supplies speed, but cannot supply the new acceleration metrics; those graphs display no data until the backend is updated.

## Validation

- 39 frontend tests passed, including legacy alert parsing, complete-assignment isolation, 500-reading graph aggregation and missing readings.
- Web export and Android bundle export passed.
- 12 backend tests passed: `DrivingMetricsTest`, `DrivingBehaviorServiceImplTest`, `AlertPolicyTest`, `AlertPolicyAuthorizationTest`.
- Browser smoke check with mocked API responses passed for assigned bus navigation, four charts, a 150-reading turn, switching assignments and instruction-free alert details. No browser runtime errors were observed.
- A physical device and live bus/sensor backend were not available for end-to-end verification.
