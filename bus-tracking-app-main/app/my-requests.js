import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useAuth } from '../context/AuthContext';
import { getUserApprovalRequests } from '../services/approvalService';
import Colors from '../constants/Colors';

export default function MyRequestsScreen() {
  const { user } = useAuth();
  const router = useRouter();
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchRequests = useCallback(async () => {
    if (!user?.id) return;
    try {
      const data = await getUserApprovalRequests(user.id);
      
      // Sort requests by createdAt (newest first)
      const sorted = (data || []).sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
      );
      setRequests(sorted);
    } catch (error) {
      // Suppress error in local states but log
      console.error('Failed to load requests:', error);
    } finally {
      setIsLoading(false);
      setRefreshing(false);
    }
  }, [user?.id]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchRequests();
  };

  const getStatusBadgeStyle = (status) => {
    switch (status) {
      case 'APPROVED':
        return { bg: '#34C75915', text: '#34C759', dot: '#34C759' };
      case 'REJECTED':
        return { bg: '#FF3B3015', text: '#FF3B30', dot: '#FF3B30' };
      case 'PENDING':
      default:
        return { bg: '#FF950015', text: '#FF9500', dot: '#FF9500' };
    }
  };

  const getFriendlyEmergencyType = (type) => {
    switch (type) {
      case 'ACCIDENT': return 'Accident';
      case 'BREAKDOWN': return 'Breakdown';
      case 'OTHER': return 'Other Emergency';
      default: return type || 'Emergency';
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (e) {
      return dateStr;
    }
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return 'N/A';
    try {
      const parts = timeStr.split(':');
      let hours = parseInt(parts[0], 10);
      const minutes = parts[1];
      const ampm = hours >= 12 ? 'PM' : 'AM';
      hours = hours % 12;
      hours = hours ? hours : 12;
      return `${hours.toString().padStart(2, '0')}:${minutes} ${ampm}`;
    } catch (e) {
      return timeStr;
    }
  };

  const renderItem = ({ item }) => {
    const badge = getStatusBadgeStyle(item.status);
    const isEmergency = item.approvalType === 'EMERGENCY';

    return (
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <View style={styles.titleContainer}>
            <View style={[styles.typeIconContainer, { backgroundColor: isEmergency ? '#FF3B3010' : '#0066cc10' }]}>
              <Ionicons 
                name={isEmergency ? "warning-outline" : "calendar-outline"} 
                size={20} 
                color={isEmergency ? "#FF3B30" : Colors.primary} 
              />
            </View>
            <View>
              <Text style={styles.cardTitle}>
                {isEmergency ? 'Emergency Request' : 'Schedule Change'}
              </Text>
              <Text style={styles.busNumber}>Bus: {item.busNumber || 'Unknown'}</Text>
            </View>
          </View>
          
          <View style={[styles.statusBadge, { backgroundColor: badge.bg }]}>
            <View style={[styles.statusDot, { backgroundColor: badge.dot }]} />
            <Text style={[styles.statusText, { color: badge.text }]}>
              {item.status || 'PENDING'}
            </Text>
          </View>
        </View>

        <View style={styles.divider} />

        <View style={styles.cardBody}>
          {isEmergency ? (
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Reason:</Text>
              <Text style={styles.detailValue}>{getFriendlyEmergencyType(item.emergencyType)}</Text>
            </View>
          ) : (
            <>
              <View style={styles.detailRow}>
                <Text style={styles.detailLabel}>Dates:</Text>
                <Text style={styles.detailValue}>
                  {formatDate(item.startDate)} — {formatDate(item.endDate)}
                </Text>
              </View>
              <View style={styles.detailRow}>
                <Text style={styles.detailLabel}>Times:</Text>
                <Text style={styles.detailValue}>
                  {formatTime(item.startTime)} — {formatTime(item.endTime)}
                </Text>
              </View>
            </>
          )}

          {/* Admin Response/Comments */}
          {item.reason && item.reason.trim().length > 0 && (
            <View style={[styles.reasonContainer, { borderLeftColor: badge.dot }]}>
              <Text style={styles.reasonLabel}>Admin Response:</Text>
              <Text style={styles.reasonText}>{item.reason}</Text>
            </View>
          )}
        </View>

        <View style={styles.cardFooter}>
          <Text style={styles.footerTime}>
            Submitted on {new Date(item.createdAt).toLocaleDateString()} at {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </Text>
        </View>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#000" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>My Requests</Text>
        <TouchableOpacity onPress={onRefresh} style={styles.refreshButton}>
          <Ionicons name="refresh" size={22} color={Colors.primary} />
        </TouchableOpacity>
      </View>

      {isLoading && requests.length === 0 ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color={Colors.primary} />
          <Text style={styles.loadingText}>Fetching approval history...</Text>
        </View>
      ) : (
        <FlatList
          data={requests}
          renderItem={renderItem}
          keyExtractor={(item) => (item.id || item.approvalId || Math.random()).toString()}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={[Colors.primary]} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Ionicons name="documents-outline" size={64} color="#C7C7CC" />
              <Text style={styles.emptyText}>You haven't submitted any approval requests yet.</Text>
              <TouchableOpacity 
                style={styles.emptyButton}
                onPress={() => router.push('/new-request')}
              >
                <Text style={styles.emptyButtonText}>New Request</Text>
              </TouchableOpacity>
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
  },
  refreshButton: {
    padding: 8,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#000',
  },
  listContent: {
    padding: 16,
    paddingBottom: 30,
  },
  card: {
    backgroundColor: '#FFF',
    borderRadius: 20,
    padding: 18,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.04,
    shadowRadius: 10,
    elevation: 2,
    borderWidth: 1,
    borderColor: '#F2F2F7',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  titleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    paddingRight: 8,
  },
  typeIconContainer: {
    width: 44,
    height: 44,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  busNumber: {
    fontSize: 13,
    color: '#8E8E93',
    marginTop: 2,
    fontWeight: '500',
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 20,
  },
  statusDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 6,
  },
  statusText: {
    fontSize: 11,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
  divider: {
    height: 1,
    backgroundColor: '#F2F2F7',
    marginVertical: 14,
  },
  cardBody: {
    gap: 10,
  },
  detailRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  detailLabel: {
    fontSize: 14,
    color: '#8E8E93',
    width: 70,
    fontWeight: '500',
  },
  detailValue: {
    fontSize: 14,
    color: '#1C1C1E',
    fontWeight: '600',
    flex: 1,
  },
  reasonContainer: {
    marginTop: 8,
    paddingLeft: 12,
    borderLeftWidth: 3,
    backgroundColor: '#F8F9FB',
    paddingVertical: 10,
    paddingHorizontal: 10,
    borderRadius: 8,
  },
  reasonLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: '#3A3A3C',
    marginBottom: 4,
  },
  reasonText: {
    fontSize: 13,
    color: '#555',
    lineHeight: 18,
    fontWeight: '500',
  },
  cardFooter: {
    marginTop: 14,
    borderTopWidth: 1,
    borderTopColor: '#F2F2F7',
    paddingTop: 10,
  },
  footerTime: {
    fontSize: 11,
    color: '#8E8E93',
    fontWeight: '500',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
    marginTop: 100,
  },
  loadingText: {
    marginTop: 12,
    fontSize: 15,
    color: '#8E8E93',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: 40,
    marginTop: 100,
  },
  emptyText: {
    fontSize: 15,
    color: '#8E8E93',
    textAlign: 'center',
    marginTop: 16,
    marginBottom: 24,
    lineHeight: 20,
    paddingHorizontal: 20,
  },
  emptyButton: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 20,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.15,
    shadowRadius: 6,
    elevation: 2,
  },
  emptyButtonText: {
    color: '#FFF',
    fontSize: 15,
    fontWeight: '700',
  },
});
