package com.bustrackpro.repository.finance;

import com.bustrackpro.modal.finance.FinanceRateCard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the finance rate card.
 * Always reads the latest entry (highest ID / most recently created).
 */
@Repository
public interface FinanceRateCardRepository extends JpaRepository<FinanceRateCard, Integer> {

    /**
     * Returns the most recently created rate card.
     *
     * @return optional rate card
     */
    Optional<FinanceRateCard> findTopByOrderByCreatedAtDesc();
}
