import React, { useMemo, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import Colors from '../constants/Colors';
import { ui } from './UI';
import { graphBuckets } from '../utils/drivingMetrics';

export const readingTime = value => value ? new Date(value).toLocaleTimeString('en-LK', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '—';

export default function TurnGraph({ title, readings, field, unit, color, aggregate = 'max', ceiling, description }) {
  const [selected, setSelected] = useState(null);
  const buckets = useMemo(() => {
    return graphBuckets(readings, field, aggregate);
  }, [readings, field, aggregate]);
  const values = buckets.filter(bucket => bucket.value !== null).map(bucket => bucket.value);
  const max = ceiling || Math.max(1, ...values);
  const active = selected !== null ? buckets[selected] : null;
  const hasData = readings.some(reading => reading[field] !== null && reading[field] !== undefined);
  return <View style={ui.card}>
    <Text style={ui.sectionTitle}>{title}</Text>
    <Text style={ui.subtitle}>{description || `${unit} · ${aggregate === 'sum' ? 'Events per interval' : aggregate === 'min' ? 'Lowest score per interval' : 'Peak speed per interval'}`}</Text>
    {!hasData ? <Text style={[ui.subtitle, { paddingVertical: 24 }]}>No {title.toLowerCase()} readings recorded for this turn.</Text> : <>
      <View style={styles.chart}>
        <View style={styles.scale}><Text style={styles.tick}>{max}</Text><Text style={styles.tick}>0</Text></View>
        <View style={styles.plot}>
          {[0, 0.5, 1].map(fraction => <View key={fraction} style={[styles.grid, { bottom: `${fraction * 100}%` }]} />)}
          {buckets.map((bucket, index) => <TouchableOpacity key={index} accessibilityRole="button"
            accessibilityLabel={`${title}, ${readingTime(bucket.time)}, ${bucket.value === null ? 'No data' : `${bucket.value} ${unit}`}`}
            accessibilityState={{ selected: selected === index }} onPress={() => setSelected(index)}
            style={[styles.column, selected === index && { backgroundColor: Colors.primaryLight }]}>
            <View style={{ width: '80%', height: bucket.value === null ? 4 : Math.max(2, bucket.value / max * 130),
              borderRadius: 3, backgroundColor: bucket.value === null ? '#CAD3D9' : color, opacity: selected === null || selected === index ? 1 : 0.5 }} />
          </TouchableOpacity>)}
        </View>
      </View>
      <View style={styles.times}><Text style={styles.tick}>{readingTime(readings[0]?.recordedAt)}</Text><Text style={styles.tick}>{readingTime(readings[readings.length - 1]?.recordedAt)}</Text></View>
      <Text style={ui.subtitle}>{active ? `${readingTime(active.time)} · ${active.value === null ? 'No complete data' : `${active.value} ${unit}`}` : 'Tap a bar to see its time and value. Grey indicates missing data.'}</Text>
    </>}
  </View>;
}
const styles = StyleSheet.create({ chart: { flexDirection: 'row', gap: 8, marginTop: 24, marginBottom: 12, height: 130 },
  scale: { justifyContent: 'space-between', minWidth: 28 }, tick: { fontSize: 10, color: Colors.textSecondary },
  plot: { flex: 1, flexDirection: 'row', alignItems: 'flex-end' }, grid: { position: 'absolute', left: 0, right: 0, height: 1, backgroundColor: Colors.border },
  column: { flex: 1, height: 130, justifyContent: 'flex-end', alignItems: 'center', minWidth: 1 },
  times: { flexDirection: 'row', justifyContent: 'space-between', marginLeft: 36 } });
