package com.bustrackpro.controller;

import com.bustrackpro.dto.response.NotificationResponseDTO;
import com.bustrackpro.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/receiver/{receiverId}")
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(@PathVariable Integer receiverId) {
        return ResponseEntity.ok(notificationService.getNotificationsByReceiverId(receiverId));
    }

    @GetMapping("/receiver/{receiverId}/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications(@PathVariable Integer receiverId) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsByReceiverId(receiverId));
    }

    @PatchMapping("/read/{notificationId}/receiver/{receiverId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer notificationId, @PathVariable Integer receiverId) {
        notificationService.markAsRead(notificationId, receiverId);
        return ResponseEntity.ok().build();
    }
}
