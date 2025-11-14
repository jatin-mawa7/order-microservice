package com.example.orderservice.service;

import com.example.orderservice.client.PaymentClient;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.model.OrderRequest;
import com.example.orderservice.model.OrderResponse;
import com.example.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Service
@Slf4j
public class OrderService {

    //private static final Logger log = LoggerFactory.getLogger(OrderService.class);


    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;

    @Value("${product.discount.rate:0.2}")
    private double discountRate;
    //private static final String PRODUCT_SERVICE = "productService";
    //private static final String PAYMENT_SERVICE = "paymentService";
    private static final String ORDER_SERVICE = "orderService";

    public OrderService(OrderRepository orderRepository, ProductClient productClient, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
    }

//    // Product Service Call with CircuitBreaker + Retry
//    @CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "productAvailabilityFallback")
// @Retry(name = PRODUCT_SERVICE)
//    public boolean checkProductAvailability(Long productId, Integer quantity)
//    {
//        try {
//            return productClient.checkAvailability(productId, quantity);
//        } catch (feign.RetryableException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//    public boolean productAvailabilityFallback(Long productId, Integer quantity, Throwable ex) {
//        System.out.println("Fallback triggered for productId: " + productId + ", reason: " + ex.getMessage());
//        return false;
//    }
//    @CircuitBreaker(name = PRODUCT_SERVICE, fallbackMethod = "productPriceFallback")
//   @Retry(name = PRODUCT_SERVICE)
//    public double getProductPrice(Long productId) {
//        try {
//            return productClient.getProductPrice(productId);
//        } catch (feign.RetryableException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    public double productPriceFallback(Long productId, Throwable ex) {
//        System.out.println("Fallback triggered for getProductPrice for productId: " + productId + ", reason: " + ex.getMessage());
//        return 0.0; // fallback price
//    }
//
//    @CircuitBreaker(name = PAYMENT_SERVICE, fallbackMethod = "paymentServiceFallback")
//@Retry(name = PAYMENT_SERVICE)
//    public PaymentClient.PaymentResponse makePayment(Long orderId, double amount) {
//        try {
//            return paymentClient.processPayment(new PaymentClient.PaymentRequest(orderId, amount));
//        } catch (feign.RetryableException ex) {
//            throw new RuntimeException(ex);
//        }
//
//    }
//
//
//    public PaymentClient.PaymentResponse paymentServiceFallback(Long orderId, double amount, Throwable ex) {
//        System.out.println("Fallback triggered for payment, orderId: " + orderId + ", reason: " + ex.getMessage());
//        return new PaymentClient.PaymentResponse(0L, orderId, amount, "FAILED", "N/A");
//
//    }


    @CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "createOrderFallback")
    public OrderResponse createOrder(OrderRequest request) {
        // 1. Check product availability
        boolean available = checkProductAvailability(request.getProductId(), request.getQuantity());
        if (!available) {
            Order failedOrder = new Order();
            failedOrder.setProductId(request.getProductId());
            failedOrder.setQuantity(request.getQuantity());
            failedOrder.setTotalAmount(0.0);
            failedOrder.setStatus("FAILED - OUT_OF_STOCK");
            orderRepository.save(failedOrder);
            return new OrderResponse(failedOrder.getId(),failedOrder.getProductId(), failedOrder.getStatus(), failedOrder.getTotalAmount(), failedOrder.getPaymentMode());
        }

        //calculating the price using the product price and the discount rate
        double unitPrice = getProductPrice(request.getProductId());
        double total = unitPrice * request.getQuantity();
        total = total * (1.0 - discountRate);

        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(total);
        order.setStatus("CREATED");

        Order savedOrder = orderRepository.save(order);

        // 3. Process payment
        PaymentClient.PaymentResponse paymentResponse = makePayment(savedOrder.getId(), savedOrder.getTotalAmount());

        if(paymentResponse !=null && "SUCCESS".equalsIgnoreCase(paymentResponse.status()))
        {
            savedOrder.setStatus("PAID");
            savedOrder.setPaymentMode(paymentResponse.paymentMode());
            // IMPORTANT: reduce stock in product service (only on successful payment)
            productClient.reduceStock(savedOrder.getProductId(), savedOrder.getQuantity());
        } else {
            savedOrder.setStatus("FAILED - PAYMENT_ERROR");
        }
        savedOrder = orderRepository.save(savedOrder);

        return new OrderResponse(savedOrder.getId(),savedOrder.getProductId(), savedOrder.getStatus(), savedOrder.getTotalAmount(), savedOrder.getPaymentMode());
    }

    // ---- Fallback method ----
    public OrderResponse createOrderFallback(OrderRequest request, Throwable ex) {
        System.out.println("Fallback triggered for createOrder. Reason: {}" + ex.getMessage());
        Order failedOrder = new Order();
        failedOrder.setProductId(request.getProductId());
        failedOrder.setQuantity(request.getQuantity());
        failedOrder.setTotalAmount(0.0);
        failedOrder.setStatus("FAILED - SERVICE_UNAVAILABLE");
        orderRepository.save(failedOrder);
        return mapToResponse(failedOrder);
    }

    // ---- Helper methods ----
    private boolean checkProductAvailability(Long productId, Integer quantity) {
        return productClient.checkAvailability(productId, quantity);
    }

    private double getProductPrice(Long productId) {
        return productClient.getProductPrice(productId);
    }

    private PaymentClient.PaymentResponse makePayment(Long orderId, double amount) {
        return paymentClient.processPayment(new PaymentClient.PaymentRequest(orderId, amount));
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(order.getId(), order.getProductId(), order.getStatus(), order.getTotalAmount(), order.getPaymentMode());
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
