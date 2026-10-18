import React, { useMemo, useState } from 'react';
import { View, Text, Modal, FlatList, TextInput, TouchableOpacity, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { ui } from './UI';
import Colors from '../constants/Colors';
import { turnLabel } from '../utils/drivingMetrics';

export default function TurnSelector({ turns, selectedId, onSelect }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const selected = turns.find(turn => String(turn.id) === String(selectedId));
  const matches = useMemo(() => turns.filter(turn => `${turnLabel(turn)} ${turn.tripLabel || ''} ${turn.status || ''} ${turn.startTime || ''}`.toLowerCase().includes(query.trim().toLowerCase())), [turns, query]);
  return <>
    <TouchableOpacity accessibilityRole="button" accessibilityLabel="Select turn" accessibilityState={{ expanded: open, disabled: !turns.length }}
      disabled={!turns.length} style={[ui.card, styles.trigger]} onPress={() => { setQuery(''); setOpen(true); }}>
      <Ionicons name="calendar-outline" size={24} color={Colors.primary} />
      <View style={{ flex: 1 }}><Text style={ui.eyebrow}>SELECT TURN</Text>
        <Text style={[ui.sectionTitle, { fontSize: 16, marginTop: 6 }]}>{selected ? turnLabel(selected) : 'Choose a turn'}</Text>
        <Text style={ui.subtitle}>{selected ? selected.tripLabel : turns.length ? `${turns.length} turns available` : 'No turns available'}</Text></View>
      <Ionicons name="chevron-down" size={20} color={Colors.primary} />
    </TouchableOpacity>
    <Modal visible={open} animationType="slide" onRequestClose={() => setOpen(false)}>
      <SafeAreaView style={ui.page}>
        <View style={[ui.content, { flex: 1, paddingBottom: 16 }]}>
          <View style={styles.trigger}><Text style={[ui.sectionTitle, { flex: 1 }]}>Select a turn</Text>
            <TouchableOpacity accessibilityRole="button" accessibilityLabel="Close turn selector" onPress={() => setOpen(false)} style={ui.linkButton}><Ionicons name="close" size={24} color={Colors.text} /></TouchableOpacity></View>
          <TextInput accessibilityLabel="Search turns" value={query} onChangeText={setQuery} placeholder="Search date, route or turn" placeholderTextColor={Colors.muted} style={ui.input} />
          <FlatList data={matches} keyExtractor={turn => String(turn.id)} contentContainerStyle={{ gap: 12 }}
            ListEmptyComponent={<Text style={ui.subtitle}>No matching turns.</Text>}
            renderItem={({ item }) => {
              const active = String(item.id) === String(selectedId);
              return <TouchableOpacity accessibilityRole="radio" accessibilityState={{ checked: active }} accessibilityLabel={`${turnLabel(item)}, ${item.tripLabel || ''}`}
                style={[ui.card, styles.trigger, active && styles.active]} onPress={() => { onSelect(String(item.id)); setOpen(false); }}>
                <View style={{ flex: 1 }}><Text style={[ui.sectionTitle, { fontSize: 16 }]}>{turnLabel(item)}</Text>
                  <Text style={ui.subtitle}>{item.tripLabel}</Text><Text style={ui.subtitle}>{item.status?.replace(/_/g, ' ')}</Text></View>
                <Ionicons name={active ? 'radio-button-on' : 'radio-button-off'} size={24} color={active ? Colors.primary : Colors.muted} />
              </TouchableOpacity>;
            }} />
        </View>
      </SafeAreaView>
    </Modal>
  </>;
}
const styles = StyleSheet.create({ trigger: { flexDirection: 'row', alignItems: 'center', gap: 12 }, active: { borderColor: Colors.primary, backgroundColor: Colors.primaryLight } });
