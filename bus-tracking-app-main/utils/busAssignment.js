export function selectAssignedBus(buses, driverId, selectedBusId) {
  const assigned = buses.filter(bus => bus.driver?.id != null && String(bus.driver.id) === String(driverId));
  return assigned.find(bus => String(bus.id ?? bus.busId) === String(selectedBusId)) || assigned[0] || null;
}

// A missing/failed preference read must not prevent a server assignment from loading.
export async function loadAssignedBus({ driverId, fetchBuses, readSelectedBusId }) {
  const [buses, selectedBusId] = await Promise.all([
    fetchBuses(), Promise.resolve().then(readSelectedBusId).catch(() => null),
  ]);
  return selectAssignedBus(buses, driverId, selectedBusId);
}
