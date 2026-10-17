package com.bustrackpro.modal;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String body;
    @Enumerated(EnumType.STRING)
    private NotificationCategory category;
    private String createdBy;
    private LocalDateTime createdAt;
    private Integer safetyScore;
    private String severity;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
