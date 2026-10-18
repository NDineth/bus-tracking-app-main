import { View, Text, StyleSheet, Modal, TouchableOpacity, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import Colors from '../constants/Colors';

const BusDetailsModal = ({ visible, bus, onClose }) => {
  const router = useRouter();
  if (!bus) return null;

  return (
    <Modal
      animationType="slide"
      transparent={true}
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <View style={styles.header}>
            <Text style={styles.headerTitle}>Bus Details</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <Ionicons name="close" size={24} color="#000" />
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.scrollView}>
            <View style={styles.section}>
              <Text style={styles.busNumber}>{bus.busNumber}</Text>
              <Text style={styles.routeText}>{bus.route}</Text>
            </View>

            <View style={styles.infoRow}>
              <View style={styles.infoItem}>
                <Text style={styles.infoLabel}>Assigned Driver</Text>
                <Text style={styles.infoValue}>{bus.assignedDriver || 'None'}</Text>
              </View>
              <View style={styles.infoItem}>
                <Text style={styles.infoLabel}>Capacity</Text>
                <Text style={styles.infoValue}>{bus.capacity} Seats</Text>
              </View>
            </View>

            <TouchableOpacity accessibilityRole="button" style={styles.fullScheduleButton} onPress={() => {
              onClose(); router.push({ pathname: '/bus-details', params: { busId: bus.id || bus.busId, busNumber: bus.busNumber } });
            }}><Ionicons name="analytics-outline" size={20} color={Colors.primary} /><Text style={styles.fullScheduleButtonText}>View full-turn driving graphs</Text></TouchableOpacity>
            <View style={styles.divider} />

            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Bus Turns Today</Text>
              {bus.turns && bus.turns.map((turn, index) => (
                <View key={index} style={styles.turnRow}>
                  <Ionicons name="time-outline" size={20} color="#176B60" />
                  <Text style={styles.turnText}>Turn {index + 1}: {turn}</Text>
                </View>
              ))}

              <TouchableOpacity 
                style={styles.fullScheduleButton}
                onPress={() => {
                  onClose();
                  router.push({
                    pathname: '/bus-schedule',
                    params: { 
                      busId: bus.id || bus.busId, 
                      busNumber: bus.busNumber,
                      route: bus.route
                    }
                  });
                }}
              >
                <Text style={styles.fullScheduleButtonText}>View Full Route Schedule</Text>
                <Ionicons name="chevron-forward" size={16} color={Colors.primary} />
              </TouchableOpacity>
            </View>
          </ScrollView>

          <TouchableOpacity style={styles.doneButton} onPress={onClose}>
            <Text style={styles.doneButtonText}>Done</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#FFF',
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    padding: 24,
    maxHeight: '80%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 10,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#172D3B',
  },
  closeButton: {
    padding: 4,
  },
  scrollView: {
    marginBottom: 20,
  },
  section: {
    marginBottom: 24,
  },
  busNumber: {
    fontSize: 28,
    fontWeight: '800',
    color: '#176B60',
  },
  routeText: {
    fontSize: 18,
    color: '#3A3A3C',
    marginTop: 4,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  infoItem: {
    flex: 1,
  },
  infoLabel: {
    fontSize: 12,
    color: '#617381',
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 4,
  },
  infoValue: {
    fontSize: 16,
    fontWeight: '600',
    color: '#172D3B',
  },
  divider: {
    height: 1,
    backgroundColor: '#EBEBEB',
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#172D3B',
    marginBottom: 16,
  },
  turnRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F0F4F5',
    padding: 12,
    borderRadius: 12,
    marginBottom: 8,
  },
  turnText: {
    fontSize: 15,
    color: '#172D3B',
    marginLeft: 12,
  },
  doneButton: {
    backgroundColor: '#176B60',
    height: 56,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  doneButtonText: {
    color: '#FFF',
    fontSize: 18,
    fontWeight: '700',
  },
  fullScheduleButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    marginTop: 8,
    backgroundColor: Colors.primary + '10',
    borderRadius: 12,
  },
  fullScheduleButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.primary,
    marginRight: 4,
  },
});

export default BusDetailsModal;
