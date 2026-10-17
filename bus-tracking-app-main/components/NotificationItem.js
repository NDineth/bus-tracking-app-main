import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useAlertPolicy } from '../context/AlertPolicyContext';
import { alertSeverity, alertType, safetyScore, isUnread, notificationId, SEVERITIES } from '../utils/alertPolicy';
import { errorMessage } from '../services/api';
import { Button, ui } from './UI';
import Colors from '../constants/Colors';
import { getBrakingDetails } from '../utils/braking';

export default function NotificationItem({ notification, onRead }) {
  const { policy } = useAlertPolicy();
  const router = useRouter();
  const unread = isUnread(notification);
  const severity = alertSeverity(notification, policy);
  const risk = SEVERITIES[severity];
  const score = safetyScore(notification);
  const braking = getBrakingDetails(notification);
  const type = alertType(notification);
  const [expanded, setExpanded] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const date = new Date(notification.createdAt);
  const time = Number.isNaN(date.getTime()) ? 'Time unavailable' : date.toLocaleString('en-LK', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  const approval = /approval request/i.test(notification.title || '');
  const markRead = async () => {
    setBusy(true); setError(null);
    try { await onRead(notificationId(notification)); }
    catch (e) { setError(errorMessage(e)); }
    finally { setBusy(false); }
  };
  return <View style={[styles.card, unread && { borderLeftColor: risk.color, borderLeftWidth: 4 }]}>
    <TouchableOpacity accessibilityRole="button" accessibilityLabel={`${type}. ${unread ? 'Unread' : 'Read'}. ${expanded ? 'Hide' : 'Show'} details.`}
      accessibilityState={{ expanded }} onPress={() => setExpanded(!expanded)}>
      <View style={styles.top}>
      <View style={[styles.icon, { backgroundColor: risk.background }]}><Ionicons name={risk.icon} size={22} color={risk.color} /></View>
      <Text style={styles.title}>{type}</Text><Ionicons name={expanded ? 'chevron-up' : 'chevron-down'} size={18} color={Colors.muted} />
      </View>
    </TouchableOpacity>
    {expanded && <View style={styles.details}>
      <ScrollView style={styles.detailScroll} nestedScrollEnabled showsVerticalScrollIndicator contentContainerStyle={{ paddingBottom: 12 }}>
      <View style={styles.meta}><Text style={[styles.badge, { color: risk.color, backgroundColor: risk.background }]}>{risk.label}</Text>
        {unread && <Text style={styles.unread}>NEW</Text>}</View>
      {typeof notification.title === 'string' && notification.title.trim() !== type && <Text style={styles.fullTitle}>{notification.title}</Text>}
      <Text style={styles.time}>{time}</Text>
      {!braking && <Text style={styles.body}>{notification.body || 'No additional details.'}</Text>}
      {braking && <View style={[styles.brakingData, { backgroundColor: braking.level?.background || Colors.background }]}>
        <Text style={styles.detailTitle}>Braking event details</Text>
        <View style={styles.valueRow}><Text style={styles.valueLabel}>Braking value (X)</Text><Text style={styles.valueText}>{braking.x === null ? 'Not provided' : String(braking.x)}</Text></View>
        <View style={styles.valueRow}><Text style={styles.valueLabel}>Severity</Text><Text style={[styles.valueText, { color: braking.level?.color || Colors.textSecondary }]}>{braking.level?.label || 'Not available'}</Text></View>
        <Text style={[styles.valueLabel, { marginTop: 8 }]}>Message</Text><Text style={styles.body}>{braking.message}</Text>
        {braking.count !== null && <View style={styles.valueRow}><Text style={styles.valueLabel}>Harsh braking count</Text><Text style={styles.valueText}>{braking.count}</Text></View>}
        {braking.x === null && <Text style={ui.subtitle}>This notification does not include an X reading. Its event count cannot determine braking severity.</Text>}
        {braking.detailsBody ? <Text style={styles.body}>{braking.detailsBody}</Text> : null}
      </View>}
      {score !== null && <View style={styles.scoreRow}><Text style={styles.scoreLabel}>Safety score</Text><View style={styles.track}><View style={{ width: `${score}%`, height: 6, backgroundColor: risk.color, borderRadius: 4 }} /></View><Text style={[styles.score, { color: risk.color }]}>{score}/100</Text></View>}
      <Text style={ui.subtitle}>{severity === 'CRITICAL' ? 'Check on the bus, driver and passengers immediately. Follow your emergency response procedure.'
        : severity === 'HIGH' ? 'Review this event with the driver and check the bus before the next departure.'
        : severity === 'CAUTION' ? 'Review the driving pattern and monitor upcoming events.' : 'Review the details and any related request updates.'}</Text>
      {score !== null && <Text style={ui.subtitle}>This score applies to one sensor batch. Marking an alert read does not resolve the underlying incident.</Text>}
      {notification.severity && <Text style={ui.subtitle}>Overall alert severity: {risk.label}. Recorded when this alert was generated{braking ? '; braking severity is based on its X reading' : ''}.</Text>}
      </ScrollView>
      <View style={styles.actions}>{unread && <Button title="Mark as read" icon="checkmark-outline" loading={busy} secondary onPress={markRead} />}
        {approval && <Button title="View requests" icon="document-text-outline" onPress={() => router.push('/my-requests')} />}
      </View>
    </View>}
    {error && <Text accessibilityRole="alert" style={{ color: Colors.error, marginTop: 10 }}>{error}</Text>}
  </View>;
}
const styles = StyleSheet.create({
  card: { backgroundColor: '#FFF', borderRadius: 17, borderWidth: 1, borderColor: Colors.border, padding: 17, marginBottom: 12 },
  top: { flexDirection: 'row', alignItems: 'center', gap: 12, minHeight: 48 }, icon: { padding: 12, borderRadius: 14 },
  meta: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', gap: 8, marginBottom: 6 }, badge: { fontSize: 10, fontWeight: '700', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 6 }, unread: { color: Colors.primary, fontSize: 9, fontWeight: '800', letterSpacing: 1 },
  title: { flex: 1, color: Colors.text, fontSize: 15, fontWeight: '700', lineHeight: 21 }, fullTitle: { color: Colors.text, fontSize: 14, fontWeight: '600', marginTop: 10 }, time: { color: Colors.textSecondary, fontSize: 11, marginTop: 5 }, body: { color: Colors.textSecondary, fontSize: 13, lineHeight: 21, marginTop: 13 },
  scoreRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 15 }, scoreLabel: { color: Colors.textSecondary, fontSize: 11 }, track: { flex: 1, height: 6, borderRadius: 4, backgroundColor: Colors.background, overflow: 'hidden' }, score: { fontSize: 13, fontWeight: '800' },
  details: { borderTopWidth: 1, borderTopColor: Colors.border, marginTop: 14, paddingTop: 10 }, actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  detailScroll: { maxHeight: 320 },
  brakingData: { padding: 14, borderRadius: 12 }, detailTitle: { fontSize: 14, fontWeight: '700', color: Colors.text, marginBottom: 10 },
  valueRow: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginVertical: 6 },
  valueLabel: { color: Colors.textSecondary, fontSize: 13 }, valueText: { fontSize: 14, fontWeight: '700', color: Colors.text },
});
