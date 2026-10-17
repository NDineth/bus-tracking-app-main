package com.bustrackpro.modal;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class NotificationRecipient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "notification_id")
    private Notification notification;

    private Integer receiverId;
    private boolean isRead = false;
}
