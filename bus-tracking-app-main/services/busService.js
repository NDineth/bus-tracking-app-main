import API from './api';

const getColomboNow = () => {
  const colomboOffset = 5.5 * 60 * 60 * 1000;
  return new Date(Date.now() + colomboOffset);
};

const getColomboDateStr = () => {
  const date = getColomboNow();
  let month = '' + (date.getUTCMonth() + 1);
  let day = '' + date.getUTCDate();
  const year = date.getUTCFullYear();

  if (month.length < 2) month = '0' + month;
  if (day.length < 2) day = '0' + day;

  return [year, month, day].join('-');
};

const formatTime = (timeStr) => {
  if (!timeStr) return 'N/A';
  try {
    const parts = timeStr.split(':');
    let hours = parseInt(parts[0], 10);
    const minutes = parts[1];
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12;
    hours = hours ? hours : 12; // the hour '0' should be '12'
    return `${hours.toString().padStart(2, '0')}:${minutes} ${ampm}`;
  } catch (e) {
    return timeStr;
  }
};

export const getAllBuses = async () => {
  try {
    const response = await API.get('/buses');
    
    const data = response.data;
    const buses = Array.isArray(data) ? data : data?.content;
    if (!Array.isArray(buses)) throw new Error('The server returned an unexpected bus list. Check the backend API address.');
    return buses.map(bus => {
      const assignedDriver = bus.driver
        ? `${bus.driver.firstName} ${bus.driver.lastName}`
        : 'None';
      const status = bus.availabilityStatus === 'UNAVAILABLE' ? 'UNAVAILABLE'
        : bus.status || bus.driver?.status || 'IDLE';

      return {
        id: bus.id,
        busId: bus.id,
        busNumber: bus.busNumber,
        route: bus.route || 'No Active Route',
        capacity: bus.capacity,
        status: status,
        availabilityStatus: bus.availabilityStatus,
        unavailabilityReason: bus.unavailabilityReason,
        assignedDriver,
        driver: bus.driver,
        nextTurn: bus.nextTurn || 'N/A',
        turns: bus.turns && bus.turns.length > 0 ? bus.turns : ['No active turns today'],
        owner: bus.owner
      };
    });
  } catch (error) {
    throw error;
  }
};

export const getAssignedBusByDriverId = async (driverId) => {
  try {
    const buses = await getAllBuses();
    const assigned = buses.find(bus => 
      bus.driver && String(bus.driver.id) === String(driverId)
    );
    return assigned || null;
  } catch (error) {
    throw error;
  }
};

export const updateBusStatus = async (busId, status, driverId) => {
  try {
    const response = await API.put(`/users/${driverId}/status`, { status });
    return {
      success: true,
      message: `Status updated to ${status}`,
      data: response.data
    };
  } catch (error) {
    throw error.response?.data?.message || 'Failed to update status';
  }
};

export const getRouteSchedule = async (route, busNumber) => {
  try {
    const today = getColomboDateStr();
    const response = await API.get(`/schedules/assignments/date/${today}`);
    
    const activeAssignments = (response.data || []).filter(item => 
      item.status !== 'CANCELLED'
    );
    // Sort assignments by departure time first
    const sortedAssignments = activeAssignments.sort((a, b) => 
      a.departureTime.localeCompare(b.departureTime)
    );

    // Filter assignments that match the route
    const filteredAssignments = (route && route !== 'No Active Route') 
      ? sortedAssignments.filter(item => {
          // Try matching route number first (extracted from route name e.g. "177 (Kaduwela...)")
          const busRouteNumber = route.split(' ')[0].split('(')[0].trim().toLowerCase();
          const itemRouteNumber = (item.routeNumber || '').trim().toLowerCase();
          
          if (busRouteNumber && itemRouteNumber && busRouteNumber === itemRouteNumber) {
            return true;
          }

          // Fallback to tripLabel word matching
          if (!item.tripLabel) return false;
          
          const getWords = (str) => {
            return str
              .toLowerCase()
              .replace(/[^a-z0-9]/g, ' ')
              .split(/\s+/)
              .filter(w => w.length >= 3 && isNaN(w));
          };
          
          const routeWords = getWords(route);
          const tripWords = getWords(item.tripLabel);
          
          if (routeWords.length === 0 || tripWords.length === 0) return false;
          
          const allIn = (wordsA, wordsB) => wordsA.every(w => wordsB.includes(w));
          return allIn(routeWords, tripWords) || allIn(tripWords, routeWords);
        })
      : sortedAssignments;

    return filteredAssignments.map((item, index) => {
      let status = 'UPCOMING';
      if (item.status === 'COMPLETED' || item.status === 'TURN_LOSS') {
        status = 'PAST';
      } else if (item.status === 'ACTIVE' || item.status === 'IN_PROGRESS') {
        status = 'CURRENT';
      }
      
      return {
        id: item.id,
        turn: index + 1,
        time: formatTime(item.departureTime),
        bus: item.busNumber || 'Unassigned',
        isYourTurn: item.busNumber ? item.busNumber === busNumber : false,
        status,
        permitFee: item.permitFee,
        offPeakCommission: item.offPeakCommission,
        isOffPeak: item.isOffPeak,
        rawStatus: item.status,
        rawTime: item.departureTime,
        route: item.tripLabel || 'Unknown Route'
      };
    });
  } catch (error) {
    throw error.response?.data?.message || 'Failed to fetch schedule';
  }
};


export const getBusFutureAgenda = async (busNumber) => {
  try {
    const agenda = [];
    const dates = [];
    
    for (let i = 0; i < 3; i++) {
      const date = getColomboNow();
      date.setUTCDate(date.getUTCDate() + i);
      
      let month = '' + (date.getUTCMonth() + 1);
      let day = '' + date.getUTCDate();
      const year = date.getUTCFullYear();

      if (month.length < 2) month = '0' + month;
      if (day.length < 2) day = '0' + day;

      const dateStr = [year, month, day].join('-');
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const label = i === 0 ? 'Today' : (i === 1 ? 'Tomorrow' : `${months[date.getUTCMonth()]} ${day}`);
      dates.push({ dateStr, label });
    }
    
    for (const d of dates) {
      try {
        const response = await API.get(`/schedules/assignments/date/${d.dateStr}`);
        const busAssignments = response.data.filter(item => 
          item.busNumber === busNumber && 
          item.status !== 'CANCELLED' && 
          item.status !== 'TURN_LOSS'
        );
        
        busAssignments.forEach((item, index) => {
          let status = 'UPCOMING';
          if (item.status === 'COMPLETED') {
            status = 'PAST';
          } else if (item.status === 'ACTIVE') {
            status = 'CURRENT';
          }
          
          agenda.push({
            id: item.id || `${d.dateStr}-${index}`,
            date: d.label,
            time: item.departureTime,
            route: item.tripLabel || 'Route not assigned',
            turn: index + 1,
            status
          });
        });
      } catch (e) {
        throw e;
      }
    }
    
    return agenda;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to fetch agenda';
  }
};

export const startAssignment = async (assignmentId) => {
  try {
    const response = await API.post(`/schedules/assignments/${assignmentId}/start`);
    return response.data;
  } catch (error) {
    // Fallback if /start endpoint doesn't exist but we can update status
    try {
      const response = await API.put(`/schedules/assignments/${assignmentId}`, { status: 'ACTIVE' });
      return response.data;
    } catch (fallbackError) {
      throw error.response?.data?.message || 'Failed to start run';
    }
  }
};

export const completeAssignment = async (assignmentId) => {
  try {
    const response = await API.post(`/schedules/assignments/${assignmentId}/complete`);
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to complete run';
  }
};

export const getDailyNetSummary = async (busId, date) => {
  try {
    const response = await API.get(`/finance/buses/${busId}/daily-net`, {
      params: { date }
    });
    const data = response.data;
    if (data && Array.isArray(data.runs)) {
      data.runs = data.runs.map(run => ({
        ...run,
        departureTime: formatTime(run.departureTime),
        arrivalTime: formatTime(run.arrivalTime)
      }));
    }
    return data;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to fetch daily net summary';
  }
};

export const getFinanceSummary = async (from, to, busId) => {
  try {
    const params = {};
    if (from) params.from = from;
    if (to) params.to = to;
    if (busId) params.busId = busId;
    const response = await API.get('/finance/summary', { params });
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to fetch finance summary';
  }
};

export const getFleetSummary = async (ownerId, date) => {
  try {
    const params = {};
    if (ownerId) params.ownerId = ownerId;
    if (date) params.date = date;
    const response = await API.get('/buses/fleet-summary', { params });
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to fetch fleet summary';
  }
};

export default {
  getAssignedBusByDriverId,
  updateBusStatus,
  getAllBuses,
  getRouteSchedule,
  getBusFutureAgenda,
  startAssignment,
  completeAssignment,
  getDailyNetSummary,
  getFinanceSummary,
  getFleetSummary,
};

