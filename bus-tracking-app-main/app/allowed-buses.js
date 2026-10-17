import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useBus } from '../context/BusContext';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import BusDetailsModal from '../components/BusDetailsModal';

export default function AllowedBusesScreen() {
  const { allBuses, fetchAllBuses, isLoading } = useBus();
  const [selectedBus, setSelectedBus] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);
  const router = useRouter();

  useEffect(() => {
    fetchAllBuses();
  }, [fetchAllBuses]);

  const handlePressBus = (bus) => {
    setSelectedBus(bus);
    setModalVisible(true);
  };

  const renderItem = ({ item }) => {
    return (
      <TouchableOpacity 
        style={styles.busCard} 
        onPress={() => handlePressBus(item)}
        activeOpacity={0.7}
      >
        <View style={styles.cardHeader}>
          <View style={styles.iconContainer}>
            <Ionicons name="bus" size={24} color="#176B60" />
          </View>
          <View style={styles.headerText}>
            <Text style={styles.busNumber}>{item.busNumber}</Text>
            <View style={[styles.statusBadge, { backgroundColor: item.status === 'IDLE' ? '#176B6020' : '#9A650A20' }]}>
              <View style={[styles.statusDot, { backgroundColor: item.status === 'IDLE' ? '#176B60' : '#9A650A' }]} />
              <Text style={[styles.statusText, { color: item.status === 'IDLE' ? '#176B60' : '#9A650A' }]}>
                {item.status}
              </Text>
            </View>
          </View>
        </View>

        <View style={styles.cardBody}>
          <View style={styles.infoRow}>
            <Ionicons name="map-outline" size={18} color="#617381" />
            <Text style={styles.infoText} numberOfLines={1}>{item.route}</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="person-outline" size={18} color="#617381" />
            <Text style={styles.infoText}>Driver: {item.assignedDriver || 'None'}</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="time-outline" size={18} color="#617381" />
            <Text style={styles.infoText}>Next Turn: {item.nextTurn || 'N/A'}</Text>
          </View>
        </View>
        
        <View style={styles.cardFooter}>
          <Text style={styles.viewDetails}>View Details</Text>
          <Ionicons name="chevron-forward" size={16} color="#176B60" />
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#000" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Allowed Buses</Text>
        <View style={{ width: 40 }} />
      </View>

      {isLoading && allBuses.length === 0 ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color="#176B60" />
        </View>
      ) : (
        <FlatList
          data={allBuses}
          renderItem={renderItem}
          keyExtractor={(item) => (item.id || item.busId).toString()}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <View style={styles.centerContainer}>
              <Text style={styles.emptyText}>No allowed buses found.</Text>
            </View>
          }
        />
      )}

      <BusDetailsModal 
        visible={modalVisible} 
        bus={selectedBus} 
        onClose={() => setModalVisible(false)} 
      />
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
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#000',
  },
  listContent: {
    width: '100%',
    maxWidth: 1000,
    alignSelf: 'center',
    padding: 16,
  },
  busCard: {
    backgroundColor: '#FFF',
    borderRadius: 20,
    padding: 20,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 3,
  },
  cardHeader: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: '#176B6010',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  headerText: {
    flex: 1,
    justifyContent: 'center',
  },
  busNumber: {
    fontSize: 18,
    fontWeight: '700',
    color: '#172D3B',
    marginBottom: 4,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
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
  cardBody: {
    borderTopWidth: 1,
    borderTopColor: '#F0F4F5',
    paddingTop: 16,
    marginBottom: 16,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  infoText: {
    fontSize: 14,
    color: '#3A3A3C',
    marginLeft: 12,
    flex: 1,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  viewDetails: {
    fontSize: 14,
    fontWeight: '600',
    color: '#176B60',
    marginRight: 4,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  emptyText: {
    fontSize: 16,
    color: '#617381',
  }
});
