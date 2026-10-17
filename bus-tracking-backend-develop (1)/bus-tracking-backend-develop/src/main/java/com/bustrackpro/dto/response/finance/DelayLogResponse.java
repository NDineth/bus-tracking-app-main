package com.bustrackpro.dto.response.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayLogResponse {

    private String id;
    private String busNumber;
    private String route;
    private Integer scheduledDurationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String date;
}
