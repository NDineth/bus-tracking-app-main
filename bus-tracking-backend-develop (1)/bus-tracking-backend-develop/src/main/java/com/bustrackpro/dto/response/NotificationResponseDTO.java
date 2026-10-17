package com.bustrackpro.dto.response;

import com.bustrackpro.modal.NotificationCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Integer id;
    private Integer notificationId;
    private String title;
    private String body;
    private NotificationCategory category;
    private String createdBy;
    private LocalDateTime createdAt;

    @JsonProperty("isRead")
    private boolean isRead;
}
