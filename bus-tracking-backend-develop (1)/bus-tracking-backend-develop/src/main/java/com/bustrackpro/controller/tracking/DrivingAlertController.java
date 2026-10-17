package com.bustrackpro.controller.tracking;

import com.bustrackpro.dto.request.tracking.DrivingAlertRequest;
import com.bustrackpro.service.tracking.DrivingBehaviorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class DrivingAlertController {

    private final DrivingBehaviorService drivingBehaviorService;

    @PostMapping("/alert")
    public ResponseEntity<String> receiveDrivingAlert(@RequestBody DrivingAlertRequest alert) {
        drivingBehaviorService.processDrivingAlert(alert);
        return ResponseEntity.ok("Alert received and processed successfully");
    }
}
