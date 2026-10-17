package com.bustrackpro.controller.tracking;

import com.bustrackpro.modal.tracking.AlertPolicy;
import com.bustrackpro.service.tracking.AlertPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alert-policy")
@RequiredArgsConstructor
public class AlertPolicyController {
    private final AlertPolicyService service;

    @GetMapping
    public AlertPolicy getPolicy() {
        return service.getPolicy();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public AlertPolicy updatePolicy(@Valid @RequestBody AlertPolicy policy) {
        return service.updatePolicy(policy);
    }
}
