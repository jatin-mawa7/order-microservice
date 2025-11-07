package com.example.orderservice.service;

import com.example.orderservice.client.PaymentClient;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.model.OrderRequest;
import com.example.orderservice.model.OrderResponse;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;

    @Value("${product.discount.rate:0.2}")
    private double discountRate;

    public OrderService(OrderRepository orderRepository, ProductClient productClient, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
    }

    public OrderResponse createOrder(OrderRequest request) {
        // 1. Check product availability
        boolean available = productClient.checkAvailability(request.getProductId(), request.getQuantity());
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
        double unitPrice = productClient.getProductPrice(request.getProductId());
        double total = unitPrice * request.getQuantity();
        total = total * (1.0 - discountRate);

        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(total);
        order.setStatus("CREATED");

        Order savedOrder = orderRepository.save(order);

        // 3. Process payment
        PaymentClient.PaymentResponse paymentResponse = paymentClient.processPayment(
                new PaymentClient.PaymentRequest(savedOrder.getId(), savedOrder.getTotalAmount())
        );


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

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
