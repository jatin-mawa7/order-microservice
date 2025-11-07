package com.example.orderservice.service;

import com.example.orderservice.client.PaymentClient;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.model.OrderRequest;
import com.example.orderservice.model.OrderResponse;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
    }

    public OrderResponse createOrder(OrderRequest request) {
        // 1. Check product availability
        boolean available = productClient.checkAvailability(request.getProductId());
        if (!available) {
            Order failedOrder = new Order();
            failedOrder.setProductId(request.getProductId());
            failedOrder.setQuantity(request.getQuantity());
            failedOrder.setTotalAmount(request.getTotalAmount());
            failedOrder.setStatus("FAILED - OUT_OF_STOCK");
            orderRepository.save(failedOrder);
            return new OrderResponse(failedOrder.getId(),failedOrder.getProductId(), failedOrder.getStatus(), failedOrder.getTotalAmount());
        }

        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus("CREATED");

        Order savedOrder = orderRepository.save(order);

        // 3. Process payment
        PaymentClient.PaymentResponse paymentResponse = paymentClient.processPayment(
                new PaymentClient.PaymentRequest(savedOrder.getId(), savedOrder.getTotalAmount())
        );

        savedOrder.setStatus(paymentResponse.status().equalsIgnoreCase("SUCCESS")
                ? "PAID"
                : "FAILED - PAYMENT ERROR");

        orderRepository.save(savedOrder);

        return new OrderResponse(savedOrder.getId(),savedOrder.getProductId(), savedOrder.getStatus(), savedOrder.getTotalAmount());
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
