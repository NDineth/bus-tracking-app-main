import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useAlertPolicy } from '../context/AlertPolicyContext';
import { alertSeverity, safetyScore, isUnread, notificationId, SEVERITIES } from '../utils/alertPolicy';
import { errorMessage } from '../services/api';
import { Button, ui } from './UI';
import Colors from '../constants/Colors';

export default function NotificationItem({ notification, onRead, compact = false }) {
  const { policy } = useAlertPolicy();
  const router = useRouter();
  const unread = isUnread(notification);
  const severity = alertSeverity(notification, policy);
  const risk = SEVERITIES[severity];
  const score = safetyScore(notification);
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
    <TouchableOpacity accessibilityRole="button" accessibilityLabel={`${notification.title}. ${risk.label}. ${unread ? 'Unread' : 'Read'}. Show details.`}
      accessibilityState={{ expanded }} onPress={() => setExpanded(!expanded)} style={styles.top}>
      <View style={[styles.icon, { backgroundColor: risk.background }]}><Ionicons name={risk.icon} size={22} color={risk.color} /></View>
      <View style={{ flex: 1 }}><View style={styles.meta}><Text style={[styles.badge, { color: risk.color, backgroundColor: risk.background }]}>{risk.label}</Text>
        {unread && <Text style={styles.unread}>NEW</Text>}</View><Text style={styles.title}>{notification.title || 'Notification'}</Text>
        <Text style={styles.time}>{time}</Text></View><Ionicons name={expanded ? 'chevron-up' : 'chevron-down'} size={18} color={Colors.muted} />
    </TouchableOpacity>
    <Text style={styles.body} numberOfLines={expanded ? undefined : compact ? 2 : 3}>{notification.body || 'No additional details.'}</Text>
    {score !== null && <View style={styles.scoreRow}><Text style={styles.scoreLabel}>Safety score</Text><View style={styles.track}><View style={{ width: `${score}%`, height: 6, backgroundColor: risk.color, borderRadius: 4 }} /></View><Text style={[styles.score, { color: risk.color }]}>{score}/100</Text></View>}
    {expanded && <View style={styles.details}>
      <Text style={ui.subtitle}>{severity === 'CRITICAL' ? 'Check on the bus, driver and passengers immediately. Follow your emergency response procedure.'
        : severity === 'HIGH' ? 'Review this event with the driver and check the bus before the next departure.'
        : severity === 'CAUTION' ? 'Review the driving pattern and monitor upcoming events.' : 'Review the details and any related request updates.'}</Text>
      {score !== null && <Text style={ui.subtitle}>This score applies to one sensor batch. Marking an alert read does not resolve the underlying incident.</Text>}
      {notification.severity && <Text style={ui.subtitle}>Severity was recorded when this alert was generated.</Text>}
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
  title: { color: Colors.text, fontSize: 14, fontWeight: '700', lineHeight: 20 }, time: { color: Colors.textSecondary, fontSize: 11, marginTop: 5 }, body: { color: Colors.textSecondary, fontSize: 13, lineHeight: 21, marginTop: 13 },
  scoreRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 15 }, scoreLabel: { color: Colors.textSecondary, fontSize: 11 }, track: { flex: 1, height: 6, borderRadius: 4, backgroundColor: Colors.background, overflow: 'hidden' }, score: { fontSize: 13, fontWeight: '800' },
  details: { borderTopWidth: 1, borderTopColor: Colors.border, marginTop: 14, paddingTop: 10 }, actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
});
