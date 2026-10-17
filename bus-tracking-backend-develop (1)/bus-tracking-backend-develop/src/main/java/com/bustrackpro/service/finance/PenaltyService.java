package com.bustrackpro.service.finance;

import com.bustrackpro.dto.request.finance.PenaltyRequest;
import com.bustrackpro.dto.response.finance.PenaltyRecordResponse;

import java.util.List;

public interface PenaltyService {
    PenaltyRecordResponse applyPenalty(PenaltyRequest request, Integer issuedById);
    List<PenaltyRecordResponse> getAllPenalties();
}
