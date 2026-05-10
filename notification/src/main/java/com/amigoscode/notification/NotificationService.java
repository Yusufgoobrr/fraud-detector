package com.amigoscode.notification;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public void send(NotificationRequest notificationRequest) {
        // Notification notification = new Notification(notificationRequest.toCustomerId(), notificationRequest.toCustomerEmail(), notificationRequest.sender(), notificationRequest.message(), LocalDateTime.now());
        Notification notification = Notification.builder().toCustomerId(notificationRequest.toCustomerId()).toCustomerEmail(notificationRequest.toCustomerEmail()).sender(notificationRequest.sender()).message(notificationRequest.message()).sentAt(LocalDateTime.now()).build();
        notificationRepository.save(notification);
    }
}
