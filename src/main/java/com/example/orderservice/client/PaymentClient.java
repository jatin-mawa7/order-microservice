package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "http://localhost:8083") // Mock within same app
public interface PaymentClient {

    @PostMapping("/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest paymentRequest);

    record PaymentRequest(Long orderId, Double amount) {}

    record PaymentResponse(Long paymentId, Long orderId, Double amount, String status, String paymentMode) {}
}
