package com.bustrackpro.dto.request.finance;

import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSegment {

    private LocalTime time;
    private LocalTime endTime;
    private LocalTime expectedEndTime;
    private Double distanceKm;

}
