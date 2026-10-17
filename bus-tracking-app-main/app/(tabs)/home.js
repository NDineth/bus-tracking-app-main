import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, RefreshControl, ActivityIndicator } from 'react-native';
import { useAuth } from '../../context/AuthContext';
import Colors from '../../constants/Colors';
import { Ionicons } from '@expo/vector-icons';
import { useBus } from '../../context/BusContext';
import BusCard from '../../components/BusCard';
import { useRouter } from 'expo-router';
import { getAllBuses, getFinanceSummary, getFleetSummary } from '../../services/busService';
import { showError } from '../../utils/alertService';

export default function HomeScreen() {
  const { user, logout } = useAuth();
  const { currentBus, isDriving, toggleDriving, isLoading: isDriverLoading } = useBus();
  const router = useRouter();

  // Owner specific states
  const [buses, setBuses] = useState([]);
  const [fleetSummary, setFleetSummary] = useState(null);
  const [financeSummary, setFinanceSummary] = useState([]);
  const [isOwnerLoading, setIsOwnerLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTab, setActiveTab] = useState('buses'); // 'buses' or 'reports'

  const getFriendlyRole = (role) => {
    const roles = {
      'BUS_DRIVER': 'Bus Driver',
      'BUS_OWNER': 'Bus Owner',
      'ADMIN': 'Administrator',
      'PASSENGER': 'Passenger',
    };
    return roles[role] || role;
  };

  const fetchOwnerData = useCallback(async () => {
    if (user?.role !== 'BUS_OWNER') return;
    try {
      // Fetch buses
      const allBuses = await getAllBuses();
      const filtered = allBuses.filter(b => b.owner && b.owner.id === user.id);
      setBuses(filtered);

      // Fetch fleet summary
      try {
        const summary = await getFleetSummary(user.id);
        setFleetSummary(summary);
      } catch (e) {
        console.log('Failed to fetch fleet summary:', e);
      }

      // Fetch financial summary for the last 30 days
      const toDate = new Date();
      const fromDate = new Date();
      fromDate.setDate(toDate.getDate() - 30);
      const toStr = toDate.toISOString().split('T')[0];
      const fromStr = fromDate.toISOString().split('T')[0];

      const financeData = await getFinanceSummary(fromStr, toStr);
      setFinanceSummary(financeData || []);
    } catch (error) {
      showError('Dashboard', 'Failed to fetch owner fleet data');
    }
  }, [user]);

  const onRefresh = async () => {
    setRefreshing(true);
    if (user?.role === 'BUS_OWNER') {
      await fetchOwnerData();
    }
    setRefreshing(false);
  };

  useEffect(() => {
    if (user?.role === 'BUS_OWNER') {
      setIsOwnerLoading(true);
      fetchOwnerData().finally(() => setIsOwnerLoading(false));
    }
  }, [user, fetchOwnerData]);

  const stats = (() => {
    const totalBuses = fleetSummary?.totalFleetCount ?? buses.length;
    const unavailableBuses = fleetSummary?.unavailableBusCount ?? buses.filter(b => b.status === 'UNAVAILABLE').length;
    const activeBuses = fleetSummary?.activeFleetCount ?? Math.max(0, totalBuses - unavailableBuses);

    let totalPermitFees = 0;
    let totalCommissions = 0;
    let totalPenalties = 0;

    if (financeSummary && Array.isArray(financeSummary)) {
      financeSummary.forEach(item => {
        totalPermitFees += Number(item.permitFeeTotal || 0);
        totalCommissions += Number(item.commissionTotal || 0);
        totalPenalties += Number(item.penaltyTotal || 0);
      });
    }

    const netPayable = totalPermitFees - totalCommissions + totalPenalties;

    return {
      totalBuses,
      activeBuses,
      unavailableBuses,
      netPayable
    };
  })();

  const renderReportsSection = () => {
    if (financeSummary.length === 0) {
      return (
        <View style={styles.emptyContainer}>
          <Ionicons name="calculator-outline" size={64} color="#C7C7CC" />
          <Text style={styles.emptyText}>No financial records found for the last 30 days.</Text>
        </View>
      );
    }

    return (
      <View style={{ gap: 16 }}>
        <Text style={styles.sectionTitle}>Fleet Financial Summaries (30 Days)</Text>
        {financeSummary.map(item => {
          const permitFees = Number(item.permitFeeTotal || 0);
          const commissions = Number(item.commissionTotal || 0);
          const penalties = Number(item.penaltyTotal || 0);
          const netPayable = permitFees - commissions + penalties;

          return (
            <View key={item.busId} style={styles.reportRowCard}>
              <View style={styles.reportCardHeader}>
                <Text style={styles.reportBusNum}>{item.busNumber}</Text>
                <Text style={[styles.reportGrandTotal, { color: Colors.primary }]}>
                  LKR {netPayable.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </Text>
              </View>

              <View style={styles.divider} />

              <View style={styles.breakdownGrid}>
                <View style={styles.breakdownCol}>
                  <Text style={styles.breakdownLabel}>Permit Fees</Text>
                  <Text style={styles.breakdownVal}>LKR {permitFees.toLocaleString('en-US')}</Text>
                </View>
                <View style={styles.breakdownCol}>
                  <Text style={styles.breakdownLabel}>Commissions</Text>
                  <Text style={[styles.breakdownVal, { color: '#FF9500' }]}>- LKR {commissions.toLocaleString('en-US')}</Text>
                </View>
                <View style={styles.breakdownCol}>
                  <Text style={styles.breakdownLabel}>Fines/Penalties</Text>
                  <Text style={[styles.breakdownVal, { color: '#FF3B30' }]}>+ LKR {penalties.toLocaleString('en-US')}</Text>
                </View>
              </View>

              <TouchableOpacity
                style={styles.viewDetailedReportButton}
                onPress={() => router.push({
                  pathname: '/daily-net',
                  params: {
                    busId: item.busId,
                    busNumber: item.busNumber
                  }
                })}
              >
                <Text style={styles.viewDetailedReportText}>View Daily Breakdown</Text>
                <Ionicons name="arrow-forward" size={16} color={Colors.primary} />
              </TouchableOpacity>
            </View>
          );
        })}
      </View>
    );
  };

  const renderOwnerDashboard = () => {
    if (isOwnerLoading && buses.length === 0) {
      return (
        <View style={styles.loaderContainer}>
          <ActivityIndicator size="large" color={Colors.primary} />
          <Text style={styles.loaderText}>Loading dashboard...</Text>
        </View>
      );
    }

    return (
      <ScrollView
        contentContainerStyle={styles.scrollContainer}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={[Colors.primary]} />
        }
      >
        {/* Statistics Ribbon */}
        <View style={styles.statsContainer}>
          <View style={styles.statCard}>
            <Text style={styles.statNumber}>{stats.totalBuses}</Text>
            <Text style={styles.statLabel}>Total Fleet</Text>
          </View>
          <View style={styles.statCard}>
            <Text style={[styles.statNumber, { color: '#34C759' }]}>{stats.activeBuses}</Text>
            <Text style={styles.statLabel}>Active Buses</Text>
          </View>
          <View style={styles.statCard}>
            <Text style={[styles.statNumber, { color: stats.unavailableBuses > 0 ? '#FF3B30' : '#8E8E93' }]}>
              {stats.unavailableBuses}
            </Text>
            <Text style={styles.statLabel}>Unavailable</Text>
          </View>
          <View style={[styles.statCard, { flex: 1.3 }]}>
            <Text style={[styles.statNumber, { color: Colors.primary, fontSize: 13 }]} numberOfLines={1}>
              LKR {stats.netPayable.toLocaleString('en-US', { maximumFractionDigits: 0 })}
            </Text>
            <Text style={styles.statLabel}>30D Net Authority</Text>
          </View>
        </View>

        {/* Quick Actions / Approval Requests */}
        <Text style={[styles.sectionTitle, { marginTop: 10, marginBottom: 12 }]}>Approval Requests</Text>
        <View style={styles.quickActionsRow}>
          <TouchableOpacity 
            style={styles.quickActionCard}
            onPress={() => router.push('/new-request')}
          >
            <View style={[styles.quickActionIconContainer, { backgroundColor: Colors.primary + '15' }]}>
              <Ionicons name="add-circle" size={28} color={Colors.primary} />
            </View>
            <Text style={styles.quickActionTitle}>New Request</Text>
            <Text style={styles.quickActionDesc}>Submit a new emergency or schedule change request</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.quickActionCard}
            onPress={() => router.push('/my-requests')}
          >
            <View style={[styles.quickActionIconContainer, { backgroundColor: '#FF950015' }]}>
              <Ionicons name="document-text" size={28} color="#FF9500" />
            </View>
            <Text style={styles.quickActionTitle}>My Requests</Text>
            <Text style={styles.quickActionDesc}>Track status of submitted approval requests</Text>
          </TouchableOpacity>
        </View>

        {/* Tab Buttons (Owned Buses List vs Daily Net Reports) */}
        <View style={styles.tabContainer}>
          <TouchableOpacity
            style={[styles.tabButton, activeTab === 'buses' && styles.activeTabButton]}
            onPress={() => setActiveTab('buses')}
          >
            <Ionicons name="bus-outline" size={18} color={activeTab === 'buses' ? '#FFF' : Colors.primary} />
            <Text style={[styles.tabButtonText, activeTab === 'buses' && styles.activeTabButtonText]}>
              Owned Buses
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.tabButton, activeTab === 'reports' && styles.activeTabButton]}
            onPress={() => setActiveTab('reports')}
          >
            <Ionicons name="calculator-outline" size={18} color={activeTab === 'reports' ? '#FFF' : Colors.primary} />
            <Text style={[styles.tabButtonText, activeTab === 'reports' && styles.activeTabButtonText]}>
              Daily Net Reports
            </Text>
          </TouchableOpacity>
        </View>

        {/* Dynamic Tab Content */}
        {activeTab === 'buses' ? (
          <View>
            {/* Fleet Section */}
            <View style={styles.fleetHeader}>
              <Text style={styles.sectionTitle}>Your Fleet</Text>
              <Text style={styles.fleetCount}>{buses.length} Bus{buses.length !== 1 ? 'es' : ''}</Text>
            </View>

            {buses.length === 0 ? (
              <View style={styles.emptyContainer}>
                <Ionicons name="bus-outline" size={64} color="#C7C7CC" />
                <Text style={styles.emptyText}>You don't have any buses registered in your fleet.</Text>
              </View>
            ) : (
              buses.map(bus => {
                const isUnavailable = bus.status === 'UNAVAILABLE';
                const isPending = bus.status === 'PENDING_APPROVAL';
                const isActive = bus.status === 'ACTIVE' || (!isUnavailable && !isPending && bus.status !== 'IDLE' && bus.status !== 'OFFLINE');

                return (
                  <View key={bus.id} style={styles.busRowCard}>
                    <View style={styles.cardHeaderRow}>
                      <View style={{ flex: 1, paddingRight: 8 }}>
                        <Text style={styles.busNumText}>{bus.busNumber}</Text>
                        <Text style={styles.busRouteText}>{bus.route}</Text>
                      </View>
                      <View style={[
                        styles.statusBadge, 
                        isUnavailable ? { backgroundColor: '#FF3B3015' } :
                        isPending ? { backgroundColor: '#FF950015' } :
                        isActive ? styles.activeBadge : styles.idleBadge
                      ]}>
                        <View style={[
                          styles.statusDot, 
                          isUnavailable ? { backgroundColor: '#FF3B30' } :
                          isPending ? { backgroundColor: '#FF9500' } :
                          isActive ? styles.activeDot : styles.idleDot
                        ]} />
                        <Text style={[
                          styles.statusBadgeText, 
                          isUnavailable ? { color: '#FF3B30' } :
                          isPending ? { color: '#FF9500' } :
                          isActive ? styles.activeBadgeText : styles.idleBadgeText
                        ]}>
                          {isUnavailable ? 'UNAVAILABLE' : isPending ? 'PENDING' : isActive ? 'ACTIVE' : 'IDLE'}
                        </Text>
                      </View>
                    </View>

                    {isUnavailable && bus.unavailabilityReason && (
                      <View style={{ marginTop: 8, padding: 8, backgroundColor: '#FF3B3010', borderRadius: 8, flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                        <Ionicons name="alert-circle-outline" size={16} color="#FF3B30" />
                        <Text style={{ fontSize: 12, color: '#FF3B30', fontWeight: '600', flex: 1 }} numberOfLines={1}>
                          {bus.unavailabilityReason}
                        </Text>
                      </View>
                    )}

                    <View style={styles.divider} />

                    <View style={styles.busInfoGrid}>
                      <View style={styles.infoRow}>
                        <Ionicons name="person-outline" size={16} color="#8E8E93" />
                        <Text style={styles.infoText}>
                          Driver: <Text style={styles.infoValue}>{bus.assignedDriver}</Text>
                        </Text>
                      </View>
                      <View style={styles.infoRow}>
                        <Ionicons name="people-outline" size={16} color="#8E8E93" />
                        <Text style={styles.infoText}>
                          Capacity: <Text style={styles.infoValue}>{bus.capacity} seats</Text>
                        </Text>
                      </View>
                      {bus.nextTurn && bus.nextTurn !== 'N/A' && (
                        <View style={styles.infoRow}>
                          <Ionicons name="time-outline" size={16} color="#8E8E93" />
                          <Text style={styles.infoText}>
                            Next Turn: <Text style={styles.infoValue}>{bus.nextTurn}</Text>
                          </Text>
                        </View>
                      )}
                    </View>

                    <View style={styles.actionButtonsRow}>
                      <TouchableOpacity
                        style={[styles.actionButton, styles.scheduleButton]}
                        onPress={() => router.push({
                          pathname: '/bus-schedule',
                          params: {
                            busId: bus.id,
                            busNumber: bus.busNumber,
                            route: bus.route
                          }
                        })}
                      >
                        <Ionicons name="calendar-outline" size={15} color={Colors.primary} />
                        <Text style={styles.actionButtonText}>Schedule</Text>
                      </TouchableOpacity>

                      <TouchableOpacity
                        style={[styles.actionButton, styles.agendaButton]}
                        onPress={() => router.push({
                          pathname: '/bus-agenda',
                          params: { busNumber: bus.busNumber }
                        })}
                      >
                        <Ionicons name="list-outline" size={15} color={Colors.primary} />
                        <Text style={styles.actionButtonText}>Agenda</Text>
                      </TouchableOpacity>

                      <TouchableOpacity
                        style={[styles.actionButton, styles.reportButton]}
                        onPress={() => router.push({
                          pathname: '/daily-net',
                          params: {
                            busId: bus.id,
                            busNumber: bus.busNumber
                          }
                        })}
                      >
                        <Ionicons name="calculator-outline" size={15} color="#34C759" />
                        <Text style={[styles.actionButtonText, { color: '#34C759' }]}>Net Report</Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                );
              })
            )}
          </View>
        ) : (
          renderReportsSection()
        )}
      </ScrollView>
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <View>
          <Text style={styles.greeting}>Hello, {user?.name || (user?.firstName ? `${user.firstName} ${user.lastName}` : 'User')}</Text>
          <Text style={styles.role}>{getFriendlyRole(user?.role)}</Text>
        </View>
        <TouchableOpacity style={styles.logoutButton} onPress={logout}>
          <Text style={styles.logoutText}>Logout</Text>
        </TouchableOpacity>
      </View>

      {user?.role === 'BUS_DRIVER' && (
        <ScrollView contentContainerStyle={styles.driverScrollContainer} showsVerticalScrollIndicator={false}>
          <View style={styles.busSection}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Your Assigned Bus</Text>
              <TouchableOpacity
                onPress={() => router.push('/allowed-buses')}
                style={styles.browseButton}
              >
                <Ionicons name="grid-outline" size={18} color={Colors.primary} />
                <Text style={styles.changeLink}>Allowed Buses</Text>
              </TouchableOpacity>
            </View>
            <BusCard
              bus={currentBus}
              isDriving={isDriving}
              onToggleDriving={toggleDriving}
              isLoading={isDriverLoading}
            />
          </View>

          {/* Quick Actions / Approval Requests */}
          <Text style={[styles.sectionTitle, { marginTop: 24, marginBottom: 12 }]}>Approval Requests</Text>
          <View style={styles.quickActionsRow}>
            <TouchableOpacity 
              style={styles.quickActionCard}
              onPress={() => router.push('/new-request')}
            >
              <View style={[styles.quickActionIconContainer, { backgroundColor: Colors.primary + '15' }]}>
                <Ionicons name="add-circle" size={28} color={Colors.primary} />
              </View>
              <Text style={styles.quickActionTitle}>New Request</Text>
              <Text style={styles.quickActionDesc}>Submit a new emergency or schedule change request</Text>
            </TouchableOpacity>

            <TouchableOpacity 
              style={styles.quickActionCard}
              onPress={() => router.push('/my-requests')}
            >
              <View style={[styles.quickActionIconContainer, { backgroundColor: '#FF950015' }]}>
                <Ionicons name="document-text" size={28} color="#FF9500" />
              </View>
              <Text style={styles.quickActionTitle}>My Requests</Text>
              <Text style={styles.quickActionDesc}>Track status of submitted approval requests</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      )}

      {user?.role === 'BUS_OWNER' && renderOwnerDashboard()}
    </View>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
    paddingHorizontal: 20,
    paddingTop: 40,
  },
  scrollContainer: {
    paddingBottom: 40,
  },
  busSection: {
    marginBottom: 10,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  changeLink: {
    color: Colors.primary,
    fontWeight: '600',
    fontSize: 14,
    marginLeft: 6,
  },
  browseButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.primary + '10',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
    paddingVertical: 10,
  },
  greeting: {
    fontSize: 22,
    fontWeight: 'bold',
    color: Colors.text,
  },
  role: {
    fontSize: 15,
    color: Colors.primary,
    marginTop: 4,
    fontWeight: '600',
  },
  logoutButton: {
    paddingVertical: 8,
    paddingHorizontal: 15,
    backgroundColor: '#ffebee',
    borderRadius: 20,
  },
  logoutText: {
    color: '#d32f2f',
    fontWeight: 'bold',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.text,
  },
  statsContainer: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 25,
    justifyContent: 'space-between',
  },
  statCard: {
    flex: 1,
    backgroundColor: '#FFF',
    borderRadius: 12,
    padding: 12,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 5,
    elevation: 1,
  },
  statNumber: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 10,
    color: '#8E8E93',
    fontWeight: '500',
    textAlign: 'center',
  },
  fleetHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  fleetCount: {
    fontSize: 14,
    color: '#8E8E93',
    fontWeight: '600',
  },
  busRowCard: {
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
  },
  cardHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  busNumText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
  },
  busRouteText: {
    fontSize: 13,
    color: '#8E8E93',
    marginTop: 2,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  activeBadge: {
    backgroundColor: '#34C75915',
  },
  idleBadge: {
    backgroundColor: '#F2F2F7',
  },
  statusDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 5,
  },
  activeDot: {
    backgroundColor: '#34C759',
  },
  idleDot: {
    backgroundColor: '#8E8E93',
  },
  statusBadgeText: {
    fontSize: 10,
    fontWeight: '700',
  },
  activeBadgeText: {
    color: '#34C759',
  },
  idleBadgeText: {
    color: '#8E8E93',
  },
  divider: {
    height: 1,
    backgroundColor: '#F2F2F7',
    marginVertical: 12,
  },
  busInfoGrid: {
    gap: 8,
    marginBottom: 14,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  infoText: {
    fontSize: 13,
    color: '#8E8E93',
  },
  infoValue: {
    fontWeight: '600',
    color: '#1C1C1E',
  },
  actionButtonsRow: {
    flexDirection: 'row',
    gap: 8,
    justifyContent: 'space-between',
  },
  actionButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
  },
  scheduleButton: {
    backgroundColor: Colors.primary + '10',
    borderColor: Colors.primary + '20',
  },
  agendaButton: {
    backgroundColor: Colors.primary + '10',
    borderColor: Colors.primary + '20',
  },
  reportButton: {
    backgroundColor: '#34C75910',
    borderColor: '#34C75920',
  },
  actionButtonText: {
    fontSize: 12,
    fontWeight: '600',
    color: Colors.primary,
  },
  loaderContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 60,
  },
  loaderText: {
    marginTop: 10,
    fontSize: 15,
    color: '#8E8E93',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: 40,
    backgroundColor: '#FFF',
    borderRadius: 16,
    borderStyle: 'dashed',
    borderWidth: 1,
    borderColor: '#C7C7CC',
  },
  emptyText: {
    textAlign: 'center',
    color: '#8E8E93',
    fontSize: 14,
    marginTop: 10,
  },
  tabContainer: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 20,
  },
  tabButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
    borderRadius: 25,
    backgroundColor: '#FFF',
    borderWidth: 1,
    borderColor: Colors.primary + '30',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.03,
    shadowRadius: 3,
    elevation: 1,
  },
  activeTabButton: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  tabButtonText: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.primary,
  },
  activeTabButtonText: {
    color: '#FFF',
  },
  reportRowCard: {
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
  },
  reportCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  reportBusNum: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
  },
  reportGrandTotal: {
    fontSize: 16,
    fontWeight: '800',
  },
  breakdownGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
    marginVertical: 4,
  },
  breakdownCol: {
    flex: 1,
    alignItems: 'center',
  },
  breakdownLabel: {
    fontSize: 10,
    color: '#8E8E93',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  breakdownVal: {
    fontSize: 12,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  viewDetailedReportButton: {
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#F2F2F7',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
  },
  viewDetailedReportText: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.primary,
  },
  driverScrollContainer: {
    paddingBottom: 30,
  },
  quickActionsRow: {
    flexDirection: 'row',
    gap: 16,
    marginBottom: 20,
  },
  quickActionCard: {
    flex: 1,
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
    borderWidth: 1,
    borderColor: '#F2F2F7',
  },
  quickActionIconContainer: {
    width: 48,
    height: 48,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  quickActionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1C1C1E',
    marginBottom: 4,
  },
  quickActionDesc: {
    fontSize: 11,
    color: '#8E8E93',
    lineHeight: 14,
  },
});


