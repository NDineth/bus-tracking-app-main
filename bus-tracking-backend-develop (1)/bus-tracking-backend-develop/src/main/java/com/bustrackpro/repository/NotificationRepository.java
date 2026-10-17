package com.bustrackpro.repository;

import com.bustrackpro.modal.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
