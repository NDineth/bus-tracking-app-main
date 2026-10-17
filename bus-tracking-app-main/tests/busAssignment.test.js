import { test } from 'node:test';
import assert from 'node:assert/strict';
import { loadAssignedBus, selectAssignedBus } from '../utils/busAssignment.js';

const buses = [{ id: 17, driver: { id: 4 } }, { id: 4, driver: { id: 9 } }, { id: 3, driver: null }];
test('matches the driver user ID even when API and stored ID types differ', () => {
  assert.equal(selectAssignedBus(buses, '4')?.id, 17);
});
test('a bus ID that coincides with the user ID is not an assignment', () => {
  assert.equal(selectAssignedBus([buses[1]], 4), null);
});
test('stale selections cannot select another driver’s bus', () => {
  assert.equal(selectAssignedBus(buses, 4, '4')?.id, 17);
});
test('a new driver without a bus gets an empty state', async () => {
  assert.equal(await loadAssignedBus({ driverId: 8, fetchBuses: async () => [], readSelectedBusId: async () => null }), null);
});
test('preference-storage failure does not break assignment loading or duplicate the API request', async () => {
  let calls = 0;
  const bus = await loadAssignedBus({ driverId: 4, fetchBuses: async () => { calls++; return buses; },
    readSelectedBusId: async () => { throw new Error('Storage unavailable'); } });
  assert.equal(bus.id, 17);
  assert.equal(calls, 1);
});
test('server failures are preserved for the retry message', async () => {
  const error = { isAxiosError: true, response: { status: 401 } };
  await assert.rejects(loadAssignedBus({ driverId: 4, fetchBuses: async () => { throw error; }, readSelectedBusId: async () => null }), actual => actual === error);
});
