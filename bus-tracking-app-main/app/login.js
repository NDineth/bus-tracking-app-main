import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform, ScrollView, useWindowDimensions } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Link, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../context/AuthContext';
import { Button, ErrorNotice, ui } from '../components/UI';
import { errorMessage } from '../services/api';
import Colors from '../constants/Colors';

export default function LoginScreen() {
  const params = useLocalSearchParams();
  const [email, setEmail] = useState(typeof params.email === 'string' ? params.email : '');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const { login } = useAuth();
  const wide = useWindowDimensions().width >= 900;
  const submit = async () => {
    if (busy) return;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) || !password) {
      setError('Enter your email address and password to continue.'); return;
    }
    setError(null); setBusy(true);
    try { await login(email.trim(), password); }
    catch (e) { setError(errorMessage(e)); }
    finally { setBusy(false); }
  };
  return <SafeAreaView style={ui.page}><KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
    <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
      <View style={[styles.shell, wide && { flexDirection: 'row' }]}>
        <View style={[styles.brand, wide && { flex: 1, minHeight: 570 }]}>
          <View style={styles.wordmark}><View style={styles.logo}><Ionicons name="bus" size={24} color={Colors.navy} /></View><Text style={styles.brandName}>BUS TRACK PRO</Text></View>
          <View style={{ marginTop: wide ? 70 : 30 }}><Text style={styles.brandEyebrow}>A BETTER WAY FORWARD</Text><Text style={styles.brandTitle}>Connected journeys.{`\n`}Confident decisions.</Text><Text style={styles.brandDescription}>A single place for your fleet, schedules, earnings and driving safety.</Text></View>
          {wide && <View style={styles.features}>{[['navigate-outline', 'Know your next move', 'Your schedule and agenda, always close.'], ['shield-checkmark-outline', 'Keep safety in sight', 'Clear alerts and meaningful safety scores.'], ['stats-chart-outline', 'Make every trip count', 'Understand fees, commissions and daily totals.']].map(([icon, title, description]) => <View key={title} style={styles.feature}><Ionicons name={icon} size={23} color="#B4D6C9" /><View style={{ flex: 1 }}><Text style={styles.featureTitle}>{title}</Text><Text style={styles.featureDescription}>{description}</Text></View></View>)}</View>}
        </View>
        <View style={[styles.form, wide && { flex: 1 }]}>
          <Text style={ui.eyebrow}>YOUR OPERATIONS WORKSPACE</Text><Text style={styles.title}>Welcome back.</Text><Text style={[ui.subtitle, { marginBottom: 26 }]}>Sign in to get your day moving.</Text>
          <ErrorNotice message={error} />
          {params.registered === '1' && <View accessibilityRole="alert" style={{ padding: 14, borderRadius: 12, backgroundColor: Colors.primaryLight }}><Text style={{ color: Colors.primary, lineHeight: 21 }}>Account created successfully. Sign in to continue.</Text></View>}
          <Text style={styles.label}>Email address</Text><TextInput accessibilityLabel="Email address" style={ui.input} placeholder="you@example.com" placeholderTextColor={Colors.muted} value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" autoCorrect={false} autoComplete="email" editable={!busy} />
          <Text style={styles.label}>Password</Text><View style={styles.password}><TextInput accessibilityLabel="Password" style={styles.passwordInput} placeholder="Enter your password" placeholderTextColor={Colors.muted} value={password} onChangeText={setPassword} secureTextEntry={!showPassword} autoComplete="current-password" editable={!busy} onSubmitEditing={submit} />
            <TouchableOpacity accessibilityRole="button" accessibilityLabel={showPassword ? 'Hide password' : 'Show password'} onPress={() => setShowPassword(!showPassword)} style={styles.eye}><Ionicons name={showPassword ? 'eye-off-outline' : 'eye-outline'} size={21} color={Colors.textSecondary} /></TouchableOpacity></View>
          <Button title={busy ? 'Signing in…' : 'Sign in'} icon="arrow-forward" onPress={submit} loading={busy} style={{ marginTop: 26 }} />
          <View style={styles.footer}><Text style={ui.subtitle}>New to Bus Track Pro?</Text><Link href="/register" style={styles.link}>Create an account</Link></View>
          <View style={styles.hint}><Ionicons name="lock-closed-outline" size={14} color={Colors.textSecondary} /><Text style={styles.hintText}>Your workspace is tailored to your account role.</Text></View>
        </View>
      </View>
    </ScrollView>
  </KeyboardAvoidingView></SafeAreaView>;
}
const styles = StyleSheet.create({ scroll: { flexGrow: 1, justifyContent: 'center', padding: 20 }, shell: { width: '100%', maxWidth: 1050, alignSelf: 'center', backgroundColor: '#FFF', borderRadius: 26, overflow: 'hidden', borderWidth: 1, borderColor: Colors.border },
  brand: { backgroundColor: Colors.navy, padding: 32 }, wordmark: { flexDirection: 'row', gap: 12, alignItems: 'center' }, logo: { padding: 11, borderRadius: 13, backgroundColor: '#B8D7C9' }, brandName: { color: '#FFF', fontWeight: '800', letterSpacing: 1.3, fontSize: 14 }, brandEyebrow: { color: '#A8CDC0', fontSize: 10, fontWeight: '700', letterSpacing: 2 }, brandTitle: { color: '#FFF', fontSize: 31, lineHeight: 40, fontWeight: '700', letterSpacing: -0.6, marginTop: 16 }, brandDescription: { color: '#B4C9D3', fontSize: 14, lineHeight: 23, marginTop: 16 },
  features: { gap: 22, marginTop: 45 }, feature: { flexDirection: 'row', gap: 16, alignItems: 'center' }, featureTitle: { color: '#E1EBE8', fontWeight: '600', fontSize: 13 }, featureDescription: { color: '#A1B9C4', fontSize: 11, lineHeight: 19, marginTop: 3 },
  form: { padding: 30, justifyContent: 'center' }, title: { color: Colors.text, fontSize: 32, letterSpacing: -1, fontWeight: '800', marginTop: 12 }, label: { color: Colors.text, fontSize: 13, fontWeight: '600', marginTop: 20, marginBottom: 9 },
  password: { flexDirection: 'row', backgroundColor: '#F7F9FA', borderRadius: 12, borderWidth: 1, borderColor: Colors.border }, passwordInput: { flex: 1, padding: 14, minHeight: 48, fontSize: 15, color: Colors.text }, eye: { minWidth: 48, alignItems: 'center', justifyContent: 'center' }, footer: { gap: 4, alignItems: 'center', marginTop: 24 }, link: { color: Colors.primary, fontWeight: '700', fontSize: 13, paddingVertical: 10 }, hint: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 6, marginTop: 20 }, hintText: { fontSize: 10, color: Colors.textSecondary, flexShrink: 1 },
});
