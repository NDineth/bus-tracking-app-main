package com.bustrackpro.controller.route;

import com.bustrackpro.dto.request.route.BusRouteRequest;
import com.bustrackpro.dto.response.route.BusRouteResponse;
import com.bustrackpro.service.route.BusRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Bus route controller.
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BusRouteController {

    private final BusRouteService busRouteService;

    /**
     * Add a new bus route.
     *
     * @param busRouteRequest - DTO for receiving bus route requests
     * @return - DTO for sending bus route responses
     */
    @PostMapping
    public ResponseEntity<BusRouteResponse> addBusRoute(@RequestBody BusRouteRequest busRouteRequest) {
        return ResponseEntity.ok(busRouteService.addBusRoute(busRouteRequest));
    }

    /**
     * Get all bus routes.
     *
     * @return - DTO for sending bus route responses
     */
    @GetMapping
    public ResponseEntity<List<BusRouteResponse>> getAllBusRoutes() {
        return ResponseEntity.ok(busRouteService.getAllBusRoutes());
    }

    /**
     * Get bus route by id.
     *
     * @param id - Bus route id
     * @return - DTO for sending bus route responses
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusRouteResponse> getBusRouteById(@PathVariable Integer id) {
        return ResponseEntity.ok(busRouteService.getBusRouteById(id));
    }

    /**
     * Update bus route.
     *
     * @param id - Bus route id
     * @param busRouteRequest - DTO for receiving bus route requests
     * @return - DTO for sending bus route responses
     */
    @PutMapping("/{id}")
    public ResponseEntity<BusRouteResponse> updateBusRoute(
            @PathVariable Integer id,
            @RequestBody BusRouteRequest busRouteRequest) {
        return ResponseEntity.ok(busRouteService.updateBusRoute(id, busRouteRequest));
    }

    /**
     * Delete bus route.
     *
     * @param id - Bus route id
     * @return - No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusRoute(@PathVariable Integer id) {
        busRouteService.deleteBusRoute(id);
        return ResponseEntity.noContent().build();
    }
}
