package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "product-service", url = "http://localhost:8080") // Mock within same app

public interface ProductClient {

    //to check the product is there or not
    @GetMapping("/products/check/{id}")
    boolean checkAvailability(@PathVariable("id") Long productId);

    //to check if the product at least has that quantity or not
    @GetMapping("/products/check/{productId}/{quantity}")
    boolean checkAvailability(@PathVariable("productId") Long productId, @PathVariable("quantity") Integer quantity);

    // get product price
    @GetMapping("/products/price/{productId}")
    double getProductPrice(@PathVariable("productId") Long productId);

    // reduce stock after successful payment
    @PutMapping("/products/reduce/{productId}/{quantity}")
    void reduceStock(@PathVariable("productId") Long productId, @PathVariable("quantity") Integer quantity);
}
