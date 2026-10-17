package com.bustrackpro.controller.finance;

import com.bustrackpro.config.jwt.UserDetailsImpl;
import com.bustrackpro.dto.request.finance.PenaltyRequest;
import com.bustrackpro.dto.response.finance.PenaltyRecordResponse;
import com.bustrackpro.service.finance.PenaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/finance/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_AUTHORITY_USER')")
    public ResponseEntity<PenaltyRecordResponse> applyPenalty(
            @Valid @RequestBody PenaltyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // userDetails.getId() provides the ID of the authenticated user
        PenaltyRecordResponse response = penaltyService.applyPenalty(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_AUTHORITY_USER')")
    public ResponseEntity<List<PenaltyRecordResponse>> getAllPenalties() {
        return ResponseEntity.ok(penaltyService.getAllPenalties());
    }
}
