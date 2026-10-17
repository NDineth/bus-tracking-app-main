import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, ActivityIndicator, Modal, Alert, TextInput, Keyboard, TouchableWithoutFeedback } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useAuth } from '../context/AuthContext';
import { getAllBuses } from '../services/busService';
import { submitApprovalRequest } from '../services/approvalService';
import Colors from '../constants/Colors';

export default function NewRequestScreen() {
  const { user } = useAuth();
  const router = useRouter();

  // Form states
  const [buses, setBuses] = useState([]);
  const [selectedBus, setSelectedBus] = useState(null);
  const [approvalType, setApprovalType] = useState('EMERGENCY'); // 'EMERGENCY' or 'NORMAL'
  
  // Emergency specific states
  const [emergencyType, setEmergencyType] = useState(null); // 'ACCIDENT', 'BREAKDOWN', 'OTHER'

  // Normal (Schedule Change) specific states
  const [startDate, setStartDate] = useState(null); // Date object
  const [endDate, setEndDate] = useState(null); // Date object
  const [startTime, setStartTime] = useState(null); // Time string "HH:MM"
  const [endTime, setEndTime] = useState(null); // Time string "HH:MM"

  // Modal / Picker visibility states
  const [busModalVisible, setBusModalVisible] = useState(false);
  const [datePickerConfig, setDatePickerConfig] = useState({ visible: false, mode: 'start' }); // mode: 'start' | 'end'
  const [timePickerConfig, setTimePickerConfig] = useState({ visible: false, mode: 'start' }); // mode: 'start' | 'end'
  
  // UI states
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoadingBuses, setIsLoadingBuses] = useState(true);

  // Month navigation for custom calendar
  const [calendarDate, setCalendarDate] = useState(new Date());

  // Time picker selections
  const [tempHour, setTempHour] = useState('08');
  const [tempMinute, setTempMinute] = useState('00');

  useEffect(() => {
    const fetchUserBuses = async () => {
      try {
        setIsLoadingBuses(true);
        const allBuses = await getAllBuses();
        
        let filteredBuses = [];
        if (user?.role === 'BUS_DRIVER') {
          // Filter buses assigned to this driver
          filteredBuses = allBuses.filter(
            bus => bus.driver && bus.driver.id === user.id
          );
        } else if (user?.role === 'BUS_OWNER') {
          // Filter buses owned by this owner
          filteredBuses = allBuses.filter(
            bus => bus.owner && bus.owner.id === user.id
          );
        }
        
        setBuses(filteredBuses);
        if (filteredBuses.length > 0) {
          setSelectedBus(filteredBuses[0]); // Default to first bus
        }
      } catch (error) {
        Alert.alert('Error', 'Failed to load assigned buses. Please try again.');
      } finally {
        setIsLoadingBuses(false);
      }
    };

    if (user?.id) {
      fetchUserBuses();
    }
  }, [user]);

  const handleSelectBus = (bus) => {
    setSelectedBus(bus);
    setBusModalVisible(false);
  };

  const handleOpenDatePicker = (mode) => {
    setDatePickerConfig({ visible: true, mode });
    setCalendarDate(new Date());
  };

  const handleOpenTimePicker = (mode) => {
    setTimePickerConfig({ visible: true, mode });
    const currentVal = mode === 'start' ? startTime : endTime;
    if (currentVal) {
      const parts = currentVal.split(':');
      setTempHour(parts[0]);
      setTempMinute(parts[1]);
    } else {
      setTempHour('08');
      setTempMinute('00');
    }
  };

  const handleSelectDate = (day) => {
    const selected = new Date(calendarDate.getFullYear(), calendarDate.getMonth(), day);
    if (datePickerConfig.mode === 'start') {
      setStartDate(selected);
      // Automatically set end date to start date if it is currently unset or before start date
      if (!endDate || endDate < selected) {
        setEndDate(selected);
      }
    } else {
      if (startDate && selected < startDate) {
        Alert.alert('Invalid Date', 'End Date cannot be before Start Date.');
        return;
      }
      setEndDate(selected);
    }
    setDatePickerConfig({ visible: false, mode: 'start' });
  };

  const handleConfirmTime = () => {
    Keyboard.dismiss();
    const timeString = `${tempHour.padStart(2, '0')}:${tempMinute.padStart(2, '0')}`;
    if (timePickerConfig.mode === 'start') {
      setStartTime(timeString);
      if (!endTime) {
        setEndTime(timeString);
      }
    } else {
      if (startDate && endDate && startDate.toDateString() === endDate.toDateString() && startTime) {
        if (timeString <= startTime) {
          Alert.alert('Invalid Time', 'End Time must be after Start Time on the same day.');
          return;
        }
      }
      setEndTime(timeString);
    }
    setTimePickerConfig({ visible: false, mode: 'start' });
  };

  const handleSubmit = async () => {
    if (!selectedBus) {
      Alert.alert('Validation Error', 'Please select a bus.');
      return;
    }

    const payload = {
      requestedById: user.id,
      busId: selectedBus.id || selectedBus.busId,
      approvalType: approvalType,
    };

    if (approvalType === 'EMERGENCY') {
      if (!emergencyType) {
        Alert.alert('Validation Error', 'Please select an emergency reason.');
        return;
      }
      payload.emergencyType = emergencyType;
    } else {
      if (!startDate || !endDate || !startTime || !endTime) {
        Alert.alert('Validation Error', 'Please fill in all date and time fields.');
        return;
      }
      
      // Formatting date to YYYY-MM-DD
      const formatDate = (date) => {
        const d = new Date(date);
        let month = '' + (d.getMonth() + 1);
        let day = '' + d.getDate();
        const year = d.getFullYear();

        if (month.length < 2) month = '0' + month;
        if (day.length < 2) day = '0' + day;

        return [year, month, day].join('-');
      };

      payload.startDate = formatDate(startDate);
      payload.endDate = formatDate(endDate);
      payload.startTime = `${startTime}:00`;
      payload.endTime = `${endTime}:00`;
    }

    try {
      setIsSubmitting(true);
      await submitApprovalRequest(payload);
      Alert.alert(
        'Success',
        'Approval request submitted successfully.',
        [
          { 
            text: 'OK', 
            onPress: () => router.replace('/my-requests') 
          }
        ]
      );
    } catch (error) {
      Alert.alert('Submission Failed', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Custom Calendar Helpers
  const changeMonth = (direction) => {
    const nextMonth = new Date(calendarDate.getFullYear(), calendarDate.getMonth() + direction, 1);
    setCalendarDate(nextMonth);
  };

  const renderCalendarDays = () => {
    const year = calendarDate.getFullYear();
    const month = calendarDate.getMonth();
    
    // First day of the month
    const firstDayIndex = new Date(year, month, 1).getDay();
    // Days in current month
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const days = [];
    
    // Fill empty spots for starting weekdays
    for (let i = 0; i < firstDayIndex; i++) {
      days.push(<View key={`empty-${i}`} style={styles.calendarDayEmpty} />);
    }

    // Fill days of the month
    const todayStr = new Date().toDateString();
    for (let day = 1; day <= daysInMonth; day++) {
      const current = new Date(year, month, day);
      const isToday = current.toDateString() === todayStr;
      
      let isSelected = false;
      if (datePickerConfig.mode === 'start' && startDate) {
        isSelected = current.toDateString() === startDate.toDateString();
      } else if (datePickerConfig.mode === 'end' && endDate) {
        isSelected = current.toDateString() === endDate.toDateString();
      }

      const isPast = current < new Date(new Date().setHours(0, 0, 0, 0));

      days.push(
        <TouchableOpacity
          key={`day-${day}`}
          style={[
            styles.calendarDay,
            isToday && styles.calendarDayToday,
            isSelected && styles.calendarDaySelected,
            isPast && styles.calendarDayDisabled
          ]}
          disabled={isPast}
          onPress={() => handleSelectDate(day)}
        >
          <Text style={[
            styles.calendarDayText,
            isToday && styles.calendarDayTodayText,
            isSelected && styles.calendarDaySelectedText,
            isPast && styles.calendarDayDisabledText
          ]}>
            {day}
          </Text>
        </TouchableOpacity>
      );
    }

    return days;
  };

  const getMonthName = (date) => {
    return date.toLocaleString('en-US', { month: 'long', year: 'numeric' });
  };

  const formatDateString = (date) => {
    if (!date) return 'Select Date';
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#000" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>New Request</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContainer} showsVerticalScrollIndicator={false}>
        
        {/* Bus Selector */}
        <Text style={styles.label}>Select Bus</Text>
        {isLoadingBuses ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="small" color={Colors.primary} />
          </View>
        ) : (
          <TouchableOpacity 
            style={styles.busSelector}
            onPress={() => setBusModalVisible(true)}
            disabled={buses.length <= 1}
          >
            <View style={styles.busSelectorContent}>
              <Ionicons name="bus" size={24} color={Colors.primary} style={styles.busIcon} />
              <View>
                <Text style={styles.selectedBusNumber}>
                  {selectedBus ? selectedBus.busNumber : 'No assigned buses'}
                </Text>
                <Text style={styles.selectedBusRoute}>
                  {selectedBus ? selectedBus.route : 'Please contact admin'}
                </Text>
              </View>
            </View>
            {buses.length > 1 && (
              <Ionicons name="chevron-down" size={20} color="#8E8E93" />
            )}
          </TouchableOpacity>
        )}

        {/* Approval Type Segment Selector */}
        <Text style={styles.label}>Approval Type</Text>
        <View style={styles.segmentContainer}>
          <TouchableOpacity
            style={[styles.segmentButton, approvalType === 'EMERGENCY' && styles.segmentButtonActive]}
            onPress={() => setApprovalType('EMERGENCY')}
          >
            <Ionicons name="warning-outline" size={18} color={approvalType === 'EMERGENCY' ? '#FFF' : '#333'} />
            <Text style={[styles.segmentButtonText, approvalType === 'EMERGENCY' && styles.segmentButtonTextActive]}>
              Emergency
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.segmentButton, approvalType === 'NORMAL' && styles.segmentButtonActive]}
            onPress={() => setApprovalType('NORMAL')}
          >
            <Ionicons name="calendar-outline" size={18} color={approvalType === 'NORMAL' ? '#FFF' : '#333'} />
            <Text style={[styles.segmentButtonText, approvalType === 'NORMAL' && styles.segmentButtonTextActive]}>
              Normal
            </Text>
          </TouchableOpacity>
        </View>

        {/* Dynamic Fields */}
        {approvalType === 'EMERGENCY' ? (
          <View style={styles.section}>
            <Text style={styles.label}>Emergency Reason</Text>
            <View style={styles.emergencyGrid}>
              <TouchableOpacity
                style={[
                  styles.emergencyCard, 
                  emergencyType === 'ACCIDENT' && styles.emergencyCardActive,
                  { borderColor: emergencyType === 'ACCIDENT' ? '#FF3B30' : '#E5E5EA' }
                ]}
                onPress={() => setEmergencyType('ACCIDENT')}
              >
                <View style={[styles.emergencyIconContainer, { backgroundColor: '#FF3B3015' }]}>
                  <Ionicons name="car-sport" size={28} color="#FF3B30" />
                </View>
                <Text style={styles.emergencyTitle}>Accident</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[
                  styles.emergencyCard, 
                  emergencyType === 'BREAKDOWN' && styles.emergencyCardActive,
                  { borderColor: emergencyType === 'BREAKDOWN' ? '#FF9500' : '#E5E5EA' }
                ]}
                onPress={() => setEmergencyType('BREAKDOWN')}
              >
                <View style={[styles.emergencyIconContainer, { backgroundColor: '#FF950015' }]}>
                  <Ionicons name="construct" size={28} color="#FF9500" />
                </View>
                <Text style={styles.emergencyTitle}>Breakdown</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[
                  styles.emergencyCard, 
                  emergencyType === 'OTHER' && styles.emergencyCardActive,
                  { borderColor: emergencyType === 'OTHER' ? Colors.primary : '#E5E5EA' }
                ]}
                onPress={() => setEmergencyType('OTHER')}
              >
                <View style={[styles.emergencyIconContainer, { backgroundColor: Colors.primary + '15' }]}>
                  <Ionicons name="alert-circle" size={28} color={Colors.primary} />
                </View>
                <Text style={styles.emergencyTitle}>Other Emergency</Text>
              </TouchableOpacity>
            </View>
          </View>
        ) : (
          <View style={styles.section}>
            <Text style={styles.sectionHeading}>Request Schedule Change</Text>
            
            {/* Dates Grid */}
            <View style={styles.gridRow}>
              <View style={styles.gridCol}>
                <Text style={styles.subLabel}>Start Date</Text>
                <TouchableOpacity 
                  style={styles.pickerButton}
                  onPress={() => handleOpenDatePicker('start')}
                >
                  <Ionicons name="calendar-outline" size={20} color={Colors.primary} />
                  <Text style={styles.pickerButtonText}>{formatDateString(startDate)}</Text>
                </TouchableOpacity>
              </View>

              <View style={styles.gridCol}>
                <Text style={styles.subLabel}>End Date</Text>
                <TouchableOpacity 
                  style={styles.pickerButton}
                  onPress={() => handleOpenDatePicker('end')}
                >
                  <Ionicons name="calendar-outline" size={20} color={Colors.primary} />
                  <Text style={styles.pickerButtonText}>{formatDateString(endDate)}</Text>
                </TouchableOpacity>
              </View>
            </View>

            {/* Times Grid */}
            <View style={styles.gridRow}>
              <View style={styles.gridCol}>
                <Text style={styles.subLabel}>Start Time</Text>
                <TouchableOpacity 
                  style={styles.pickerButton}
                  onPress={() => handleOpenTimePicker('start')}
                >
                  <Ionicons name="time-outline" size={20} color={Colors.primary} />
                  <Text style={styles.pickerButtonText}>{startTime || 'Select Time'}</Text>
                </TouchableOpacity>
              </View>

              <View style={styles.gridCol}>
                <Text style={styles.subLabel}>End Time</Text>
                <TouchableOpacity 
                  style={styles.pickerButton}
                  onPress={() => handleOpenTimePicker('end')}
                >
                  <Ionicons name="time-outline" size={20} color={Colors.primary} />
                  <Text style={styles.pickerButtonText}>{endTime || 'Select Time'}</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        )}

        <TouchableOpacity 
          style={[styles.submitButton, isSubmitting && styles.submitButtonDisabled]}
          onPress={handleSubmit}
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <ActivityIndicator size="small" color="#FFF" />
          ) : (
            <Text style={styles.submitButtonText}>Submit Request</Text>
          )}
        </TouchableOpacity>

      </ScrollView>

      {/* Bus Selection Modal */}
      <Modal visible={busModalVisible} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Select a Bus</Text>
              <TouchableOpacity onPress={() => setBusModalVisible(false)}>
                <Ionicons name="close" size={24} color="#000" />
              </TouchableOpacity>
            </View>
            <ScrollView style={styles.busListContainer}>
              {buses.map(bus => (
                <TouchableOpacity 
                  key={bus.id} 
                  style={styles.busItem}
                  onPress={() => handleSelectBus(bus)}
                >
                  <Ionicons name="bus" size={22} color={Colors.primary} />
                  <View style={styles.busItemTextContainer}>
                    <Text style={styles.busItemNumber}>{bus.busNumber}</Text>
                    <Text style={styles.busItemRoute}>{bus.route}</Text>
                  </View>
                  {selectedBus?.id === bus.id && (
                    <Ionicons name="checkmark-circle" size={22} color="#34C759" />
                  )}
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* Date Picker Modal */}
      <Modal visible={datePickerConfig.visible} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.calendarCard}>
            <View style={styles.calendarHeader}>
              <TouchableOpacity onPress={() => changeMonth(-1)}>
                <Ionicons name="chevron-back" size={24} color={Colors.primary} />
              </TouchableOpacity>
              <Text style={styles.calendarTitle}>{getMonthName(calendarDate)}</Text>
              <TouchableOpacity onPress={() => changeMonth(1)}>
                <Ionicons name="chevron-forward" size={24} color={Colors.primary} />
              </TouchableOpacity>
            </View>

            <View style={styles.calendarWeekdays}>
              {['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'].map(day => (
                <Text key={day} style={styles.calendarWeekdayText}>{day}</Text>
              ))}
            </View>

            <View style={styles.calendarGrid}>
              {renderCalendarDays()}
            </View>

            <TouchableOpacity 
              style={styles.calendarCloseButton}
              onPress={() => setDatePickerConfig({ visible: false, mode: 'start' })}
            >
              <Text style={styles.calendarCloseButtonText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Time Picker Modal */}
      <Modal 
        visible={timePickerConfig.visible} 
        transparent 
        animationType="fade"
        onRequestClose={() => {
          Keyboard.dismiss();
          setTimePickerConfig({ visible: false, mode: 'start' });
        }}
      >
        <TouchableWithoutFeedback onPress={Keyboard.dismiss}>
          <View style={styles.modalOverlay}>
            <TouchableWithoutFeedback onPress={Keyboard.dismiss}>
              <View style={styles.timePickerCard}>
                <Text style={styles.timePickerTitle}>
                  Select {timePickerConfig.mode === 'start' ? 'Start' : 'End'} Time (24h)
                </Text>
                
                <View style={styles.timePickerRow}>
                  {/* Hour Input */}
                  <View style={styles.timeInputCol}>
                    <Text style={styles.timeLabel}>Hour</Text>
                    <TextInput
                      style={styles.timeInput}
                      keyboardType="number-pad"
                      maxLength={2}
                      value={tempHour}
                      onChangeText={(val) => {
                        const clean = val.replace(/[^0-9]/g, '');
                        if (Number(clean) >= 0 && Number(clean) <= 23) {
                          setTempHour(clean);
                        }
                      }}
                      selectTextOnFocus
                    />
                  </View>
                  
                  <Text style={styles.timeColon}>:</Text>

                  {/* Minute Input */}
                  <View style={styles.timeInputCol}>
                    <Text style={styles.timeLabel}>Minute</Text>
                    <TextInput
                      style={styles.timeInput}
                      keyboardType="number-pad"
                      maxLength={2}
                      value={tempMinute}
                      onChangeText={(val) => {
                        const clean = val.replace(/[^0-9]/g, '');
                        if (Number(clean) >= 0 && Number(clean) <= 59) {
                          setTempMinute(clean);
                        }
                      }}
                      selectTextOnFocus
                    />
                  </View>
                </View>

                <View style={styles.timePickerActions}>
                  <TouchableOpacity 
                    style={[styles.timePickerButton, styles.timeCancelButton]}
                    onPress={() => {
                      Keyboard.dismiss();
                      setTimePickerConfig({ visible: false, mode: 'start' });
                    }}
                  >
                    <Text style={styles.timeCancelButtonText}>Cancel</Text>
                  </TouchableOpacity>
                  <TouchableOpacity 
                    style={[styles.timePickerButton, styles.timeConfirmButton]}
                    onPress={handleConfirmTime}
                  >
                    <Text style={styles.timeConfirmButtonText}>Confirm</Text>
                  </TouchableOpacity>
                </View>
              </View>
            </TouchableWithoutFeedback>
          </View>
        </TouchableWithoutFeedback>
      </Modal>

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
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#000',
  },
  scrollContainer: {
    padding: 20,
    paddingBottom: 40,
  },
  label: {
    fontSize: 15,
    fontWeight: '600',
    color: '#3A3A3C',
    marginBottom: 8,
    marginTop: 16,
  },
  subLabel: {
    fontSize: 13,
    fontWeight: '500',
    color: '#8E8E93',
    marginBottom: 6,
  },
  loadingContainer: {
    backgroundColor: '#FFF',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E5E5EA',
  },
  busSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E5E5EA',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.03,
    shadowRadius: 5,
    elevation: 1,
  },
  busSelectorContent: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  busIcon: {
    marginRight: 16,
  },
  selectedBusNumber: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  selectedBusRoute: {
    fontSize: 13,
    color: '#8E8E93',
    marginTop: 2,
  },
  segmentContainer: {
    flexDirection: 'row',
    backgroundColor: '#E5E5EA',
    borderRadius: 12,
    padding: 4,
    marginBottom: 8,
  },
  segmentButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 10,
    borderRadius: 8,
  },
  segmentButtonActive: {
    backgroundColor: Colors.primary,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.15,
    shadowRadius: 3,
    elevation: 2,
  },
  segmentButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#3A3A3C',
  },
  segmentButtonTextActive: {
    color: '#FFF',
  },
  section: {
    marginTop: 16,
  },
  sectionHeading: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1C1C1E',
    marginBottom: 16,
  },
  emergencyGrid: {
    flexDirection: 'row',
    gap: 10,
  },
  emergencyCard: {
    flex: 1,
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 14,
    alignItems: 'center',
    borderWidth: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.04,
    shadowRadius: 6,
    elevation: 1,
  },
  emergencyCardActive: {
    backgroundColor: '#FFF',
  },
  emergencyIconContainer: {
    width: 54,
    height: 54,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 10,
  },
  emergencyTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: '#1C1C1E',
    textAlign: 'center',
  },
  gridRow: {
    flexDirection: 'row',
    gap: 16,
    marginBottom: 16,
  },
  gridCol: {
    flex: 1,
  },
  pickerButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: '#FFF',
    borderWidth: 1,
    borderColor: '#E5E5EA',
    borderRadius: 12,
    padding: 14,
  },
  pickerButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  submitButton: {
    backgroundColor: Colors.primary,
    borderRadius: 16,
    paddingVertical: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 32,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 3,
  },
  submitButtonDisabled: {
    backgroundColor: '#A2C8EC',
  },
  submitButtonText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '700',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: '#FFF',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 40,
    width: '100%',
    maxHeight: '60%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  busListContainer: {
    marginVertical: 10,
  },
  busItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#F2F2F7',
  },
  busItemTextContainer: {
    flex: 1,
    marginLeft: 16,
  },
  busItemNumber: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  busItemRoute: {
    fontSize: 13,
    color: '#8E8E93',
    marginTop: 2,
  },
  calendarCard: {
    backgroundColor: '#FFF',
    borderRadius: 24,
    padding: 20,
    width: '90%',
    marginBottom: '20%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.15,
    shadowRadius: 20,
    elevation: 5,
  },
  calendarHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  calendarTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  calendarWeekdays: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  calendarWeekdayText: {
    flex: 1,
    textAlign: 'center',
    fontSize: 12,
    fontWeight: '600',
    color: '#8E8E93',
  },
  calendarGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'flex-start',
  },
  calendarDay: {
    width: '14.28%',
    aspectRatio: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: 4,
    borderRadius: 20,
  },
  calendarDayEmpty: {
    width: '14.28%',
    aspectRatio: 1,
  },
  calendarDayToday: {
    borderWidth: 1.5,
    borderColor: Colors.primary,
  },
  calendarDaySelected: {
    backgroundColor: Colors.primary,
  },
  calendarDayDisabled: {
    opacity: 0.3,
  },
  calendarDayText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  calendarDayTodayText: {
    color: Colors.primary,
    fontWeight: '700',
  },
  calendarDaySelectedText: {
    color: '#FFF',
    fontWeight: '700',
  },
  calendarDayDisabledText: {
    color: '#8E8E93',
  },
  calendarCloseButton: {
    marginTop: 16,
    paddingVertical: 12,
    alignItems: 'center',
  },
  calendarCloseButtonText: {
    fontSize: 15,
    fontWeight: '600',
    color: '#FF3B30',
  },
  timePickerCard: {
    backgroundColor: '#FFF',
    borderRadius: 24,
    padding: 24,
    width: '85%',
    marginBottom: '30%',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.15,
    shadowRadius: 20,
    elevation: 5,
  },
  timePickerTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#1C1C1E',
    marginBottom: 20,
  },
  timePickerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  timeInputCol: {
    alignItems: 'center',
  },
  timeInput: {
    backgroundColor: '#F2F2F7',
    borderRadius: 12,
    width: 64,
    height: 64,
    textAlign: 'center',
    fontSize: 28,
    fontWeight: '700',
    color: '#1C1C1E',
  },
  timeLabel: {
    fontSize: 11,
    color: '#8E8E93',
    fontWeight: '600',
    textTransform: 'uppercase',
    marginBottom: 6,
  },
  timeColon: {
    fontSize: 32,
    fontWeight: '700',
    color: '#1C1C1E',
    marginHorizontal: 16,
    paddingBottom: 4,
  },
  timePickerActions: {
    flexDirection: 'row',
    gap: 12,
    width: '100%',
  },
  timePickerButton: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  timeCancelButton: {
    backgroundColor: '#F2F2F7',
  },
  timeCancelButtonText: {
    color: '#FF3B30',
    fontSize: 14,
    fontWeight: '700',
  },
  timeConfirmButton: {
    backgroundColor: Colors.primary,
  },
  timeConfirmButtonText: {
    color: '#FFF',
    fontSize: 14,
    fontWeight: '700',
  },
});
