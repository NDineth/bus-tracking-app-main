import React, { useMemo, useState } from 'react';
import { View, Text, TextInput, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useNotifications } from '../context/NotificationContext';
import { useAlertPolicy } from '../context/AlertPolicyContext';
import { alertSeverity, isUnread, SEVERITIES } from '../utils/alertPolicy';
import { Button, ErrorNotice, Metric, ui } from './UI';
import NotificationList from './NotificationList';
import Colors from '../constants/Colors';

export default function AlertCenter() {
  const { notifications, unreadCount, isLoading, refreshNotifications, markNotificationRead, markAllRead, error, lastUpdated } = useNotifications();
  const { policy } = useAlertPolicy();
  const [filter, setFilter] = useState('ALL');
  const [query, setQuery] = useState('');
  const [priority, setPriority] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const router = useRouter();
  const critical = notifications.filter(n => alertSeverity(n, policy) === 'CRITICAL').length;
  const high = notifications.filter(n => alertSeverity(n, policy) === 'HIGH').length;
  const filtered = useMemo(() => {
    const result = notifications.filter(n => (filter === 'ALL' || (filter === 'UNREAD' ? isUnread(n) : alertSeverity(n, policy) === filter))
      && `${n.title} ${n.body}`.toLowerCase().includes(query.toLowerCase()));
    const order = { CRITICAL: 0, HIGH: 1, CAUTION: 2, LOW: 3, INFO: 4 };
    return priority ? result.sort((a, b) => order[alertSeverity(a, policy)] - order[alertSeverity(b, policy)]) : result;
  }, [notifications, filter, query, priority, policy]);
  const readAll = async () => {
    setBusy(true); setActionError(null);
    try { await markAllRead(); }
    catch (e) { setActionError(e.message); }
    finally { setBusy(false); }
  };
  const header = <View style={{ gap: 18, marginBottom: 20 }}>
    <View><Text style={ui.eyebrow}>STAY INFORMED / STAY IN CONTROL</Text><Text style={styles.title}>Alert center</Text>
      <Text style={ui.subtitle}>Your 20 most recent notifications. Updates every 30 seconds.</Text>
      {lastUpdated && <Text style={ui.subtitle}>Last synced {lastUpdated.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</Text>}</View>
    <View style={ui.metrics}><Metric label="Unread" value={unreadCount} icon="mail-unread-outline" detail="Recent notifications" />
      <Metric label="Critical" value={critical} color={SEVERITIES.CRITICAL.color} icon="alert-circle-outline" detail="Immediate attention" />
      <Metric label="High risk" value={high} color={SEVERITIES.HIGH.color} icon="warning-outline" detail="Review with driver" /></View>
    <View style={styles.actions}><Button title="Mark recent read" icon="checkmark-done-outline" secondary loading={busy} disabled={!unreadCount} onPress={readAll} />
      <Button title="Alert thresholds" icon="options-outline" secondary onPress={() => router.push('/(tabs)/settings')} /></View>
    <ErrorNotice message={actionError || error} onRetry={refreshNotifications} />
    <View style={styles.search}><Ionicons name="search-outline" size={20} color={Colors.muted} /><TextInput value={query} onChangeText={setQuery} placeholder="Search alerts or bus number" placeholderTextColor={Colors.muted} accessibilityLabel="Search alerts" style={styles.input} /></View>
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
      {[['ALL', 'All activity'], ['UNREAD', 'Unread'], ...Object.entries(SEVERITIES).map(([id, value]) => [id, value.label])].map(([id, label]) =>
        <TouchableOpacity key={id} accessibilityRole="button" accessibilityState={{ selected: filter === id }} style={[ui.chip, filter === id && ui.chipSelected]} onPress={() => setFilter(id)}><Text style={[ui.chipText, filter === id && { color: '#FFF' }]}>{label}</Text></TouchableOpacity>)}
    </ScrollView>
    <View style={styles.result}><Text style={ui.subtitle}>{filtered.length} notification{filtered.length === 1 ? '' : 's'}</Text><TouchableOpacity accessibilityRole="button" onPress={() => setPriority(!priority)} style={ui.linkButton}><Ionicons name="swap-vertical-outline" size={16} color={Colors.primary} /><Text style={ui.link}>{priority ? 'Highest severity first' : 'Newest first'}</Text></TouchableOpacity></View>
  </View>;
  return <View style={ui.page}><NotificationList notifications={filtered} onRefresh={refreshNotifications} refreshing={isLoading} onMarkRead={markNotificationRead} header={header} filtered={filter !== 'ALL' || !!query} /></View>;
}
const styles = StyleSheet.create({ title: { fontSize: 32, fontWeight: '800', color: Colors.text, marginTop: 10, letterSpacing: -1 }, actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  search: { flexDirection: 'row', gap: 10, alignItems: 'center', backgroundColor: '#FFF', borderWidth: 1, borderColor: Colors.border, borderRadius: 12, paddingHorizontal: 14 }, input: { flex: 1, minHeight: 48, color: Colors.text, fontSize: 14 }, result: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 } });
