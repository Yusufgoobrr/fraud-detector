package com.amigoscode.customer.service;

import com.amigoscode.customer.NotificationRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface NotificationHttpService {
    @PostExchange("/api/v1/notification")
    void createNotification(@RequestBody NotificationRequest notificationRequest);
}
