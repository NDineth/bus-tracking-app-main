package com.bustrackpro.service.route;

import com.bustrackpro.dto.request.route.BusRouteRequest;
import com.bustrackpro.dto.response.route.BusRouteResponse;
import com.bustrackpro.modal.route.BusHalt;
import com.bustrackpro.modal.route.BusRoute;
import com.bustrackpro.modal.route.BusRouteHalt;
import com.bustrackpro.repository.BusHaltRepository;
import com.bustrackpro.repository.BusRouteHaltRepository;
import com.bustrackpro.repository.BusRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bus route service.
 */
@Service
@RequiredArgsConstructor
public class BusRouteService {

    private final BusRouteRepository busRouteRepository;
    private final BusHaltRepository busHaltRepository;
    private final BusRouteHaltRepository busRouteHaltRepository;

    /**
     * Add a new bus route.
     *
     * @param busRouteRequest - DTO for receiving bus route requests
     * @return - DTO for sending bus route responses
     */
    @Transactional
    public BusRouteResponse addBusRoute(BusRouteRequest busRouteRequest) {
        BusRoute busRoute = new BusRoute();
        busRoute.setRouteNumber(busRouteRequest.getRouteNumber());
        busRoute.setStartLocation(busRouteRequest.getStartLocation());
        busRoute.setEndLocation(busRouteRequest.getEndLocation());
        busRoute.setDistance(busRouteRequest.getDistance());
        busRoute = busRouteRepository.save(busRoute);

        List<BusRouteHalt> savedHalts = new ArrayList<>();
        if (busRouteRequest.getHalts() != null) {
            for (var haltRequest : busRouteRequest.getHalts()) {
                BusHalt busHalt = new BusHalt();
                busHalt.setName(haltRequest.getName());
                busHalt.setLatitude(haltRequest.getLatitude());
                busHalt.setLongitude(haltRequest.getLongitude());
                busHalt = busHaltRepository.save(busHalt);

                BusRouteHalt busRouteHalt = new BusRouteHalt();
                busRouteHalt.setBusRoute(busRoute);
                busRouteHalt.setBusHalt(busHalt);
                busRouteHalt.setStopOrder(haltRequest.getStopOrder());
                busRouteHalt = busRouteHaltRepository.save(busRouteHalt);
                savedHalts.add(busRouteHalt);
            }
        }

        return new BusRouteResponse(busRoute, savedHalts);
    }

    /**
     * Get all bus routes.
     *
     * @return - DTO for sending bus route responses
     */
    public List<BusRouteResponse> getAllBusRoutes() {
        List<BusRoute> routes = busRouteRepository.findAll();
        return routes.stream().map(route -> {
            List<BusRouteHalt> halts = busRouteHaltRepository.findByBusRoute(route);
            return new BusRouteResponse(route, halts);
        }).collect(Collectors.toList());
    }

    /**
     * Get bus route by id.
     *
     * @param id - Bus route id
     * @return - DTO for sending bus route responses
     */
    public BusRouteResponse getBusRouteById(Integer id) {
        BusRoute route = busRouteRepository.findById(id).orElseThrow(() -> new RuntimeException("Route not found"));
        List<BusRouteHalt> halts = busRouteHaltRepository.findByBusRoute(route);
        return new BusRouteResponse(route, halts);
    }

    /**
     * Update bus route.
     *
     * @param id - Bus route id
     * @param busRouteRequest - DTO for receiving bus route requests
     * @return - DTO for sending bus route responses
     */
    @Transactional
    public BusRouteResponse updateBusRoute(Integer id, BusRouteRequest busRouteRequest) {
        BusRoute busRoute = busRouteRepository.findById(id).orElseThrow(() -> new RuntimeException("Route not found"));
        busRoute.setRouteNumber(busRouteRequest.getRouteNumber());
        busRoute.setStartLocation(busRouteRequest.getStartLocation());
        busRoute.setEndLocation(busRouteRequest.getEndLocation());
        busRoute.setDistance(busRouteRequest.getDistance());
        busRoute = busRouteRepository.save(busRoute);

        // Delete existing route halts
        busRouteHaltRepository.deleteByBusRoute(busRoute);

        List<BusRouteHalt> savedHalts = new ArrayList<>();
        if (busRouteRequest.getHalts() != null) {
            for (var haltRequest : busRouteRequest.getHalts()) {
                BusHalt busHalt = new BusHalt();
                busHalt.setName(haltRequest.getName());
                busHalt.setLatitude(haltRequest.getLatitude());
                busHalt.setLongitude(haltRequest.getLongitude());
                busHalt = busHaltRepository.save(busHalt);

                BusRouteHalt busRouteHalt = new BusRouteHalt();
                busRouteHalt.setBusRoute(busRoute);
                busRouteHalt.setBusHalt(busHalt);
                busRouteHalt.setStopOrder(haltRequest.getStopOrder());
                busRouteHalt = busRouteHaltRepository.save(busRouteHalt);
                savedHalts.add(busRouteHalt);
            }
        }

        return new BusRouteResponse(busRoute, savedHalts);
    }

    /**
     * Delete bus route.
     *
     * @param id - Bus route id
     */
    @Transactional
    public void deleteBusRoute(Integer id) {
        BusRoute route = busRouteRepository.findById(id).orElseThrow(() -> new RuntimeException("Route not found"));
        busRouteHaltRepository.deleteByBusRoute(route);
        busRouteRepository.delete(route);
    }
}
