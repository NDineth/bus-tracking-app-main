import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, ScrollView, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { useAlertPolicy } from '../../context/AlertPolicyContext';
import { DEFAULT_POLICY, SEVERITIES, createPolicyDraft, policyHasChanges, scoreSeverity, validatePolicy, validateScorePolicy } from '../../utils/alertPolicy';
import { Button, ErrorNotice, SectionTitle, ui } from '../../components/UI';
import { errorMessage } from '../../services/api';
import Colors from '../../constants/Colors';

const scoreFields = [['criticalBelow', 'Critical below', 'Immediate attention'], ['highRiskBelow', 'High risk below', 'Review with the driver'], ['cautionBelow', 'Caution below', 'Monitor driving patterns']];
const eventFields = [['highSpeedEvents', 'High speed', 'speedometer-outline'], ['brakingEvents', 'Sudden braking', 'hand-left-outline'], ['accelerationEvents', 'Sudden acceleration', 'trending-up-outline'], ['turningEvents', 'Sudden turning', 'git-compare-outline']];

export default function SettingsScreen() {
  const { user, logout } = useAuth();
  const { policy, loading, loaded, error, refreshPolicy, savePolicy } = useAlertPolicy();
  const [draft, setDraft] = useState(() => createPolicyDraft(policy));
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [sample, setSample] = useState('65');
  const admin = user?.role === 'ADMIN';
  useEffect(() => { setDraft(createPolicyDraft(policy)); }, [policy]);
  const dirty = policyHasChanges(draft, policy);
  const validation = validatePolicy(draft);
  const previewPolicy = validateScorePolicy(draft) ? policy : Object.fromEntries(Object.entries(draft).map(([key, value]) => [key, Number(value)]));
  const sampleNumber = String(sample).trim() === '' ? null : Number(sample);
  const validSample = sampleNumber !== null && Number.isFinite(sampleNumber) && sampleNumber >= 0 && sampleNumber <= 100;
  const risk = SEVERITIES[validSample ? scoreSeverity(sampleNumber, previewPolicy) : 'INFO'];
  const change = (key, value) => { setDraft(prev => ({ ...prev, [key]: value })); setSuccess(false); setSaveError(null); };
  const save = async () => {
    setSaving(true); setSaveError(null); setSuccess(false);
    try { await savePolicy(draft); setSuccess(true); }
    catch (e) { setSaveError(errorMessage(e)); }
    finally { setSaving(false); }
  };
  return <KeyboardAvoidingView style={ui.page} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
    <ScrollView contentContainerStyle={[ui.content, { maxWidth: 900 }]} keyboardShouldPersistTaps="handled">
      <View><Text style={ui.eyebrow}>PREFERENCES / SAFETY POLICY</Text><Text style={styles.title}>Make every alert count.</Text><Text style={ui.subtitle}>Understand your safety score and manage when your team is notified.</Text></View>
      <View style={[ui.card, styles.profile]}><View style={styles.avatar}><Ionicons name="person-outline" size={26} color={Colors.primary} /></View><View style={{ flex: 1 }}><Text style={styles.profileName}>{typeof user?.name === 'string' && user.name ? user.name : [user?.firstName, user?.lastName].filter(Boolean).join(' ') || 'Your account'}</Text><Text style={ui.subtitle}>{user?.role?.replace(/_/g, ' ')}</Text></View><Button title="Sign out" secondary icon="log-out-outline" onPress={async () => { try { await logout(); } catch (e) { setSaveError(errorMessage(e)); } }} /></View>
      <ErrorNotice message={error ? `Could not load the active policy. ${loaded ? 'Showing the last loaded values.' : 'Showing default score bands. Driving event thresholds are unavailable.'} ${error}` : null} onRetry={refreshPolicy} />
      <View style={styles.notice}><Ionicons name={admin ? 'shield-checkmark-outline' : 'lock-closed-outline'} size={22} color={Colors.primary} /><Text style={[ui.subtitle, { flex: 1, marginTop: 0 }]}>{admin ? 'These settings apply to the whole fleet. Saved changes apply to future driving events.' : 'The fleet administrator manages these settings. You can review the current thresholds and try a score below.'}</Text></View>
      <View style={ui.card}>
        <SectionTitle title="Safety score thresholds" subtitle="0–100 · higher scores mean safer driving" />
        <Text style={ui.subtitle}>A score below a boundary enters that risk band. A score equal to a boundary enters the next, safer band.</Text>
        <View style={styles.fieldGrid}>{scoreFields.map(([key, label, hint], index) => <View key={key} style={styles.field}>
          <Text style={[styles.label, { color: SEVERITIES[['CRITICAL', 'HIGH', 'CAUTION'][index]].color }]}>{label}</Text>
          <TextInput accessibilityLabel={label} style={[ui.input, styles.number, (!admin || !loaded) && styles.readOnly]} keyboardType="number-pad" maxLength={3} value={String(draft[key])} editable={admin && loaded && !saving} onChangeText={value => change(key, value)} />
          <Text style={ui.subtitle}>{hint}</Text></View>)}</View>
        <View style={styles.bands}>{[['CRITICAL', `0–${previewPolicy.criticalBelow - 1}`], ['HIGH', `${previewPolicy.criticalBelow}–${previewPolicy.highRiskBelow - 1}`], ['CAUTION', `${previewPolicy.highRiskBelow}–${previewPolicy.cautionBelow - 1}`], ['LOW', `${previewPolicy.cautionBelow}–100`]].map(([id, range]) => <View key={id} style={[styles.band, { backgroundColor: SEVERITIES[id].background }]}><Text style={[styles.bandLabel, { color: SEVERITIES[id].color }]}>{SEVERITIES[id].label}</Text><Text style={[styles.bandValue, { color: SEVERITIES[id].color }]}>{range}</Text></View>)}</View>
        {dirty && validation && <Text accessibilityRole="alert" style={styles.validation}>{validation}</Text>}
      </View>
      <View style={ui.card}><SectionTitle title="Try a safety score" subtitle={dirty ? 'Previewing your unsaved threshold changes' : 'See how the active thresholds classify an event'} />
        <View style={styles.preview}><TextInput value={sample} onChangeText={setSample} accessibilityLabel="Safety score preview" keyboardType="numeric" maxLength={5} style={[ui.input, styles.sample]} />
          <View style={[styles.previewResult, { backgroundColor: risk.background }]}><Ionicons name={risk.icon} size={26} color={risk.color} /><View style={{ flex: 1 }}><Text style={[styles.profileName, { color: risk.color }]}>{validSample ? risk.label : 'Enter a score from 0 to 100'}</Text><Text style={ui.subtitle}>{validSample ? `${sampleNumber} / 100 safety score` : 'Use a number in the valid range.'}</Text></View></View></View>
      </View>
      <View style={ui.card}><SectionTitle title="Driving event thresholds" subtitle="Number of events before a behavior warning is sent" />
        <Text style={ui.subtitle}>{loading && !loaded ? 'Loading event thresholds...' : 'Counts come from the saved fleet policy. Values that have not been configured remain empty.'}</Text>
        {eventFields.map(([key, label, icon]) => <View key={key} style={styles.eventRow}><Ionicons name={icon} size={22} color={Colors.primary} /><Text style={[styles.label, { flex: 1 }]}>{label}</Text>
          <TextInput accessibilityLabel={`${label} event threshold`} value={String(draft[key] ?? '')} placeholder={admin && loaded ? 'Enter' : 'Not set'} placeholderTextColor={Colors.muted} keyboardType="number-pad" maxLength={3} style={[ui.input, styles.eventNumber, (!admin || !loaded) && styles.readOnly]} editable={admin && loaded && !saving} onChangeText={value => change(key, value)} /><Text style={ui.subtitle}>events</Text></View>)}
        <View style={[styles.notice, { marginTop: 16, backgroundColor: '#FCECEF' }]}><Ionicons name="alert-circle-outline" size={23} color={Colors.error} /><Text style={[ui.subtitle, { flex: 1, marginTop: 0, color: Colors.error }]}>Crash detection always sends an immediate critical alert. This rule cannot be disabled.</Text></View>
      </View>
      <View style={ui.card}><SectionTitle title="How the score works" />
        <Text style={ui.subtitle}>Each sensor batch starts at 100. Harsh braking deducts 5 points per reading, aggressive acceleration deducts 4, and dangerous turning deducts 3. The minimum is 0.</Text>
        <Text style={ui.subtitle}>Scored notifications are sent below the caution boundary. Driving events are still recorded when a batch has a low risk score. Behavior warning counts are separate from this score.</Text>
      </View>
      <ErrorNotice message={saveError} />
      {success && <View accessibilityRole="alert" style={styles.notice}><Ionicons name="checkmark-circle-outline" size={22} color={Colors.primary} /><Text style={{ color: Colors.primary, fontWeight: '600', flex: 1 }}>Thresholds saved. Future events will use this policy.</Text></View>}
      {admin && <View style={styles.actions}><Button title={saving ? 'Saving thresholds…' : 'Save thresholds'} icon="checkmark-outline" onPress={save} loading={saving} disabled={!loaded || loading || !!validation || !dirty} style={{ flex: 1 }} />
        <Button title="Reset score thresholds" secondary onPress={() => { setDraft(previous => ({ ...previous, ...DEFAULT_POLICY })); setSuccess(false); }} disabled={!loaded || saving} />
        {dirty && <Button title="Discard" secondary onPress={() => { setDraft(createPolicyDraft(policy)); setSuccess(false); setSaveError(null); }} disabled={saving} />}</View>}
    </ScrollView>
  </KeyboardAvoidingView>;
}
const styles = StyleSheet.create({ title: { fontSize: 30, lineHeight: 38, color: Colors.text, fontWeight: '800', letterSpacing: -0.7, marginTop: 10 },
  profile: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 14 }, avatar: { padding: 14, borderRadius: 16, backgroundColor: Colors.primaryLight }, profileName: { fontSize: 16, fontWeight: '700', color: Colors.text },
  notice: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, borderRadius: 14, backgroundColor: Colors.primaryLight },
  fieldGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 16, marginTop: 22 }, field: { flex: 1, minWidth: 160 }, label: { color: Colors.text, fontWeight: '600', fontSize: 13 }, number: { fontSize: 25, fontWeight: '700', marginTop: 10 }, readOnly: { backgroundColor: Colors.background },
  bands: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 20 }, band: { flex: 1, minWidth: 110, padding: 14, borderRadius: 12 }, bandLabel: { fontWeight: '600', fontSize: 11 }, bandValue: { fontWeight: '800', fontSize: 20, marginTop: 5 }, validation: { color: Colors.error, lineHeight: 21, marginTop: 15 },
  preview: { flexDirection: 'row', flexWrap: 'wrap', gap: 14 }, sample: { width: 100, fontSize: 28, fontWeight: '700', textAlign: 'center' }, previewResult: { flex: 1, minWidth: 160, flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, borderRadius: 14 },
  eventRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 13, borderBottomWidth: 1, borderBottomColor: Colors.border }, eventNumber: { width: 88, textAlign: 'center', fontWeight: '700' }, actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
});
