import React, { createContext, useState, useContext, useEffect, useCallback, useRef } from 'react';
import { useAuth } from './AuthContext';
import { getAssignedBusByDriverId, updateBusStatus, getAllBuses, startAssignment, completeAssignment } from '../services/busService';
import * as SecureStore from '../utils/storage';
import { showError } from '../utils/alertService';


const BusContext = createContext();

export const useBus = () => useContext(BusContext);

export const BusProvider = ({ children }) => {
  const { user } = useAuth();
  const [currentBus, setCurrentBus] = useState(null);
  const [allBuses, setAllBuses] = useState([]);
  const [isDriving, setIsDriving] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const updating = useRef(false);
  const identity = useRef(user?.id);
  identity.current = user?.id;

  const fetchAssignedBus = useCallback(async () => {
    if (!user?.id || user.role !== 'BUS_DRIVER') return;
    
    setIsLoading(true);
    try {
      // Check if user has already selected a bus in this session or previously
      const savedBusId = await SecureStore.getItemAsync(`selectedBusId_${user.id}`);
      
      let busData;
      if (savedBusId) {
        const buses = await getAllBuses();
        busData = buses.find(b => (b.id || b.busId).toString() === savedBusId && String(b.driver?.id) === String(user.id));
      }
      
      if (!busData) {
        busData = await getAssignedBusByDriverId(user.id);
      }

      if (identity.current !== user.id) return;
      setCurrentBus(busData);
      
      if (busData) {
        const isCurrentlyDriving = busData.status === 'DRIVING' || busData.driver?.status === 'DRIVING';
        setIsDriving(isCurrentlyDriving);
        if (isCurrentlyDriving) {
          await SecureStore.setItemAsync(`isDriving_${user.id}`, 'true');
        } else {
          await SecureStore.setItemAsync(`isDriving_${user.id}`, 'false');
        }
      } else setIsDriving(false);
    } catch (error) {
      if (identity.current === user.id) showError('Bus Service', 'Failed to fetch assigned bus');
    } finally {
      if (identity.current === user.id) setIsLoading(false);
    }
  }, [user?.id, user?.role]);

  const fetchAllBuses = useCallback(async () => {
    if (user?.role !== 'BUS_DRIVER') return;
    setIsLoading(true);
    try {
      const data = await getAllBuses();
      setAllBuses(data);
    } catch (error) {
      showError('Bus Service', 'Failed to fetch all buses');
    } finally {
      setIsLoading(false);
    }
  }, [user?.role]);

  useEffect(() => {
    setCurrentBus(null); setAllBuses([]); setIsDriving(false); setIsLoading(false);
    fetchAssignedBus();
  }, [fetchAssignedBus]);

  const selectBus = async (bus) => {
    if (!user?.id) return;
    
    try {
      setIsLoading(true);
      setCurrentBus(bus);
      setIsDriving(false); // Reset driving state when switching buses
      await SecureStore.setItemAsync(`selectedBusId_${user.id}`, (bus.id || bus.busId).toString());
      await SecureStore.setItemAsync(`isDriving_${user.id}`, 'false');
    } catch (error) {
      showError('Bus Selection', 'Failed to select bus');
    } finally {
      setIsLoading(false);
    }
  };

  const toggleDriving = async (assignmentId = null) => {
    if (!currentBus || !user?.id || updating.current) return;
    updating.current = true;
    setIsLoading(true);

    const newDrivingState = !isDriving;
    const status = newDrivingState ? 'DRIVING' : 'IDLE';

    try {
      await updateBusStatus(currentBus.id || currentBus.busId, status, user.id);
      
      if (assignmentId) {
        if (newDrivingState) {
          const res = await startAssignment(assignmentId);
          if (res && (res.status === 'CANCELLED' || res.status === 'TURN_LOSS')) {
            await updateBusStatus(currentBus.id || currentBus.busId, 'IDLE', user.id);
            setIsDriving(false);
            await SecureStore.setItemAsync(`isDriving_${user.id}`, 'false');
            showError('Turn Loss', 'Your turn has been cancelled due to a delay exceeding 30 minutes.');
            await fetchAssignedBus();
            return;
          }
        } else {
          await completeAssignment(assignmentId);
        }
      }

      setIsDriving(newDrivingState);
      await SecureStore.setItemAsync(`isDriving_${user.id}`, newDrivingState.toString());
      
      // Refresh the bus state to fetch updated schedules
      await fetchAssignedBus();
    } catch (error) {
      try { await updateBusStatus(currentBus.id || currentBus.busId, isDriving ? 'DRIVING' : 'IDLE', user.id); } catch (_) { /* Reconcile on refresh. */ }
      await fetchAssignedBus();
      showError('Bus Status', 'Failed to update driving status');
    } finally {
      updating.current = false;
      setIsLoading(false);
    }
  };

  return (
    <BusContext.Provider value={{ 
      currentBus, 
      allBuses,
      isDriving, 
      isLoading, 
      refreshBus: fetchAssignedBus,
      fetchAllBuses,
      selectBus,
      toggleDriving 
    }}>
      {children}
    </BusContext.Provider>
  );
};
