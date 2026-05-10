package com.amigoscode.customer.service;

import com.amigoscode.customer.FraudRequest;
import com.amigoscode.customer.NotificationRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface FraudHttpService {
    @GetExchange("api/v1/fraud-check/{id}")
    FraudRequest isFraud(@PathVariable("id") Integer id);



}
