import { showAlert } from '../utils/alertService';
import { SafeAreaView } from 'react-native-safe-area-context';
import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator, StatusBar } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { getRouteSchedule, completeAssignment } from '../services/busService';
import Colors from '../constants/Colors';
import { showError } from '../utils/alertService';
import { useAuth } from '../context/AuthContext';
import { ErrorNotice, Metric, ui } from '../components/UI';
import { errorMessage } from '../services/api';


export default function BusScheduleScreen() {
  const { busId, busNumber, route } = useLocalSearchParams();
  const [schedule, setSchedule] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [completing, setCompleting] = useState(null);
  const { user } = useAuth();
  const router = useRouter();

  const fetchSchedule = async () => {
    try {
      const data = await getRouteSchedule(route, busNumber);
      setSchedule(data);
      setError(null);
    } catch (error) {
      setError(errorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSchedule();
  }, [route, busNumber]);

  const handleCompleteRun = async (assignmentId) => {
    if (completing) return;
    setCompleting(assignmentId);
    try {
      const data = await completeAssignment(assignmentId);
      showAlert(
        'Run Completed Successfully',
        `Permit Fee: LKR ${data.permitFee ? Number(data.permitFee).toFixed(2) : '0.00'}\n` +
        `Off-Peak Commission: LKR ${data.offPeakCommission ? Number(data.offPeakCommission).toFixed(2) : '0.00'}\n` +
        `Is Off-Peak: ${data.isOffPeak ? 'Yes' : 'No'}`,
        [{ text: 'OK' }]
      );
      setIsLoading(true);
      await fetchSchedule();
    } catch (error) {
      showError('Complete Run', error);
    } finally {
      setCompleting(null);
    }
  };

  const renderItem = ({ item }) => {
    const isYourTurn = item.isYourTurn;
    const isPast = item.status === 'PAST';
    const isCurrent = item.status === 'CURRENT';

    return (
      <View style={[
        styles.turnCard,
        isYourTurn && styles.highlightedCard,
        isPast && styles.pastCard
      ]}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <View style={styles.timeContainer}>
            <Text style={[styles.turnNumber, isPast && styles.pastText]}>Turn {item.turn}</Text>
            <Text style={[styles.timeText, isPast && styles.pastText]}>{item.time}</Text>
          </View>

          <View style={styles.detailsContainer}>
            <View style={styles.busInfo}>
              <Ionicons
                name="bus"
                size={18}
                color={isYourTurn ? Colors.primary : (isPast ? '#617381' : '#172D3B')}
              />
              <Text style={[
                styles.busNumber,
                isYourTurn && styles.highlightedBusText,
                isPast && styles.pastText
              ]}>
                {item.bus}
              </Text>
            </View>

            {isYourTurn && (
              <View style={styles.yourTurnBadge}>
                <Text style={styles.yourTurnText}>YOUR TURN</Text>
              </View>
            )}

            {isCurrent && !isYourTurn && (
              <View style={styles.currentBadge}>
                <Text style={styles.currentText}>ON ROAD</Text>
              </View>
            )}

            {item.rawStatus === 'TURN_LOSS' && (
              <View style={styles.turnLossBadge}>
                <Text style={styles.turnLossText}>TURN LOSS</Text>
              </View>
            )}
          </View>
        </View>

        {isPast && item.permitFee !== undefined && item.permitFee !== null && (
          <View style={styles.feeContainer}>
            <View style={styles.feeRow}>
              <Text style={styles.feeLabel}>Permit Fee:</Text>
              <Text style={styles.feeValue}>LKR {Number(item.permitFee).toFixed(2)}</Text>
            </View>
            {item.isOffPeak && (
              <View style={styles.feeRow}>
                <Text style={styles.commissionLabel}>Off-Peak Commission (Cr):</Text>
                <Text style={styles.commissionValue}>- LKR {Number(item.offPeakCommission).toFixed(2)}</Text>
              </View>
            )}
          </View>
        )}

        {isYourTurn && isCurrent && user?.role === 'BUS_DRIVER' && (
          <TouchableOpacity
            style={styles.completeRunButton}
            onPress={() => handleCompleteRun(item.id)}
            disabled={completing !== null}
          >
            <Ionicons name="checkmark-circle-outline" size={18} color="#FFF" />
            <Text style={styles.completeRunButtonText}>{completing === item.id ? 'Completing…' : 'Complete Run'}</Text>
          </TouchableOpacity>
        )}

        {isCurrent && (
          <View style={styles.currentIndicator} />
        )}
      </View>
    );
  };


  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#000" />
        </TouchableOpacity>
        <View style={styles.headerTitleContainer}>
          <Text style={styles.headerTitle}>Running Schedule</Text>
          <Text style={styles.headerSubtitle}>{route || 'Route Schedule'}</Text>
        </View>
        <TouchableOpacity
          onPress={() => router.push({
            pathname: '/bus-agenda',
            params: { busNumber }
          })}
          style={styles.agendaButton}
        >
          <Ionicons name="calendar" size={24} color={Colors.primary} />
        </TouchableOpacity>
      </View>

      <View style={styles.busSelectionHeader}>
        <View style={styles.busChip}>
          <Ionicons name="bus-outline" size={16} color={Colors.primary} />
          <Text style={styles.busChipText}>{busNumber || 'Selected Bus'}</Text>
        </View>
        <Text style={styles.dayLabel}>Today's Timetable</Text>
      </View>

      {isLoading ? (
        <View style={styles.loaderContainer}>
          <ActivityIndicator size="large" color={Colors.primary} />
          <Text style={styles.loadingText}>Loading schedule...</Text>
        </View>
      ) : (
        <FlatList
          data={schedule}
          renderItem={renderItem}
          keyExtractor={(item) => item.turn.toString()}
          contentContainerStyle={styles.listContent}
          refreshing={isLoading}
          onRefresh={fetchSchedule}
          ListHeaderComponent={<View style={{ gap: 12, marginBottom: 18 }}><ErrorNotice message={error} onRetry={fetchSchedule} /><View style={ui.metrics}><Metric label="Your turns" value={schedule.filter(s => s.isYourTurn).length} icon="bus-outline" /><Metric label="Completed" value={schedule.filter(s => s.isYourTurn && s.rawStatus === 'COMPLETED').length} icon="checkmark-circle-outline" /><Metric label="Upcoming" value={schedule.filter(s => s.isYourTurn && s.status === 'UPCOMING').length} icon="time-outline" /></View></View>}
          showsVerticalScrollIndicator={false}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Ionicons name="calendar-outline" size={64} color="#C7C7CC" />
              <Text style={styles.emptyText}>No schedule available for this route.</Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F4F6F8',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#FFF',
    borderBottomWidth: 1,
    borderBottomColor: '#EBEBEB',
  },
  backButton: {
    padding: 8,
    borderRadius: 12,
    backgroundColor: '#F0F4F5',
  },
  agendaButton: {
    padding: 8,
    borderRadius: 12,
    backgroundColor: Colors.primary + '10',
  },
  headerTitleContainer: {
    flex: 1,
    paddingHorizontal: 12,
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#000',
  },
  headerSubtitle: {
    fontSize: 12,
    color: '#617381',
    marginTop: 2,
  },
  busSelectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 16,
  },
  busChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.primary + '15',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.primary + '30',
  },
  busChipText: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.primary,
    marginLeft: 6,
  },
  dayLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#617381',
  },
  listContent: {
    width: '100%',
    maxWidth: 1000,
    alignSelf: 'center',
    paddingHorizontal: 16,
    paddingBottom: 30,
  },
  turnCard: {
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    flexDirection: 'column',
    alignItems: 'stretch',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
    borderLeftWidth: 4,
    borderLeftColor: 'transparent',
  },
  feeContainer: {
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#F0F4F5',
  },
  feeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  feeLabel: {
    fontSize: 13,
    color: '#617381',
  },
  feeValue: {
    fontSize: 13,
    fontWeight: '700',
    color: '#172D3B',
  },
  commissionLabel: {
    fontSize: 13,
    color: '#9A650A',
  },
  commissionValue: {
    fontSize: 13,
    fontWeight: '700',
    color: '#9A650A',
  },
  completeRunButton: {
    marginTop: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#176B60',
    paddingVertical: 10,
    borderRadius: 10,
  },
  completeRunButtonText: {
    color: '#FFF',
    fontSize: 14,
    fontWeight: '700',
    marginLeft: 6,
  },

  highlightedCard: {
    backgroundColor: Colors.primary + '05',
    borderColor: Colors.primary + '30',
    borderWidth: 1,
    borderLeftColor: Colors.primary,
  },
  pastCard: {
    opacity: 0.6,
    backgroundColor: '#F0F4F5',
  },
  timeContainer: {
    width: 90,
    borderRightWidth: 1,
    borderRightColor: '#EBEBEB',
    marginRight: 16,
  },
  turnNumber: {
    fontSize: 12,
    color: '#617381',
    fontWeight: '600',
    marginBottom: 2,
  },
  timeText: {
    fontSize: 16,
    fontWeight: '700',
    color: '#172D3B',
  },
  detailsContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  busInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  busNumber: {
    fontSize: 15,
    fontWeight: '600',
    color: '#172D3B',
    marginLeft: 8,
  },
  highlightedBusText: {
    color: Colors.primary,
    fontWeight: '700',
  },
  pastText: {
    color: '#617381',
  },
  yourTurnBadge: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  yourTurnText: {
    color: '#FFF',
    fontSize: 10,
    fontWeight: '800',
  },
  currentBadge: {
    backgroundColor: '#176B6020',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  currentText: {
    color: '#176B60',
    fontSize: 10,
    fontWeight: '800',
  },
  turnLossBadge: {
    backgroundColor: '#BD334715',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  turnLossText: {
    color: '#BD3347',
    fontSize: 10,
    fontWeight: '800',
  },
  currentIndicator: {
    position: 'absolute',
    right: 0,
    top: '50%',
    marginTop: -4,
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#176B60',
    marginRight: 12,
  },
  loaderContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#617381',
  },
  emptyContainer: {
    marginTop: 100,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    marginTop: 16,
    fontSize: 16,
    color: '#617381',
    textAlign: 'center',
    paddingHorizontal: 40,
  }
});
