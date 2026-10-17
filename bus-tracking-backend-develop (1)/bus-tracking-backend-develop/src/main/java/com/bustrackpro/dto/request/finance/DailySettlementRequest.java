package com.bustrackpro.dto.request.finance;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySettlementRequest {

    private Integer busId;
    private LocalDate date;
    private List<TripSegment> trips;

}
