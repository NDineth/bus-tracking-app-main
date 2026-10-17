package com.bustrackpro.dto.request.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for receiving timetable creation/update requests.
 */
@Data
public class TimetableRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long routeId;

    @NotBlank
    private String dayType;

    @NotNull
    private String activeDate;

    @NotNull
    private String expirationDate;

    @NotBlank
    private String scheduleType;

    private Integer duplicateFromTimetableId;
}
