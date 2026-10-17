package com.bustrackpro.repository.approval;

import com.bustrackpro.modal.approval.ApprovalRequest;
import com.bustrackpro.modal.approval.ApprovalStatus;
import com.bustrackpro.modal.approval.ApprovalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ApprovalRequest entity.
 */
@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Integer> {

    /**
     * Find history of approval requests for a specific user.
     *
     * @param userId - ID of the requesting user
     * @return List of approval requests
     */
    List<ApprovalRequest> findByRequestedById(Integer userId);

    /**
     * Find approval requests filtered optionally by status and/or approval type.
     *
     * @param status - the optional status filter
     * @param approvalType - the optional approval type filter
     * @return List of matching approval requests
     */
    @Query("SELECT a FROM ApprovalRequest a WHERE "
           + "(:status IS NULL OR a.status = :status) AND "
           + "(:approvalType IS NULL OR a.approvalType = :approvalType) "
           + "ORDER BY a.createdAt DESC")
    List<ApprovalRequest> findByStatusAndApprovalTypeOptional(
            @Param("status") ApprovalStatus status,
            @Param("approvalType") ApprovalType approvalType);

    List<ApprovalRequest> findByStatus(ApprovalStatus status);

    List<ApprovalRequest> findByBusIdAndStatus(Integer busId, ApprovalStatus status);
}
