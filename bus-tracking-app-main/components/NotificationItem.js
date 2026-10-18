import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useAlertPolicy } from '../context/AlertPolicyContext';
import { alertSeverity, alertType, isUnread, notificationId, SEVERITIES } from '../utils/alertPolicy';
import { errorMessage } from '../services/api';
import { Button } from './UI';
import Colors from '../constants/Colors';
import { drivingAlertDetails } from '../utils/drivingMetrics';

export default function NotificationItem({ notification, onRead }) {
  const { policy } = useAlertPolicy();
  const router = useRouter();
  const unread = isUnread(notification);
  const severity = alertSeverity(notification, policy);
  const risk = SEVERITIES[severity];
  const metrics = drivingAlertDetails(notification);
  const driving = /driving|braking|acceleration|high.speed|dangerous.turn/i.test(`${notification.title || ''} ${notification.source || ''}`) || metrics.score !== null;
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
      {driving ? <View style={styles.metricList}>
        {[
          ['High speed', metrics.highSpeed === null ? (metrics.highSpeedDetected ? 'Detected · count unavailable' : 'Not provided') : `${metrics.highSpeed} events`],
          ['Sudden braking', metrics.braking === null ? 'Not provided' : `${metrics.braking} events`],
          ['Dangerous acceleration', metrics.acceleration === null ? 'Not provided' : `${metrics.acceleration} events`],
          ['Dangerous turns', metrics.turning === null ? 'Not provided' : `${metrics.turning} events`],
          ['Safety score', metrics.score === null ? 'Not provided' : `${metrics.score}/100`],
          ['Driver rating', metrics.rating || 'Not provided'],
        ].map(([label, value]) => <View key={label} style={styles.valueRow}>
          <Text style={styles.valueLabel}>{label}</Text><Text style={styles.valueText}>{value}</Text>
        </View>)}
      </View> : <Text style={styles.body}>{notification.body || 'No additional details.'}</Text>}
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
  details: { borderTopWidth: 1, borderTopColor: Colors.border, marginTop: 14, paddingTop: 10 }, actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  detailScroll: { maxHeight: 320 },
  metricList: { marginTop: 12, gap: 4 },
  valueRow: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginVertical: 6 },
  valueLabel: { color: Colors.textSecondary, fontSize: 13 }, valueText: { fontSize: 14, fontWeight: '700', color: Colors.text },
});
