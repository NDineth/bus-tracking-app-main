import { test } from 'node:test';
import assert from 'node:assert/strict';
import { drivingAlertDetails, sortTurns, turnReadings, graphBuckets } from '../utils/drivingMetrics.js';

test('legacy alerts show requested metrics without confusing acceleration with speed', () => {
  assert.deepEqual(drivingAlertDetails({ body: 'Harsh Braking: 3\nAggressive Acceleration: 8\nDangerous Turns: 2\nSafety Score: 47 / 100\nDriver Rating: HIGH' }),
    { highSpeed: null, braking: 3, turning: 2, score: 47, rating: 'HIGH' });
});
test('explicit zeroes remain zero and missing readings are unavailable', () => {
  assert.deepEqual(drivingAlertDetails({ highSpeedCount: 0, harshBrakingCount: 0, dangerousTurnCount: 0, safetyScore: 100, driverRating: 'LOW' }),
    { highSpeed: 0, braking: 0, turning: 0, score: 100, rating: 'LOW' });
  assert.equal(drivingAlertDetails({ body: 'Please drive safely' }).score, null);
});
test('turn picker scopes to the selected bus and prioritizes its active turn', () => {
  const turns = [{ id: 1, busId: 9, status: 'COMPLETED', startTime: '2026-10-18' },
    { id: 2, busId: 8, status: 'ACTIVE' }, { id: 3, busId: 9, status: 'ACTIVE', startTime: '2026-10-17' }];
  assert.deepEqual(sortTurns(turns, '9').map(turn => turn.id), [3, 1]);
  assert.equal(turns[0].id, 1);
});
test('graphs include the full assignment in time order without leaking another turn', () => {
  const logs = Array.from({ length: 150 }, (_, i) => ({ id: i, scheduleAssignmentId: 7,
    recordedAt: new Date(Date.UTC(2026, 9, 18, 8, 0, i)).toISOString(), speed: i === 0 ? null : 40,
    suddenBrakingCount: 0, dangerousTurnCount: 0, safetyScore: 100 })).reverse();
  logs.push({ scheduleAssignmentId: 8, recordedAt: '2026-10-18T12:00:00', speed: 200 });
  const result = turnReadings(logs, '7');
  assert.equal(result.length, 150);
  assert.equal(result[0].speed, null);
  assert.equal(result[0].suddenBrakingCount, 0);
  assert.equal(result.at(-1).id, 149);
});

test('graph intervals retain all events and preserve peak speed and lowest safety across the full turn', () => {
  const logs = Array.from({ length: 500 }, (_, i) => ({ recordedAt: new Date(Date.UTC(2026, 9, 18, 8, i)).toISOString(),
    speed: i === 499 ? 110 : 20, suddenBrakingCount: i % 7 === 0 ? 1 : 0, safetyScore: i === 0 ? 30 : 100 }));
  assert.equal(graphBuckets(logs, 'suddenBrakingCount', 'sum').reduce((sum, b) => sum + (b.value || 0), 0), 72);
  assert.equal(Math.max(...graphBuckets(logs, 'speed').map(b => b.value)), 110);
  assert.equal(Math.min(...graphBuckets(logs, 'safetyScore', 'min').map(b => b.value)), 30);
});
test('graph gaps and partial sensor batches remain missing instead of zero', () => {
  const readings = [{ recordedAt: '2026-10-18T08:00:00', safetyScore: 100 },
    { recordedAt: '2026-10-18T08:00:00', safetyScore: null },
    { recordedAt: '2026-10-18T09:00:00', safetyScore: 0 }];
  const result = graphBuckets(readings, 'safetyScore', 'min');
  assert.equal(result[0].value, null);
  assert.equal(result[1].value, null);
  assert.equal(result[2].value, 0);
});
