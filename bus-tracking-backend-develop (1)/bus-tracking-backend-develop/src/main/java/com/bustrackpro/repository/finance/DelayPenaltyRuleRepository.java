package com.bustrackpro.repository.finance;

import com.bustrackpro.modal.finance.DelayPenaltyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DelayPenaltyRuleRepository extends JpaRepository<DelayPenaltyRule, Integer> {
    List<DelayPenaltyRule> findAllByOrderByMinDelayMinutesDesc();
}
