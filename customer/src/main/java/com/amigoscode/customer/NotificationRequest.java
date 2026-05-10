package com.amigoscode.customer;

import java.time.LocalDateTime;

public record NotificationRequest(Integer toCustomerId,
                                  String toCustomerEmail,
                                  String sender,
                                  String message,
                                  LocalDateTime sentAt) {
}
