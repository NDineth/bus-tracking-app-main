import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ActivityIndicator } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Colors from '../constants/Colors';

export function Button({ title, icon, onPress, secondary, disabled, loading, style, accessibilityLabel }) {
  return <TouchableOpacity accessibilityRole="button" accessibilityLabel={accessibilityLabel || title}
    accessibilityState={{ disabled: !!(disabled || loading), busy: !!loading }} disabled={disabled || loading}
    onPress={onPress} style={[ui.button, secondary && ui.secondaryButton, (disabled || loading) && { opacity: 0.5 }, style]}>
    {loading ? <ActivityIndicator color={secondary ? Colors.primary : '#FFF'} /> : icon && <Ionicons name={icon} size={18} color={secondary ? Colors.primary : '#FFF'} />}
    <Text style={[ui.buttonText, secondary && { color: Colors.primary }]}>{title}</Text>
  </TouchableOpacity>;
}
export function SectionTitle({ title, subtitle, action, onPress }) {
  return <View style={ui.section}><View style={{ flex: 1 }}><Text style={ui.sectionTitle}>{title}</Text>
    {subtitle && <Text style={ui.subtitle}>{subtitle}</Text>}</View>
    {action && <TouchableOpacity accessibilityRole="button" onPress={onPress} style={ui.linkButton}><Text style={ui.link}>{action}</Text><Ionicons name="arrow-forward" size={16} color={Colors.primary} /></TouchableOpacity>}
  </View>;
}
export function EmptyState({ icon = 'file-tray-outline', title, description, action, onPress }) {
  return <View style={ui.empty}><View style={ui.emptyIcon}><Ionicons name={icon} size={30} color={Colors.primary} /></View>
    <Text style={ui.emptyTitle}>{title}</Text><Text style={[ui.subtitle, { textAlign: 'center' }]}>{description}</Text>
    {action && <Button title={action} secondary onPress={onPress} style={{ marginTop: 16 }} />}</View>;
}
export function ErrorNotice({ message, onRetry }) {
  if (!message) return null;
  return <View accessibilityRole="alert" style={ui.error}><Ionicons name="cloud-offline-outline" size={20} color={Colors.error} />
    <Text style={{ flex: 1, color: Colors.error, lineHeight: 20 }}>{message}</Text>
    {onRetry && <TouchableOpacity accessibilityRole="button" onPress={onRetry} style={ui.linkButton}><Text style={ui.link}>Retry</Text></TouchableOpacity>}</View>;
}
export function Metric({ label, value, icon, detail, color = Colors.primary }) {
  return <View style={ui.metric}><View style={ui.metricTop}><Text style={ui.metricLabel}>{label}</Text><Ionicons name={icon} size={19} color={color} /></View>
    <Text style={[ui.metricValue, { color }]} numberOfLines={1} adjustsFontSizeToFit>{value}</Text>
    {detail && <Text style={ui.metricDetail}>{detail}</Text>}</View>;
}
export const ui = StyleSheet.create({
  page: { flex: 1, backgroundColor: Colors.background },
  content: { padding: 20, paddingBottom: 40, width: '100%', maxWidth: 1180, alignSelf: 'center', gap: 20 },
  card: { backgroundColor: '#FFF', borderWidth: 1, borderColor: Colors.border, borderRadius: 20, padding: 20 },
  section: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 14 },
  sectionTitle: { fontSize: 20, fontWeight: '700', color: Colors.text, letterSpacing: -0.4 },
  subtitle: { fontSize: 13, lineHeight: 21, color: Colors.textSecondary, marginTop: 4 },
  eyebrow: { fontSize: 11, fontWeight: '700', letterSpacing: 1.8, color: Colors.primary, textTransform: 'uppercase' },
  button: { minHeight: 48, borderRadius: 12, backgroundColor: Colors.primary, paddingHorizontal: 16, paddingVertical: 12, flexDirection: 'row', gap: 8, alignItems: 'center', justifyContent: 'center' },
  secondaryButton: { backgroundColor: Colors.primaryLight, borderWidth: 1, borderColor: '#CCE1D9' },
  buttonText: { color: '#FFF', fontWeight: '700', fontSize: 13 },
  linkButton: { minHeight: 44, paddingHorizontal: 6, alignItems: 'center', justifyContent: 'center', flexDirection: 'row', gap: 6 },
  link: { color: Colors.primary, fontSize: 13, fontWeight: '700' },
  empty: { backgroundColor: '#FFF', borderWidth: 1, borderColor: Colors.border, borderRadius: 20, padding: 28, alignItems: 'center' },
  emptyIcon: { padding: 16, borderRadius: 20, backgroundColor: Colors.primaryLight, marginBottom: 12 },
  emptyTitle: { fontWeight: '700', color: Colors.text, fontSize: 17, marginBottom: 4 },
  error: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FCECEF', borderRadius: 12, padding: 14, gap: 10 },
  metrics: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  metric: { flex: 1, minWidth: 140, backgroundColor: '#FFF', borderRadius: 18, padding: 18, borderWidth: 1, borderColor: Colors.border },
  metricTop: { flexDirection: 'row', gap: 10, justifyContent: 'space-between', alignItems: 'center' },
  metricLabel: { color: Colors.textSecondary, fontSize: 12, fontWeight: '600', flex: 1 },
  metricValue: { fontSize: 28, fontWeight: '800', marginTop: 12, letterSpacing: -0.8 },
  metricDetail: { color: Colors.textSecondary, fontSize: 11, marginTop: 5, lineHeight: 16 },
  input: { minHeight: 48, backgroundColor: '#F7F9FA', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: Colors.border, color: Colors.text, fontSize: 15 },
  chip: { minHeight: 44, paddingHorizontal: 15, borderRadius: 12, backgroundColor: '#FFF', borderWidth: 1, borderColor: Colors.border, alignItems: 'center', justifyContent: 'center', flexDirection: 'row', gap: 7 },
  chipSelected: { backgroundColor: Colors.primary, borderColor: Colors.primary },
  chipText: { fontSize: 13, fontWeight: '600', color: Colors.textSecondary },
});
