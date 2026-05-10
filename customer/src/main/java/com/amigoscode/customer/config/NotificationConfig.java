package com.amigoscode.customer.config;

import com.amigoscode.customer.service.NotificationHttpService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class NotificationConfig {

    @Bean
    @LoadBalanced
    @Qualifier("notificationRestClientBuilder")
    RestClient.Builder notificationRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    @Bean
    NotificationHttpService notificationHttpService(@Qualifier("notificationRestClientBuilder") RestClient.Builder notificationRestClientBuilder) {
        RestClient restClient = notificationRestClientBuilder
                .baseUrl("http://NOTIFICATION")
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(NotificationHttpService.class);
    }
}

