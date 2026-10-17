import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { runInNewContext } from 'node:vm';

const require = createRequire(import.meta.url);
const { transformSync } = require('@babel/core');

// Exercise the real service modules against a fake transport; no backend data is changed.
function service(name, api) {
  const source = readFileSync(new URL(`../services/${name}.js`, import.meta.url), 'utf8');
  const { code } = transformSync(source, { babelrc: false, configFile: false,
    plugins: [require.resolve('@babel/plugin-transform-modules-commonjs')] });
  const module = { exports: {} };
  runInNewContext(code, { module, exports: module.exports, require: path => {
    if (path === './api') return { __esModule: true, default: api };
    throw new Error(`Unexpected service dependency: ${path}`);
  } });
  return module.exports;
}

test('an empty bus API response is a valid unassigned driver state', async () => {
  const api = { get: async path => { assert.equal(path, '/buses'); return { data: [] }; } };
  assert.equal(await service('busService', api).getAssignedBusByDriverId(4), null);
});
test('bus service preserves the HTTP error for a useful retry message', async () => {
  const error = { isAxiosError: true, response: { status: 401 } };
  const api = { get: async () => { throw error; } };
  await assert.rejects(service('busService', api).getAssignedBusByDriverId(4), actual => actual === error);
});
test('actual bus responses are matched to the driver rather than a coincident bus ID', async () => {
  const api = { get: async () => ({ data: [
    { id: 4, driver: { id: 9 } }, { id: 12, driver: { id: 4, firstName: 'Test', lastName: 'Driver' } },
  ] }) };
  const result = await service('busService', api).getAssignedBusByDriverId('4');
  assert.equal(result.id, 12);
  assert.equal(result.assignedDriver, 'Test Driver');
});
test('an unexpected response is not presented as no bus assigned', async () => {
  const api = { get: async () => ({ data: '<html>Wrong server</html>' }) };
  await assert.rejects(service('busService', api).getAllBuses(), /unexpected bus list/);
});
test('registration uses the existing signup endpoint and returns its success response', async () => {
  const data = { firstName: 'Test', email: 'test@example.invalid', role: 'BUS_DRIVER' };
  const response = { success: true, message: 'User registered successfully' };
  const api = { post: async (path, payload) => {
    assert.equal(path, '/auth/signup'); assert.equal(payload, data); return { data: response };
  } };
  assert.equal(await service('authService', api).registerUser(data), response);
});
test('registration failures retain their server status and validation message', async () => {
  const error = { response: { status: 400, data: { message: 'Email Address already in use!' } } };
  const api = { post: async () => { throw error; } };
  await assert.rejects(service('authService', api).registerUser({}), actual => actual === error);
});
