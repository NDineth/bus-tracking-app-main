package com.bustrackpro.service.bus;

import com.bustrackpro.dto.request.bus.BusRequest;
import com.bustrackpro.dto.response.bus.BusResponse;
import com.bustrackpro.dto.response.bus.FleetSummaryResponse;
import java.util.List;

/**
 * Bus service interface.
 */
public interface BusService {
    BusResponse addBus(BusRequest busRequest);

    List<BusResponse> getAllBuses();

    BusResponse getBusById(Integer id);

    BusResponse updateBus(Integer id, BusRequest busRequest);

    void deleteBus(Integer id);

    FleetSummaryResponse getFleetSummary(Integer ownerId, String dateStr);
}
