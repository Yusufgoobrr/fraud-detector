package com.amigoscode.customer;

import com.amigoscode.amqp.RabbitMQMessageProducer;
import com.amigoscode.customer.service.FraudHttpService;
import com.amigoscode.customer.service.NotificationHttpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final FraudHttpService fraudHttpService;
    private final NotificationHttpService notificationHttpService;
    private final RabbitMQMessageProducer rabbitMQMessageProducer;

    public CustomerService(CustomerRepository customerRepository, FraudHttpService fraudHttpService, NotificationHttpService notificationHttpService, RabbitMQMessageProducer rabbitMQMessageProducer) {
        this.customerRepository = customerRepository;
        this.fraudHttpService = fraudHttpService;
        this.notificationHttpService = notificationHttpService;
        this.rabbitMQMessageProducer = rabbitMQMessageProducer;
    }

    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder().firstName(request.firstName()).lastName(request.lastName()).email(request.email()).build();
        log.info("customer initiated for insertion with this details: {}", request);
        customerRepository.save(customer);
        log.info("customer created successfully 🪅");
        FraudRequest result = fraudHttpService.isFraud(customer.getId());
        if (result.isFraudster()) {
            NotificationRequest notificationRequest = new NotificationRequest(customer.getId(), customer.getEmail(), "FRAUD CHECKER", "HE IS A FRAUD", LocalDateTime.now());
            rabbitMQMessageProducer.publish("internal.exchange", "internal.notification.routing-key", notificationRequest);
            customerRepository.delete(customer);
            log.info("customer was a fraud, so its deleted successfully 👯‍♂️");
        } else {
            log.info("customer wasn't a fraud, so its stays  ✅️");
        }
    }
}
