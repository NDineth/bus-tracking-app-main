package com.bustrackpro.service.tracking;

import com.bustrackpro.modal.tracking.AlertPolicy;
import com.bustrackpro.repository.tracking.AlertPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertPolicyService {
    private final AlertPolicyRepository repository;

    @Transactional(readOnly = true)
    public AlertPolicy getPolicy() {
        return repository.findById(1).orElseGet(AlertPolicy::new);
    }

    @Transactional
    public AlertPolicy updatePolicy(AlertPolicy policy) {
        policy.setId(1);
        return repository.save(policy);
    }
}
