import React, { useState, useCallback, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, ScrollView, RefreshControl, TouchableOpacity, TextInput, ActivityIndicator, useWindowDimensions } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useAuth } from '../../context/AuthContext';
import { useBus } from '../../context/BusContext';
import { useNotifications } from '../../context/NotificationContext';
import { useAlertPolicy } from '../../context/AlertPolicyContext';
import { getAllBuses, getFleetSummary, getFinanceSummary } from '../../services/busService';
import { errorMessage } from '../../services/api';
import { alertSeverity, safetyScore, isUnread, SEVERITIES } from '../../utils/alertPolicy';
import { Button, EmptyState, ErrorNotice, Metric, SectionTitle, ui } from '../../components/UI';
import BusCard from '../../components/BusCard';
import NotificationItem from '../../components/NotificationItem';
import Colors from '../../constants/Colors';

const money = value => `LKR ${Number(value || 0).toLocaleString('en-LK', { maximumFractionDigits: 0 })}`;
const roles = { BUS_DRIVER: 'Driver workspace', BUS_OWNER: 'Owner workspace', ADMIN: 'Administration', BUS_AUTHORITY_USER: 'Authority workspace' };
const isUnavailable = bus => bus.status === 'UNAVAILABLE' || bus.availabilityStatus === 'UNAVAILABLE';

export default function HomeScreen() {
  const { user } = useAuth();
  const { currentBus, isDriving, toggleDriving, refreshBus, assignedBusError, isLoading: driverLoading } = useBus();
  const { notifications, unreadCount, refreshNotifications, markNotificationRead, error: notificationError, lastUpdated } = useNotifications();
  const { policy, loaded: policyLoaded, error: policyError } = useAlertPolicy();
  const router = useRouter();
  const { width } = useWindowDimensions();
  const wide = width >= 980;
  const driver = user?.role === 'BUS_DRIVER';
  const canFinance = ['BUS_OWNER', 'ADMIN'].includes(user?.role);
  const canRequest = ['BUS_OWNER', 'BUS_DRIVER'].includes(user?.role);
  const [buses, setBuses] = useState([]);
  const [summary, setSummary] = useState(null);
  const [finances, setFinances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [financeError, setFinanceError] = useState(null);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [section, setSection] = useState('fleet');
  const identity = useRef(user?.id);
  identity.current = user?.id;

  const load = useCallback(async () => {
    if (driver || !user?.id) { setLoading(false); return; }
    const ownerId = user.role === 'BUS_OWNER' ? user.id : undefined;
    const results = await Promise.allSettled([getAllBuses(), getFleetSummary(ownerId),
      canFinance ? getFinanceSummary() : Promise.resolve([])]);
    if (identity.current !== user.id) return;
    if (results[0].status === 'fulfilled') {
      setBuses(ownerId ? results[0].value.filter(bus => String(bus.owner?.id) === String(ownerId)) : results[0].value);
      setError(null);
    } else setError(errorMessage(results[0].reason));
    setSummary(results[1].status === 'fulfilled' ? results[1].value : null);
    if (results[2].status === 'fulfilled') { setFinances(results[2].value || []); setFinanceError(null); }
    else setFinanceError(errorMessage(results[2].reason));
    setLoading(false);
  }, [user?.id, user?.role, driver, canFinance]);

  useEffect(() => { setLoading(true); setBuses([]); setFinances([]); setSummary(null); load(); }, [load]);
  const refresh = async () => {
    setRefreshing(true);
    try { await Promise.allSettled([driver ? refreshBus() : load(), refreshNotifications()]); }
    finally { setRefreshing(false); }
  };
  const unavailable = summary?.unavailableBusCount ?? buses.filter(isUnavailable).length;
  const active = summary?.activeFleetCount ?? buses.filter(bus => ['DRIVING', 'ACTIVE', 'IN_PROGRESS'].includes(bus.status)).length;
  const latestScored = notifications.find(n => safetyScore(n) !== null);
  const score = latestScored ? safetyScore(latestScored) : null;
  const severity = latestScored ? alertSeverity(latestScored, policy) : 'INFO';
  const risk = SEVERITIES[severity];
  const urgent = notifications.filter(n => isUnread(n) && ['CRITICAL', 'HIGH'].includes(alertSeverity(n, policy))).length;
  const totals = finances.reduce((acc, row) => ({ fees: acc.fees + Number(row.permitFeeTotal || 0),
    commissions: acc.commissions + Number(row.commissionTotal || 0), penalties: acc.penalties + Number(row.penaltyTotal || 0) }), { fees: 0, commissions: 0, penalties: 0 });
  const filtered = buses.filter(bus => {
    const match = `${bus.busNumber} ${bus.route} ${bus.assignedDriver}`.toLowerCase().includes(query.toLowerCase());
    return match && (filter === 'all' || (filter === 'unavailable' ? isUnavailable(bus) : ['DRIVING', 'ACTIVE', 'IN_PROGRESS'].includes(bus.status)));
  });
  const navigate = (pathname, bus) => router.push({ pathname, params: { busId: bus.id, busNumber: bus.busNumber, route: bus.route } });
  const displayName = typeof user?.name === 'string' && user.name ? user.name : user?.firstName || 'there';

  return <ScrollView style={ui.page} contentContainerStyle={ui.content} showsVerticalScrollIndicator={false}
    refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={Colors.primary} />}>
    <View style={styles.heading}>
      <View style={{ flex: 1 }}><Text style={ui.eyebrow}>{roles[user?.role] || 'Bus operations'}</Text>
        <Text style={styles.title}>Hello, {displayName.split(' ')[0]}<Text style={{ color: Colors.primary }}>.</Text></Text>
        <Text style={ui.subtitle}>A clearer view of your day on the road.</Text></View>
      <TouchableOpacity accessibilityRole="button" accessibilityLabel="Refresh dashboard" style={styles.refresh} onPress={refresh}>
        <Ionicons name="refresh-outline" size={20} color={Colors.primary} /></TouchableOpacity>
    </View>

    <View style={styles.hero}>
      <View style={{ flex: 1, minWidth: 200 }}><Text style={styles.heroEyebrow}>BUS TRACK PRO / OPERATIONS</Text>
        <Text style={styles.heroTitle}>{driver ? (isDriving ? 'Your journey is underway.' : 'Ready for your next journey?') : 'Every bus. A better journey.'}</Text>
        <Text style={styles.heroText}>{driver ? 'Your assigned bus, daily schedule and earnings, together in one place.' : 'Keep your fleet moving with clear schedules, financial summaries and safety alerts.'}</Text>
        <View style={styles.heroMeta}><View style={styles.dot} /><Text style={styles.heroMetaText}>Alerts refresh every 30 seconds</Text></View>
      </View>
      <View style={styles.heroIcon}><Ionicons name={driver ? 'navigate-outline' : 'bus-outline'} size={55} color="#D5EAE2" /></View>
    </View>

    {urgent > 0 && <TouchableOpacity accessibilityRole="button" onPress={() => router.push('/(tabs)/alerts')} style={styles.urgent}>
      <Ionicons name="warning-outline" size={24} color={Colors.error} /><View style={{ flex: 1 }}>
        <Text style={styles.urgentTitle}>{urgent} unread safety alert{urgent === 1 ? '' : 's'} need attention</Text>
        <Text style={ui.subtitle}>Review critical and high risk alerts in the alert center.</Text></View><Ionicons name="chevron-forward" size={20} color={Colors.error} /></TouchableOpacity>}

    <View style={ui.metrics}>
      {driver ? <Metric label="Your bus" value={currentBus?.busNumber || 'Unassigned'} icon="bus-outline" detail={isDriving ? 'Currently on the road' : 'Check your next departure'} />
        : <Metric label="Total fleet" value={loading ? '—' : summary?.totalFleetCount ?? buses.length} icon="bus-outline" detail="Registered buses" />}
      <Metric label={driver ? 'Driving status' : 'Active fleet'} value={driver ? (isDriving ? 'On the road' : 'Off duty') : loading ? '—' : active} icon="navigate-outline" detail={driver ? 'Manage your run below' : `${unavailable} unavailable today`} />
      <Metric label="Unread alerts" value={unreadCount} icon="notifications-outline" color={urgent ? Colors.error : Colors.primary} detail="Among 20 recent notifications" />
      <Metric label="Latest alert score" value={score === null ? '—' : `${score}/100`} icon="shield-checkmark-outline" color={risk.color} detail={score === null ? 'No scored alerts received' : `${risk.label} · latest scored event`} />
    </View>

    <View style={[styles.columns, wide && { flexDirection: 'row', alignItems: 'flex-start' }]}>
      <View style={{ flex: wide ? 2 : undefined, gap: 20 }}>
        {driver ? <View><SectionTitle title="Your assigned bus" subtitle="Manage your run and see what comes next" action="Browse buses" onPress={() => router.push('/allowed-buses')} />
          <ErrorNotice message={assignedBusError} onRetry={refreshBus} />
          {driverLoading && !currentBus ? <ActivityIndicator color={Colors.primary} /> : !currentBus && assignedBusError ? null : <BusCard bus={currentBus} isDriving={isDriving} onToggleDriving={toggleDriving} isLoading={driverLoading} />}</View>
          : <View>
            <SectionTitle title="Fleet workspace" subtitle="Schedules, availability and reports" />
            {canFinance && <View style={[styles.chips, { marginBottom: 16 }]}>{[['fleet', 'Your fleet', 'bus-outline'], ['reports', 'Financial reports', 'stats-chart-outline']].map(([id, label, icon]) =>
              <TouchableOpacity accessibilityRole="tab" accessibilityState={{ selected: section === id }} key={id} onPress={() => setSection(id)} style={[ui.chip, section === id && ui.chipSelected]}>
                <Ionicons name={icon} size={17} color={section === id ? '#FFF' : Colors.textSecondary} /><Text style={[ui.chipText, section === id && { color: '#FFF' }]}>{label}</Text></TouchableOpacity>)}</View>}
            <ErrorNotice message={error} onRetry={load} />
            {loading ? <View style={[ui.card, { alignItems: 'center', gap: 12 }]}><ActivityIndicator color={Colors.primary} /><Text style={ui.subtitle}>Loading your workspace…</Text></View> : section === 'fleet' ? <>
              <View style={styles.search}><Ionicons name="search-outline" size={20} color={Colors.muted} /><TextInput accessibilityLabel="Search fleet" placeholder="Search bus, route or driver" placeholderTextColor={Colors.muted} value={query} onChangeText={setQuery} style={styles.searchInput} />
                {!!query && <TouchableOpacity accessibilityLabel="Clear search" onPress={() => setQuery('')} style={{ padding: 10 }}><Ionicons name="close" size={18} color={Colors.muted} /></TouchableOpacity>}</View>
              <View style={[styles.chips, { marginVertical: 12 }]}>{[['all', 'All buses'], ['active', 'On the road'], ['unavailable', 'Unavailable']].map(([id, label]) =>
                <TouchableOpacity accessibilityRole="button" accessibilityState={{ selected: filter === id }} key={id} onPress={() => setFilter(id)} style={[ui.chip, filter === id && { backgroundColor: Colors.primaryLight, borderColor: '#BAD5CA' }]}><Text style={ui.chipText}>{label}</Text></TouchableOpacity>)}</View>
              {!filtered.length && <EmptyState icon="bus-outline" title={buses.length ? 'No matching buses' : 'Your fleet will appear here'} description={buses.length ? 'Try another search or filter.' : 'Registered buses and their schedules will be shown when available.'} action={buses.length ? 'Clear filters' : 'Refresh fleet'} onPress={() => { setQuery(''); setFilter('all'); if (!buses.length) load(); }} />}
              {filtered.map(bus => <View key={bus.id} style={[ui.card, { marginBottom: 12 }]}>
                <View style={styles.busHeading}><View style={styles.busIcon}><Ionicons name="bus-outline" size={24} color={Colors.primary} /></View><View style={{ flex: 1 }}><Text style={styles.busNumber}>{bus.busNumber}</Text><Text style={ui.subtitle}>{bus.route}</Text></View>
                  <Text style={[styles.status, { color: isUnavailable(bus) ? Colors.error : Colors.primary, backgroundColor: isUnavailable(bus) ? '#FCECEF' : Colors.primaryLight }]}>{bus.status?.replace(/_/g, ' ') || 'IDLE'}</Text></View>
                {isUnavailable(bus) && <Text style={styles.reason}>{bus.unavailabilityReason || 'Temporarily unavailable'}</Text>}
                <View style={styles.busInfo}><Text style={ui.subtitle}>Driver · {bus.assignedDriver || 'Unassigned'}</Text><Text style={ui.subtitle}>Next turn · {bus.nextTurn || 'Not scheduled'}</Text><Text style={ui.subtitle}>{bus.capacity ?? '—'} seats</Text></View>
                <View style={styles.busActions}><Button title="Schedule" icon="calendar-outline" secondary onPress={() => navigate('/bus-schedule', bus)} style={{ flex: 1 }} />
                  <Button title="Agenda" icon="list-outline" secondary onPress={() => navigate('/bus-agenda', bus)} style={{ flex: 1 }} />
                  {canFinance && <Button title="Net report" icon="stats-chart-outline" secondary onPress={() => navigate('/daily-net', bus)} style={{ flex: 1 }} />}</View>
              </View>)}
            </> : <View style={{ gap: 12 }}>
              <ErrorNotice message={financeError} onRetry={load} />
              <View style={[ui.card, { backgroundColor: Colors.navy }]}><Text style={styles.heroEyebrow}>LAST 30 DAYS / AUTHORITY PAYABLE</Text><Text style={styles.total}>{financeError ? '—' : money(totals.fees - totals.commissions + totals.penalties)}</Text>
                <Text style={styles.heroText}>Permit fees − commissions + penalties</Text></View>
              {!finances.length && !financeError && <EmptyState icon="stats-chart-outline" title="No financial records yet" description="Completed runs will appear in your financial summary." />}
              {finances.map(item => <View key={item.busId} style={ui.card}><SectionTitle title={item.busNumber} action="Daily report" onPress={() => navigate('/daily-net', { id: item.busId, busNumber: item.busNumber })} />
                {[['Permit fees', item.permitFeeTotal], ['Commissions (deducted)', item.commissionTotal], ['Penalties', item.penaltyTotal]].map(([label, value]) => <View key={label} style={styles.financeRow}><Text style={ui.subtitle}>{label}</Text><Text style={styles.amount}>{money(value)}</Text></View>)}
                <View style={[styles.financeRow, styles.netRow]}><Text style={styles.amount}>Net payable</Text><Text style={[styles.amount, { color: Colors.primary }]}>{money(Number(item.permitFeeTotal || 0) - Number(item.commissionTotal || 0) + Number(item.penaltyTotal || 0))}</Text></View></View>)}
            </View>}
          </View>}
      </View>

      <View style={{ flex: wide ? 1 : undefined, gap: 20 }}>
        {canRequest && <View style={ui.card}><SectionTitle title="Keep things moving" subtitle="Manage changes to your day" />
          <Button title="New approval request" icon="add-circle-outline" onPress={() => router.push('/new-request')} />
          <Button title="Track my requests" icon="document-text-outline" secondary onPress={() => router.push('/my-requests')} style={{ marginTop: 10 }} />
          {summary && <Text style={ui.subtitle}>{summary.pendingRequestCount} pending request{summary.pendingRequestCount === 1 ? '' : 's'} across your fleet</Text>}</View>}
        <View style={ui.card}><SectionTitle title="Safety at a glance" action="Thresholds" onPress={() => router.push('/(tabs)/settings')} />
          {policyError && <Text style={ui.subtitle}>{policyLoaded ? 'Showing the last loaded thresholds.' : 'Active thresholds unavailable. Showing defaults.'}</Text>}
          <Text style={ui.subtitle}>Higher scores mean safer driving. Scores describe individual sensor batches.</Text>
          <View style={{ flexDirection: 'row', height: 9, borderRadius: 6, overflow: 'hidden', marginVertical: 18 }}>
            {[['CRITICAL', policy.criticalBelow], ['HIGH', policy.highRiskBelow - policy.criticalBelow], ['CAUTION', policy.cautionBelow - policy.highRiskBelow], ['LOW', 100 - policy.cautionBelow]].map(([id, size]) => <View key={id} style={{ flex: size, backgroundColor: SEVERITIES[id].color }} />)}</View>
          {[['CRITICAL', `0–${policy.criticalBelow - 1}`], ['HIGH', `${policy.criticalBelow}–${policy.highRiskBelow - 1}`], ['CAUTION', `${policy.highRiskBelow}–${policy.cautionBelow - 1}`], ['LOW', `${policy.cautionBelow}–100`]].map(([id, range]) => <View key={id} style={styles.legendRow}><View style={[styles.legendDot, { backgroundColor: SEVERITIES[id].color }]} /><Text style={[ui.subtitle, { flex: 1, marginTop: 0 }]}>{SEVERITIES[id].label}</Text><Text style={styles.amount}>{range}</Text></View>)}
        </View>
        <View><SectionTitle title="Recent activity" subtitle={lastUpdated ? `Last synced ${lastUpdated.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : 'Latest notifications'} action="View all" onPress={() => router.push('/(tabs)/alerts')} />
          <ErrorNotice message={notificationError} onRetry={refreshNotifications} />
          {notifications.length ? notifications.slice(0, 3).map(n => <NotificationItem key={n.notificationId ?? n.id} notification={n} onRead={markNotificationRead} />) : <EmptyState icon="notifications-outline" title="No recent activity" description="Safety alerts and request updates will appear here." />}</View>
      </View>
    </View>
  </ScrollView>;
}

const styles = StyleSheet.create({
  heading: { flexDirection: 'row', gap: 12, alignItems: 'center', marginVertical: 6 }, title: { fontSize: 32, fontWeight: '800', color: Colors.text, letterSpacing: -1, marginTop: 7 },
  refresh: { height: 46, width: 46, borderWidth: 1, borderColor: Colors.border, borderRadius: 14, backgroundColor: '#FFF', justifyContent: 'center', alignItems: 'center' },
  hero: { flexDirection: 'row', flexWrap: 'wrap', backgroundColor: Colors.navy, borderRadius: 24, padding: 26, gap: 20, alignItems: 'center' },
  heroEyebrow: { color: '#A5C8BF', fontSize: 10, fontWeight: '700', letterSpacing: 1.8 }, heroTitle: { color: '#FFF', fontSize: 27, lineHeight: 35, letterSpacing: -0.6, fontWeight: '700', marginTop: 12 },
  heroText: { color: '#BACCD5', fontSize: 13, lineHeight: 22, marginTop: 10, maxWidth: 650 }, heroMeta: { flexDirection: 'row', gap: 8, alignItems: 'center', marginTop: 20 }, heroMetaText: { color: '#C0D8CD', fontSize: 11 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: '#82C9A9' }, heroIcon: { padding: 20, borderWidth: 1, borderColor: '#3C5664', borderRadius: 24, backgroundColor: '#213E4D' },
  columns: { gap: 24 }, urgent: { flexDirection: 'row', alignItems: 'center', gap: 14, backgroundColor: '#FCECEF', padding: 16, borderRadius: 16, borderWidth: 1, borderColor: '#F1C7CF' }, urgentTitle: { color: Colors.error, fontWeight: '700', fontSize: 14 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 }, search: { flexDirection: 'row', alignItems: 'center', borderRadius: 12, backgroundColor: '#FFF', borderWidth: 1, borderColor: Colors.border, paddingLeft: 14 }, searchInput: { flex: 1, minHeight: 48, padding: 12, fontSize: 14, color: Colors.text },
  busHeading: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', gap: 12 }, busIcon: { padding: 12, borderRadius: 14, backgroundColor: Colors.primaryLight }, busNumber: { color: Colors.text, fontWeight: '800', fontSize: 18 },
  status: { fontSize: 10, fontWeight: '700', paddingHorizontal: 10, paddingVertical: 7, borderRadius: 8 }, reason: { color: Colors.error, lineHeight: 20, fontSize: 12, marginTop: 12 },
  busInfo: { flexDirection: 'row', flexWrap: 'wrap', columnGap: 20, paddingVertical: 14, marginTop: 4, borderBottomWidth: 1, borderBottomColor: Colors.border }, busActions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 14 },
  total: { color: '#FFF', fontSize: 32, fontWeight: '800', marginTop: 12 }, financeRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 16, paddingVertical: 6 }, amount: { fontSize: 13, color: Colors.text, fontWeight: '700' }, netRow: { marginTop: 10, paddingTop: 15, borderTopWidth: 1, borderTopColor: Colors.border },
  legendRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 12 }, legendDot: { width: 8, height: 8, borderRadius: 4 },
});
