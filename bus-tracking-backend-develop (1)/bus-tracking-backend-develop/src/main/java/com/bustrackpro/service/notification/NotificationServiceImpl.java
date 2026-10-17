package com.bustrackpro.service.notification;

import com.bustrackpro.dto.response.NotificationResponseDTO;
import com.bustrackpro.modal.Notification;
import com.bustrackpro.modal.NotificationCategory;
import com.bustrackpro.modal.NotificationRecipient;
import com.bustrackpro.repository.NotificationRecipientRepository;
import com.bustrackpro.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.repository.user.UserRepository;
import com.bustrackpro.modal.user.UserRole;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Notification createNotification(String title,
                                           String body,
                                           NotificationCategory category,
                                           String createdBy,
                                           List<Integer> receiverIds) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setBody(body);
        notification.setCategory(category);
        notification.setCreatedBy(createdBy);
        Notification savedNotification = notificationRepository.save(notification);

        List<NotificationRecipient> recipients = receiverIds.stream().map(receiverId -> {
            NotificationRecipient recipient = new NotificationRecipient();
            recipient.setNotification(savedNotification);
            recipient.setReceiverId(receiverId);
            return recipient;
        }).collect(Collectors.toList());
        recipientRepository.saveAll(recipients);

        return savedNotification;
    }

    @Override
    public List<NotificationResponseDTO> getNotificationsByReceiverId(Integer receiverId) {
        List<NotificationResponseDTO> list = recipientRepository.findByReceiverId(receiverId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        int size = list.size();
        if (size > 20) {
            return list.subList(size - 20, size);
        }
        return list;
    }

    @Override
    public List<NotificationResponseDTO> getUnreadNotificationsByReceiverId(Integer receiverId) {
        List<NotificationResponseDTO> list = recipientRepository.findByReceiverIdAndIsRead(receiverId, false).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        int size = list.size();
        if (size > 20) {
            return list.subList(size - 20, size);
        }
        return list;
    }

    @Override
    public void markAsRead(Integer notificationId, Integer receiverId) {
        recipientRepository.findByReceiverId(receiverId).stream()
                .filter(r -> r.getNotification().getId().equals(notificationId) || r.getId().equals(notificationId))
                .findFirst()
                .ifPresent(recipient -> {
                    recipient.setRead(true);
                    recipientRepository.save(recipient);
                });
    }

    private NotificationResponseDTO convertToDto(NotificationRecipient recipient) {
        Notification notification = recipient.getNotification();
        return new NotificationResponseDTO(
                recipient.getId(),
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getCategory(),
                notification.getCreatedBy(),
                notification.getCreatedAt(),
                recipient.isRead()
        );
    }

    @Override
    @Transactional
    public void sendViolationNotification(Bus bus, String description) {
        if (bus == null) return;

        List<Integer> receiverIds = new java.util.ArrayList<>();
        
        // 1. Bus owner
        if (bus.getOwner() != null) {
            receiverIds.add(bus.getOwner().getId());
        }

        // 2. Admin users
        userRepository.findByRole(UserRole.ADMIN)
                .forEach(u -> receiverIds.add(u.getId()));

        // 3. Authority users
        userRepository.findByRole(UserRole.BUS_AUTHORITY_USER)
                .forEach(u -> receiverIds.add(u.getId()));

        // Deduplicate receiver IDs
        List<Integer> distinctReceiverIds = receiverIds.stream()
                .distinct()
                .collect(Collectors.toList());

        if (distinctReceiverIds.isEmpty()) return;

        createNotification(
                "Violation Alert: Bus " + bus.getBusNumber(),
                description,
                NotificationCategory.SYSTEM_ALERT,
                "SYSTEM",
                distinctReceiverIds
        );
    }
}
