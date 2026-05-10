package com.amigoscode.fraud;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/fraud-check")
@AllArgsConstructor
@Slf4j
public class FraudController {
    private FraudCheckService fraudCheckService;

    @GetMapping("/{id}")
    public FraudCheckResponse isFraudster(@PathVariable("id") Integer id) {
        log.info("Initiated a fraud check on customer with id: {}", id);
        FraudCheckResponse fradulentCustomer = fraudCheckService.isFradulentCustomer(id);
        log.info("Completed a fraud check on customer with id: {}", id);
        return new FraudCheckResponse(fradulentCustomer.isFraudster());
    }

    ;

}
