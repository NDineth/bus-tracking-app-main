package com.bustrackpro.controller.finance;

import com.bustrackpro.dto.request.finance.DelayPenaltyRuleRequest;
import com.bustrackpro.modal.finance.DelayPenaltyRule;
import com.bustrackpro.repository.finance.DelayPenaltyRuleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/finance/delay-rules")
@RequiredArgsConstructor
public class DelayPenaltyRuleController {

    private final DelayPenaltyRuleRepository delayPenaltyRuleRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_AUTHORITY_USER')")
    public ResponseEntity<List<DelayPenaltyRule>> getAllRules() {
        return ResponseEntity.ok(delayPenaltyRuleRepository.findAllByOrderByMinDelayMinutesDesc());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_AUTHORITY_USER')")
    public ResponseEntity<DelayPenaltyRule> createRule(@Valid @RequestBody DelayPenaltyRuleRequest request) {
        DelayPenaltyRule rule = DelayPenaltyRule.builder()
                .penaltyLevel(request.getPenaltyLevel())
                .minDelayMinutes(request.getMinDelayMinutes())
                .fineAmount(request.getFineAmount())
                .build();
        return ResponseEntity.ok(delayPenaltyRuleRepository.save(rule));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_AUTHORITY_USER')")
    public ResponseEntity<DelayPenaltyRule> updateRule(@PathVariable Integer id, @Valid @RequestBody DelayPenaltyRuleRequest request) {
        DelayPenaltyRule rule = delayPenaltyRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));
        
        rule.setPenaltyLevel(request.getPenaltyLevel());
        rule.setMinDelayMinutes(request.getMinDelayMinutes());
        rule.setFineAmount(request.getFineAmount());
        
        return ResponseEntity.ok(delayPenaltyRuleRepository.save(rule));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_AUTHORITY_USER')")
    public ResponseEntity<Void> deleteRule(@PathVariable Integer id) {
        delayPenaltyRuleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
