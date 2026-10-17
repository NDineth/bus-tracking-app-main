package com.bustrackpro.dto.response.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending timetable responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableResponse {

    private Integer id;
    private String name;
    private Long routeId;
    private String routeNumber;
    private String routeStartLocation;
    private String routeEndLocation;
    private String dayType;
    private String scheduleType;
    private String activeDate;
    private String expirationDate;
    private int tripCount;
    private String createdAt;
}
