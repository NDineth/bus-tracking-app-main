import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import Colors from '../constants/Colors';
import { getRouteSchedule } from '../services/busService';

const BusCard = ({ bus, isDriving, onToggleDriving, isLoading }) => {
  const router = useRouter();
  const [nextSchedule, setNextSchedule] = useState(null);
  const [canStartDriving, setCanStartDriving] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const getColomboNow = () => {
    const colomboOffset = 5.5 * 60 * 60 * 1000;
    return new Date(Date.now() + colomboOffset);
  };

  useEffect(() => {
    const fetchNextSchedule = async () => {
      if (!bus) return;
      try {
        const schedule = await getRouteSchedule(bus.route, bus.busNumber);
        
        let mySchedule = null;
        if (isDriving) {
          // If already driving, find the turn that is currently ACTIVE or IN_PROGRESS
          mySchedule = schedule.find(s => s.isYourTurn && (s.rawStatus === 'ACTIVE' || s.rawStatus === 'IN_PROGRESS'));
        }
        
        // If not driving or no active turn found, find the next valid upcoming turn
        if (!mySchedule) {
          const now = getColomboNow();
          const getTurnDateTime = (rawTimeStr) => {
            if (!rawTimeStr) return getColomboNow();
            const [hours, minutes] = rawTimeStr.split(':').map(Number);
            const date = getColomboNow();
            date.setUTCHours(hours, minutes, 0, 0);
            return date;
          };

          const validTurns = schedule.filter(s => {
            if (!s.isYourTurn || s.rawStatus === 'COMPLETED' || s.rawStatus === 'CANCELLED') {
              return false;
            }
            const turnTime = getTurnDateTime(s.rawTime);
            const diffMinutes = (now - turnTime) / 60000;
            // Filter out turns that are more than 30 minutes late (expired)
            return diffMinutes <= 30;
          });

          if (validTurns.length > 0) {
            // Pick the first upcoming turn (since schedule is already sorted by departureTime)
            mySchedule = validTurns[0];
          }
        }

        setNextSchedule(mySchedule);
      } catch (e) {
        console.log('Error fetching schedule', e);
      }
    };
    fetchNextSchedule();
  }, [bus, isDriving, refreshKey]);

  useEffect(() => {
    if (isDriving) {
      setCanStartDriving(true);
      return;
    }

    const checkTime = () => {
      if (!nextSchedule) {
        setCanStartDriving(false);
        return;
      }
      
      const now = getColomboNow();
      const [hours, minutes] = nextSchedule.rawTime.split(':').map(Number);
      const scheduleTime = getColomboNow();
      scheduleTime.setUTCHours(hours, minutes, 0, 0);

      const diffMinutes = (now - scheduleTime) / 60000;
      if (diffMinutes > 30) {
        // If the turn has expired in real-time, trigger a refresh to find the next valid one
        setRefreshKey(prev => prev + 1);
        return;
      }

      // allow starting 15 minutes early? user said: "it should not able to press start driving utill time arrived."
      // So strictly >= scheduleTime. Maybe allow a 5 min grace period?
      // I'll stick to strictly time arrived.
      if (now >= scheduleTime) {
        setCanStartDriving(true);
      } else {
        setCanStartDriving(false);
      }
    };

    checkTime();
    const interval = setInterval(checkTime, 30000); // Check every 30 seconds
    return () => clearInterval(interval);
  }, [nextSchedule, isDriving]);

  if (!bus) return (
    <View style={styles.emptyContainer}>
      <Text style={styles.emptyText}>No bus assigned to you yet.</Text>
    </View>
  );

  let displayRoute = bus.route;
  if (nextSchedule) {
    displayRoute = `${nextSchedule.route} (Turn ${nextSchedule.turn} @ ${nextSchedule.time})`;
  } else if (displayRoute === 'No Active Route' || !displayRoute) {
    displayRoute = 'No Upcoming Runs Today';
  }

  return (
    <View style={styles.container}>
      <LinearGradient
        colors={isDriving ? ['#007AFF', '#0055CC'] : ['#5856D6', '#403CAD']}
        style={styles.cardHeader}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
      >
        <View>
          <Text style={styles.busNumber}>{bus.busNumber || bus.number || 'N/A'}</Text>
          <Text style={styles.routeText}>{displayRoute}</Text>
        </View>
        <Ionicons name="bus-outline" size={40} color="rgba(255, 255, 255, 0.3)" style={styles.bgIcon} />
      </LinearGradient>

      <View style={styles.cardBody}>
        <View style={styles.row}>
          <View style={styles.infoBox}>
            <Text style={styles.infoLabel}>Status</Text>
            <Text style={[styles.infoValue, { color: isDriving ? '#34C759' : '#8E8E93' }]}>
              {isDriving ? 'ON THE ROAD' : 'IDLE'}
            </Text>
          </View>
          <View style={styles.infoBox}>
            <Text style={styles.infoLabel}>Capacity</Text>
            <Text style={styles.infoValue}>{bus.capacity || 'N/A'}</Text>
          </View>
        </View>

        <View style={styles.buttonContainer}>
          <TouchableOpacity 
            style={[
              styles.button, 
              isDriving ? styles.stopButton : styles.startButton,
              (!canStartDriving && !isDriving) && styles.disabledButton
            ]}
            onPress={() => {
              if (!isDriving && !canStartDriving) {
                if (!nextSchedule) {
                  Alert.alert('Cannot Start', 'There are no upcoming schedules for today.');
                } else {
                  Alert.alert('Cannot Start', `You can only start driving at ${nextSchedule.time}.`);
                }
                return;
              }
              onToggleDriving(nextSchedule?.id);
            }}
            disabled={isLoading || (!isDriving && !canStartDriving && !nextSchedule)} // Only fully disable if no schedule at all, or let onPress handle the alert
            activeOpacity={0.8}
          >
            <Ionicons name={isDriving ? "stop-circle-outline" : "play-circle-outline"} size={24} color="#FFF" />
            <Text style={styles.buttonText}>
              {isLoading ? 'Processing...' : (isDriving ? 'Stop Driving' : 'Start Driving')}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.scheduleButton}
            onPress={() => router.push({
              pathname: '/bus-schedule',
              params: { 
                busId: bus.id || bus.busId, 
                busNumber: bus.busNumber,
                route: bus.route
              }
            })}
            activeOpacity={0.8}
          >
            <Ionicons name="calendar-outline" size={20} color={Colors.primary} />
            <Text style={styles.scheduleButtonText}>View Running Schedule</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.dailyNetButton}
            onPress={() => router.push({
              pathname: '/daily-net',
              params: { 
                busId: bus.id || bus.busId, 
                busNumber: bus.busNumber
              }
            })}
            activeOpacity={0.8}
          >
            <Ionicons name="calculator-outline" size={20} color={Colors.primary} />
            <Text style={styles.dailyNetButtonText}>View Daily Net Report</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.agendaButton}
            onPress={() => router.push({
              pathname: '/bus-agenda',
              params: { busNumber: bus.busNumber }
            })}
            activeOpacity={0.8}
          >
            <Ionicons name="list-outline" size={20} color={Colors.primary} />
            <Text style={styles.agendaButtonText}>Weekly Personal Agenda</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#FFF',
    borderRadius: 20,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5,
    marginBottom: 20,
  },
  cardHeader: {
    padding: 20,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  busNumber: {
    fontSize: 24,
    fontWeight: '800',
    color: '#FFF',
  },
  routeText: {
    fontSize: 16,
    color: 'rgba(255, 255, 255, 0.8)',
    marginTop: 4,
  },
  bgIcon: {
    position: 'absolute',
    right: -10,
    bottom: -10,
  },
  cardBody: {
    padding: 20,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  infoBox: {
    flex: 1,
  },
  infoLabel: {
    fontSize: 12,
    color: '#8E8E93',
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 4,
  },
  infoValue: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  buttonContainer: {
    gap: 12,
  },
  button: {
    flexDirection: 'row',
    height: 56,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    width: '100%',
  },
  scheduleButton: {
    flexDirection: 'row',
    height: 50,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.primary + '10',
    borderWidth: 1,
    borderColor: Colors.primary + '20',
    width: '100%',
  },
  startButton: {
    backgroundColor: '#007AFF',
  },
  stopButton: {
    backgroundColor: '#FF3B30',
  },
  disabledButton: {
    backgroundColor: '#A2A2A2', // Gray out when disabled
  },
  buttonText: {
    color: '#FFF',
    fontSize: 18,
    fontWeight: '700',
    marginLeft: 10,
  },
  scheduleButtonText: {
    color: Colors.primary,
    fontSize: 15,
    fontWeight: '600',
    marginLeft: 8,
  },
  agendaButton: {
    flexDirection: 'row',
    height: 50,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFF',
    borderWidth: 1,
    borderColor: Colors.primary + '30',
    width: '100%',
  },
  agendaButtonText: {
    color: Colors.primary,
    fontSize: 15,
    fontWeight: '600',
    marginLeft: 8,
  },
  dailyNetButton: {
    flexDirection: 'row',
    height: 50,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.primary + '10',
    borderWidth: 1,
    borderColor: Colors.primary + '20',
    width: '100%',
  },
  dailyNetButtonText: {
    color: Colors.primary,
    fontSize: 15,
    fontWeight: '600',
    marginLeft: 8,
  },
  emptyContainer: {
    padding: 30,
    backgroundColor: '#F2F2F7',
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#EBEBEB',
    borderStyle: 'dashed',
    marginBottom: 20,
  },
  emptyText: {
    color: '#8E8E93',
    fontSize: 16,
  }
});

export default BusCard;
