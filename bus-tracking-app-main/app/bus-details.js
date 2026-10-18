import React, { useEffect, useState } from 'react';
import { View, Text, ScrollView, RefreshControl, TouchableOpacity, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { getAllBuses } from '../services/busService';
import { getBusTurns, getTurnReadings } from '../services/trackingService';
import { errorMessage } from '../services/api';
import { sortTurns, turnReadings } from '../utils/drivingMetrics';
import { Button, EmptyState, ErrorNotice, Metric, SectionTitle, ui } from '../components/UI';
import TurnGraph from '../components/TurnGraph';
import Colors from '../constants/Colors';

const dateTime = value => value ? new Date(value).toLocaleString('en-LK', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—';

export default function BusDetailsScreen() {
  const { busId, busNumber } = useLocalSearchParams();
  const router = useRouter();
  const [bus, setBus] = useState(null);
  const [turns, setTurns] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [readings, setReadings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [graphLoading, setGraphLoading] = useState(false);
  const [error, setError] = useState(null);
  const [graphError, setGraphError] = useState(null);
  const [revision, setRevision] = useState(0);
  const [graphRevision, setGraphRevision] = useState(0);
  const selected = turns.find(turn => String(turn.id) === String(selectedId));
  const live = ['ACTIVE', 'IN_PROGRESS'].includes(selected?.status);

  useEffect(() => {
    let cancelled = false;
    setLoading(true); setError(null); setBus(null); setTurns([]);
    Promise.all([getAllBuses(), getBusTurns(busId)]).then(([buses, history]) => {
      if (cancelled) return;
      const found = buses.find(item => String(item.id) === String(busId));
      if (!found) throw new Error('This bus is no longer available.');
      const ordered = sortTurns(history, busId);
      setBus(found); setTurns(ordered);
      setSelectedId(previous => ordered.some(turn => String(turn.id) === String(previous)) ? previous : ordered[0]?.id ?? null);
    }).catch(e => { if (!cancelled) setError(errorMessage(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [busId, revision]);

  useEffect(() => {
    let cancelled = false;
    setReadings([]); setGraphError(null);
    if (!selectedId || !selected) { setGraphLoading(false); return; }
    setGraphLoading(true);
    getTurnReadings(selectedId).then(logs => { if (!cancelled) setReadings(turnReadings(logs, selectedId)); })
      .catch(e => { if (!cancelled) setGraphError(errorMessage(e)); })
      .finally(() => { if (!cancelled) setGraphLoading(false); });
    return () => { cancelled = true; };
  }, [busId, selectedId, selected, graphRevision]);

  useEffect(() => {
    if (!live) return;
    const interval = setInterval(() => setGraphRevision(value => value + 1), 30000);
    return () => clearInterval(interval);
  }, [live]);

  const speed = readings.map(reading => reading.speed).filter(value => value !== null);
  const score = readings.map(reading => reading.safetyScore).filter(value => value !== null);
  return <SafeAreaView style={ui.page}>
    <ScrollView contentContainerStyle={ui.content} refreshControl={<RefreshControl refreshing={loading} onRefresh={() => setRevision(value => value + 1)} tintColor={Colors.primary} />}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
        <TouchableOpacity accessibilityRole="button" accessibilityLabel="Back" onPress={() => router.back()} style={ui.linkButton}><Ionicons name="arrow-back" size={24} color={Colors.text} /></TouchableOpacity>
        <View style={{ flex: 1 }}><Text style={ui.eyebrow}>BUS DETAILS</Text><Text style={{ color: Colors.text, fontSize: 28, fontWeight: '800' }}>{bus?.busNumber || busNumber || 'Bus'}</Text></View>
        <Button title="Refresh" icon="refresh-outline" secondary onPress={() => setRevision(value => value + 1)} />
      </View>
      <ErrorNotice message={error} onRetry={() => setRevision(value => value + 1)} />
      {loading ? <ActivityIndicator color={Colors.primary} /> : bus && <>
        <View style={ui.card}><SectionTitle title={bus.route} subtitle={bus.status?.replace(/_/g, ' ')} />
          <Text style={ui.subtitle}>Assigned driver · {bus.assignedDriver || 'Unassigned'}</Text>
          <Text style={ui.subtitle}>Capacity · {bus.capacity ?? '—'} seats</Text>
          <Text style={ui.subtitle}>Next turn · {bus.nextTurn || 'Not scheduled'}</Text>
          {!!bus.unavailabilityReason && <Text style={ui.subtitle}>{bus.unavailabilityReason}</Text>}
          <Button title="View running schedule" icon="calendar-outline" secondary style={{ marginTop: 14 }} onPress={() => router.push({ pathname: '/bus-schedule', params: { busId: bus.id, busNumber: bus.busNumber, route: bus.route } })} />
        </View>
        <View><SectionTitle title="Full-turn driving graphs" subtitle="Select a turn from the last 30 days. All recorded readings for that turn are included." />
          <ScrollView horizontal showsHorizontalScrollIndicator contentContainerStyle={{ gap: 8, paddingBottom: 8 }}>
            {turns.map(turn => <TouchableOpacity key={turn.id} accessibilityRole="button" accessibilityState={{ selected: turn.id === selectedId }}
              onPress={() => setSelectedId(turn.id)} style={[ui.chip, { paddingVertical: 12 }, turn.id === selectedId && ui.chipSelected]}>
              <View><Text style={[ui.chipText, turn.id === selectedId && { color: '#FFF' }]}>{dateTime(turn.startTime)}</Text>
                <Text style={[ui.subtitle, { fontSize: 11 }, turn.id === selectedId && { color: '#FFF' }]}>{turn.tripLabel} · {turn.status?.replace(/_/g, ' ')}</Text></View>
            </TouchableOpacity>)}
          </ScrollView>
        </View>
        {!turns.length && <EmptyState icon="analytics-outline" title="No turns recorded" description="Driving graphs will appear when a run is assigned and tracking readings are received." />}
        {selected && <View style={ui.card}><SectionTitle title={selected.tripLabel || 'Selected turn'} subtitle={`${dateTime(selected.startTime)} → ${dateTime(selected.endTime)}`} />
          <Text style={ui.subtitle}>{selected.status?.replace(/_/g, ' ')} · {live ? 'Live readings update every 30 seconds' : 'Full recorded turn'}</Text>
        </View>}
        <ErrorNotice message={graphError} onRetry={() => setGraphRevision(value => value + 1)} />
        {graphLoading ? <ActivityIndicator color={Colors.primary} /> : selected && !graphError && (readings.length ? <>
          <View style={ui.metrics}><Metric label="Peak speed" value={speed.length ? `${Math.max(...speed).toFixed(1)}` : '—'} detail="km/h" icon="speedometer-outline" />
            <Metric label="Average safety score" value={score.length ? `${Math.round(score.reduce((a, b) => a + b, 0) / score.length)}/100` : '—'} detail={`${score.length} scored sensor batches`} icon="shield-checkmark-outline" /></View>
          <TurnGraph key={`${selectedId}-speed`} title="High speed" readings={readings} field="speed" unit="km/h" color="#B45123" />
          <TurnGraph key={`${selectedId}-braking`} title="Sudden braking" readings={readings} field="suddenBrakingCount" unit="events" aggregate="sum" color="#BD3347" />
          <TurnGraph key={`${selectedId}-turning`} title="Dangerous turns" readings={readings} field="dangerousTurnCount" unit="events" aggregate="sum" color="#94610A" />
          <TurnGraph key={`${selectedId}-score`} title="Safety score" readings={readings} field="safetyScore" unit="/100" aggregate="min" ceiling={100} color={Colors.primary} />
        </> : <EmptyState icon="pulse-outline" title="No readings for this turn" description="This turn has no GPS or acceleration readings yet." />)}
      </>}
    </ScrollView>
  </SafeAreaView>;
}
