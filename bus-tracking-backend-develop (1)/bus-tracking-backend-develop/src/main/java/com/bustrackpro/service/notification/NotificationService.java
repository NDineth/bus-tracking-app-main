package com.bustrackpro.service.notification;

import com.bustrackpro.dto.response.NotificationResponseDTO;
import com.bustrackpro.modal.Notification;
import com.bustrackpro.modal.NotificationCategory;

import com.bustrackpro.modal.bus.Bus;

import java.util.List;

public interface NotificationService {
    Notification createNotification(String title, String body, NotificationCategory category, String createdBy, List<Integer> receiverIds);
    List<NotificationResponseDTO> getNotificationsByReceiverId(Integer receiverId);
    List<NotificationResponseDTO> getUnreadNotificationsByReceiverId(Integer receiverId);
    void markAsRead(Integer notificationId, Integer receiverId);
    void sendViolationNotification(Bus bus, String description);
}
