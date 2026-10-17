package com.bustrackpro.service.task;

import com.bustrackpro.modal.NotificationCategory;
import com.bustrackpro.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final NotificationService notificationService;

    public void completeTask(Long taskId, String completedBy) {
        // Notify relevant users
        String title = "Task Completed";
        String body = "Task with ID " + taskId + " has been completed by " + completedBy;
        List<Integer> receiverIds = Arrays.asList(1, 2, 3);
        notificationService.createNotification(title, body, NotificationCategory.USER_UPDATE, completedBy, receiverIds);
    }
}
