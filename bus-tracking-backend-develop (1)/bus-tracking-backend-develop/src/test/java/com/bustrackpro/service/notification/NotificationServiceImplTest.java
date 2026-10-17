package com.bustrackpro.service.notification;

import com.bustrackpro.modal.Notification;
import com.bustrackpro.modal.NotificationRecipient;
import com.bustrackpro.repository.NotificationRecipientRepository;
import com.bustrackpro.repository.NotificationRepository;
import com.bustrackpro.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
    @Mock NotificationRepository notifications;
    @Mock NotificationRecipientRepository recipients;
    @Mock UserRepository users;
    @InjectMocks NotificationServiceImpl service;

    private NotificationRecipient recipient(int recipientId, int notificationId) {
        Notification n = new Notification(); n.setId(notificationId);
        NotificationRecipient r = new NotificationRecipient(); r.setId(recipientId); r.setNotification(n); r.setReceiverId(5);
        return r;
    }
    @Test void collidingRecipientIdCannotMarkTheWrongNotification() {
        var unrelated = recipient(7, 20);
        var intended = recipient(9, 7);
        when(recipients.findByReceiverId(5)).thenReturn(List.of(unrelated, intended));
        service.markAsRead(7, 5);
        assertFalse(unrelated.isRead());
        assertTrue(intended.isRead());
        verify(recipients).save(intended);
    }
    @Test void nonexistentNotificationIsNotSilentlyAcknowledged() {
        when(recipients.findByReceiverId(5)).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> service.markAsRead(7, 5));
    }
}
