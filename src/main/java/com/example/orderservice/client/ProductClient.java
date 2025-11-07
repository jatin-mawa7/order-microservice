package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8080") // Mock within same app

public interface ProductClient {

    @GetMapping("/products/check/{id}")
    boolean checkAvailability(@PathVariable("id") Long productId);

//    @GetMapping("/products/price/{productId}")
//    double getProductPrice(@PathVariable("productId") Long productId);
}
