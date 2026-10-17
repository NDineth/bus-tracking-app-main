import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, SectionList, TouchableOpacity, ActivityIndicator, SafeAreaView, StatusBar } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { getBusFutureAgenda } from '../services/busService';
import Colors from '../constants/Colors';
import { showError } from '../utils/alertService';


export default function BusAgendaScreen() {
  const { busNumber } = useLocalSearchParams();
  const [agenda, setAgenda] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const fetchAgenda = async () => {
      try {
        const data = await getBusFutureAgenda(busNumber);
        setAgenda(data);
      } catch (error) {
        showError('Agenda', 'Failed to fetch agenda');
      } finally {
        setIsLoading(false);
      }
    };

    fetchAgenda();
  }, [busNumber]);

  // Group agenda by date for SectionList
  const groupedAgenda = agenda.reduce((acc, item) => {
    const existingSection = acc.find(section => section.title === item.date);
    if (existingSection) {
      existingSection.data.push(item);
    } else {
      acc.push({ title: item.date, data: [item] });
    }
    return acc;
  }, []);

  const renderItem = ({ item }) => {
    const isPast = item.status === 'PAST';
    const isCurrent = item.status === 'CURRENT';

    return (
      <View style={[styles.turnCard, isPast && styles.pastCard]}>
        <View style={styles.timeInfo}>
          <Text style={[styles.timeText, isPast && styles.pastText]}>{item.time}</Text>
          <View style={styles.turnBadge}>
            <Text style={styles.turnBadgeText}>Turn {item.turn}</Text>
          </View>
        </View>

        <View style={styles.routeInfo}>
          <View style={styles.routeHeader}>
            <Ionicons name="map-outline" size={16} color={isPast ? '#8E8E93' : Colors.primary} />
            <Text style={[styles.routeText, isPast && styles.pastText]} numberOfLines={1}>
              {item.route}
            </Text>
          </View>
          
          {isCurrent && (
            <View style={styles.currentIndicator}>
              <View style={styles.dot} />
              <Text style={styles.currentLabel}>ACTIVE NOW</Text>
            </View>
          )}
        </View>

        <TouchableOpacity 
          style={styles.viewRouteButton}
          onPress={() => router.push({
            pathname: '/bus-schedule',
            params: { busNumber, route: item.route }
          })}
        >
          <Ionicons name="chevron-forward" size={20} color="#C7C7CC" />
        </TouchableOpacity>
      </View>
    );
  };

  const renderSectionHeader = ({ section: { title } }) => (
    <View style={styles.sectionHeader}>
      <Text style={styles.sectionTitle}>{title}</Text>
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#000" />
        </TouchableOpacity>
        <View style={styles.headerTitleContainer}>
          <Text style={styles.headerTitle}>Bus Agenda</Text>
          <Text style={styles.headerSubtitle}>{busNumber}</Text>
        </View>
        <View style={{ width: 44 }} />
      </View>

      {isLoading ? (
        <View style={styles.loaderContainer}>
          <ActivityIndicator size="large" color={Colors.primary} />
          <Text style={styles.loadingText}>Fetching your future turns...</Text>
        </View>
      ) : (
        <SectionList
          sections={groupedAgenda}
          keyExtractor={(item) => item.id.toString()}
          renderItem={renderItem}
          renderSectionHeader={renderSectionHeader}
          contentContainerStyle={styles.listContent}
          stickySectionHeadersEnabled={false}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Ionicons name="calendar-outline" size={64} color="#C7C7CC" />
              <Text style={styles.emptyText}>No future turns assigned to this bus.</Text>
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
    color: Colors.primary,
    fontWeight: '600',
    marginTop: 2,
  },
  listContent: {
    paddingHorizontal: 16,
    paddingBottom: 40,
  },
  sectionHeader: {
    marginTop: 24,
    marginBottom: 12,
    paddingHorizontal: 4,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: '#8E8E93',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  turnCard: {
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
  },
  pastCard: {
    opacity: 0.6,
    backgroundColor: '#F2F2F7',
  },
  timeInfo: {
    width: 85,
    borderRightWidth: 1,
    borderRightColor: '#F2F2F7',
    paddingRight: 12,
  },
  timeText: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  turnBadge: {
    backgroundColor: Colors.primary + '15',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
    alignSelf: 'flex-start',
    marginTop: 4,
  },
  turnBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: Colors.primary,
  },
  routeInfo: {
    flex: 1,
    paddingHorizontal: 16,
  },
  routeHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  routeText: {
    fontSize: 15,
    fontWeight: '600',
    color: '#1C1C1E',
    marginLeft: 8,
    flex: 1,
  },
  pastText: {
    color: '#8E8E93',
  },
  currentIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#34C759',
    marginRight: 6,
  },
  currentLabel: {
    fontSize: 10,
    fontWeight: '800',
    color: '#34C759',
  },
  viewRouteButton: {
    padding: 4,
  },
  loaderContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#8E8E93',
  },
  emptyContainer: {
    marginTop: 100,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    marginTop: 16,
    fontSize: 16,
    color: '#8E8E93',
    textAlign: 'center',
    paddingHorizontal: 40,
  }
});
