package com.bustrackpro.dto.request.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccelerationReadingDto {
    private Double x;
    private Double y;
    private Double z;
}
