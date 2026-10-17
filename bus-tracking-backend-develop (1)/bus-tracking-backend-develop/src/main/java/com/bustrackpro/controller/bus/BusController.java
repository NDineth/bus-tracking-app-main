package com.bustrackpro.controller.bus;

import com.bustrackpro.dto.request.bus.BusRequest;
import com.bustrackpro.dto.response.bus.BusResponse;
import com.bustrackpro.dto.response.bus.FleetSummaryResponse;
import com.bustrackpro.service.bus.BusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bus controller.
 */
@RestController
@RequestMapping("/buses")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;

    /**
     * Get active fleet count and bus availability summary.
     *
     * @param ownerId - Optional owner ID filter
     * @param date - Optional date string (YYYY-MM-DD)
     * @return FleetSummaryResponse
     */
    @GetMapping("/fleet-summary")
    public ResponseEntity<FleetSummaryResponse> getFleetSummary(
            @RequestParam(required = false) Integer ownerId,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(busService.getFleetSummary(ownerId, date));
    }

    /**
     * Add a new bus.
     *
     * @param busRequest - DTO for receiving bus requests
     * @return - DTO for sending bus responses
     */
    @PostMapping
    public ResponseEntity<BusResponse> addBus(@RequestBody BusRequest busRequest) {
        return ResponseEntity.ok(busService.addBus(busRequest));
    }

    /**
     * Get all buses.
     *
     * @return - List of DTO for sending bus responses
     */
    @GetMapping
    public ResponseEntity<List<BusResponse>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllBuses());
    }

    /**
     * Get bus by id.
     *
     * @param id - Bus id
     * @return - DTO for sending bus responses
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusResponse> getBusById(@PathVariable Integer id) {
        return ResponseEntity.ok(busService.getBusById(id));
    }

    /**
     * Update bus.
     *
     * @param id - Bus id
     * @param busRequest - DTO for receiving bus requests
     * @return - DTO for sending bus responses
     */
    @PutMapping("/{id}")
    public ResponseEntity<BusResponse> updateBus(
            @PathVariable Integer id,
            @RequestBody BusRequest busRequest) {
        return ResponseEntity.ok(busService.updateBus(id, busRequest));
    }

    /**
     * Delete bus.
     *
     * @param id - Bus id
     * @return - No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBus(@PathVariable Integer id) {
        busService.deleteBus(id);
        return ResponseEntity.noContent().build();
    }
}
