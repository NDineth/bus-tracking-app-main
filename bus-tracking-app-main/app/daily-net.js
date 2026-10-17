import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator, SafeAreaView, StatusBar } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { getDailyNetSummary } from '../services/busService';
import Colors from '../constants/Colors';
import { showError } from '../utils/alertService';

export default function DailyNetScreen() {
  const { busId, busNumber } = useLocalSearchParams();
  const [summary, setSummary] = useState(null);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  const fetchSummary = useCallback(async () => {
    if (!busId) {
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    try {
      const dateStr = selectedDate.toISOString().split('T')[0];
      const data = await getDailyNetSummary(busId, dateStr);
      setSummary(data);
    } catch (error) {
      showError('Daily Net Summary', error);
    } finally {
      setIsLoading(false);
    }
  }, [busId, selectedDate]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  const changeDate = (days) => {
    const newDate = new Date(selectedDate);
    newDate.setDate(selectedDate.getDate() + days);
    setSelectedDate(newDate);
  };

  const formatDateLabel = (date) => {
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const renderRunItem = ({ item }) => {
    return (
      <View style={styles.runCard}>
        <View style={styles.runHeader}>
          <View style={styles.runTimeContainer}>
            <Ionicons name="time-outline" size={16} color="#8E8E93" />
            <Text style={styles.runTimeText}>{item.departureTime} - {item.arrivalTime}</Text>
          </View>
          {item.isOffPeak && (
            <View style={styles.offPeakBadge}>
              <Text style={styles.offPeakText}>OFF-PEAK</Text>
            </View>
          )}
        </View>

        <Text style={styles.runRoute}>{item.tripLabel || 'Colombo - Kandy (Express)'}</Text>

        <View style={styles.runBreakdown}>
          <View style={styles.breakdownRow}>
            <Text style={styles.breakdownLabel}>Permit Fee:</Text>
            <Text style={styles.breakdownValue}>LKR {Number(item.permitFee).toFixed(2)}</Text>
          </View>
          {item.isOffPeak && (
            <View style={styles.breakdownRow}>
              <Text style={styles.breakdownLabelCommission}>Off-Peak Commission:</Text>
              <Text style={styles.breakdownValueCommission}>- LKR {Number(item.offPeakCommission).toFixed(2)}</Text>
            </View>
          )}
          {item.delayMinutes > 0 && (
            <View style={styles.breakdownRow}>
              <Text style={styles.breakdownLabelFine}>Delay Penalty (Level {item.penaltyLevel}):</Text>
              <Text style={styles.breakdownValueFine}>+ LKR {Number(item.fineAmount || 0).toFixed(2)} ({item.delayMinutes}m late)</Text>
            </View>
          )}
        </View>
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
          <Text style={styles.headerTitle}>Daily Net Report</Text>
          <Text style={styles.headerSubtitle}>{busNumber || 'Bus Financials'}</Text>
        </View>
        <TouchableOpacity onPress={fetchSummary} style={styles.refreshButton}>
          <Ionicons name="refresh" size={22} color={Colors.primary} />
        </TouchableOpacity>
      </View>

      {/* Date Picker Ribbon */}
      <View style={styles.datePickerContainer}>
        <TouchableOpacity onPress={() => changeDate(-1)} style={styles.arrowButton}>
          <Ionicons name="chevron-back" size={20} color={Colors.primary} />
        </TouchableOpacity>
        <Text style={styles.dateLabel}>{formatDateLabel(selectedDate)}</Text>
        <TouchableOpacity onPress={() => changeDate(1)} style={styles.arrowButton}>
          <Ionicons name="chevron-forward" size={20} color={Colors.primary} />
        </TouchableOpacity>
      </View>

      {isLoading ? (
        <View style={styles.loaderContainer}>
          <ActivityIndicator size="large" color={Colors.primary} />
          <Text style={styles.loadingText}>Fetching financial summary...</Text>
        </View>
      ) : (
        <FlatList
          data={summary?.runs || []}
          renderItem={renderRunItem}
          keyExtractor={(item) => item.id.toString()}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
          ListHeaderComponent={
            <View style={styles.summaryContainer}>
              {/* Gross Permit Fees */}
              <View style={styles.summaryRow}>
                <View style={styles.summaryLabelGroup}>
                  <View style={[styles.dot, { backgroundColor: Colors.primary }]} />
                  <Text style={styles.summaryLabel}>Total Permit Fees</Text>
                </View>
                <Text style={styles.summaryValue}>LKR {summary ? Number(summary.totalPermitFees).toFixed(2) : '0.00'}</Text>
              </View>

              {/* Discount Deducted */}
              <View style={styles.summaryRow}>
                <View style={styles.summaryLabelGroup}>
                  <View style={[styles.dot, { backgroundColor: '#FF9500' }]} />
                  <Text style={styles.summaryLabel}>Total Off-Peak Discount</Text>
                </View>
                <Text style={[styles.summaryValue, { color: '#FF9500' }]}>- LKR {summary ? Number(summary.totalCommissions).toFixed(2) : '0.00'}</Text>
              </View>

              {/* Delay Fines */}
              <View style={styles.summaryRow}>
                <View style={styles.summaryLabelGroup}>
                  <View style={[styles.dot, { backgroundColor: '#FF3B30' }]} />
                  <Text style={styles.summaryLabel}>Total Delay Fines</Text>
                </View>
                <Text style={[styles.summaryValue, { color: '#FF3B30' }]}>+ LKR {summary ? Number(summary.totalFines || 0).toFixed(2) : '0.00'}</Text>
              </View>

              <View style={styles.divider} />

              {/* Net Fee Payable */}
              <View style={[styles.summaryRow, { marginTop: 8 }]}>
                <Text style={styles.netRevenueLabel}>Net Payable to Authority</Text>
                <Text style={styles.netRevenueValue}>LKR {summary ? Number(summary.dailyNetRevenue).toFixed(2) : '0.00'}</Text>
              </View>
            </View>
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Ionicons name="calculator-outline" size={64} color="#C7C7CC" />
              <Text style={styles.emptyText}>No completed runs recorded for this date.</Text>
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
    backgroundColor: '#F8F9FB',
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
    backgroundColor: '#F2F2F7',
  },
  refreshButton: {
    padding: 8,
    borderRadius: 12,
    backgroundColor: Colors.primary + '10',
  },
  headerTitleContainer: {
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#000',
  },
  headerSubtitle: {
    fontSize: 12,
    color: '#8E8E93',
    marginTop: 2,
  },
  datePickerContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#FFF',
    borderBottomWidth: 1,
    borderBottomColor: '#EBEBEB',
  },
  arrowButton: {
    padding: 8,
    borderRadius: 8,
    backgroundColor: '#F2F2F7',
  },
  dateLabel: {
    fontSize: 15,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  summaryContainer: {
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 16,
    marginVertical: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  summaryLabelGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 8,
  },
  summaryLabel: {
    fontSize: 14,
    color: '#8E8E93',
    fontWeight: '500',
  },
  summaryValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  divider: {
    height: 1,
    backgroundColor: '#F2F2F7',
    marginVertical: 12,
  },
  netRevenueLabel: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  netRevenueValue: {
    fontSize: 18,
    fontWeight: '800',
    color: Colors.primary,
  },
  listContent: {
    paddingHorizontal: 16,
    paddingBottom: 30,
  },
  runCard: {
    backgroundColor: '#FFF',
    borderRadius: 12,
    padding: 14,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.03,
    shadowRadius: 4,
    elevation: 1,
  },
  runHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  runTimeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  runTimeText: {
    fontSize: 13,
    fontWeight: '600',
    color: '#8E8E93',
    marginLeft: 6,
  },
  offPeakBadge: {
    backgroundColor: '#FF950015',
    paddingHorizontal: 6,
    paddingVertical: 3,
    borderRadius: 4,
  },
  offPeakText: {
    color: '#FF9500',
    fontSize: 9,
    fontWeight: '700',
  },
  runRoute: {
    fontSize: 15,
    fontWeight: '700',
    color: '#1C1C1E',
    marginBottom: 10,
  },
  runBreakdown: {
    backgroundColor: '#F8F9FB',
    borderRadius: 8,
    padding: 8,
  },
  breakdownRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginVertical: 2,
  },
  breakdownLabel: {
    fontSize: 12,
    color: '#8E8E93',
  },
  breakdownValue: {
    fontSize: 12,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  breakdownLabelCommission: {
    fontSize: 12,
    color: '#FF9500',
  },
  breakdownValueCommission: {
    fontSize: 12,
    fontWeight: '600',
    color: '#FF9500',
  },
  breakdownLabelFine: {
    fontSize: 12,
    color: '#FF3B30',
  },
  breakdownValueFine: {
    fontSize: 12,
    fontWeight: '600',
    color: '#FF3B30',
  },
  loaderContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 100,
  },
  loadingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#8E8E93',
  },
  emptyContainer: {
    marginTop: 60,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    marginTop: 16,
    fontSize: 15,
    color: '#8E8E93',
    textAlign: 'center',
    paddingHorizontal: 40,
  }
});
