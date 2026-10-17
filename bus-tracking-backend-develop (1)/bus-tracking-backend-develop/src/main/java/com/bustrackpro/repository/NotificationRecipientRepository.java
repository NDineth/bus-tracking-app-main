package com.bustrackpro.repository;

import com.bustrackpro.modal.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {
    List<NotificationRecipient> findByReceiverId(Integer receiverId);
    List<NotificationRecipient> findByReceiverIdAndIsRead(Integer receiverId, boolean isRead);
}
