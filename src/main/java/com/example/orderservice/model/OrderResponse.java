package com.example.orderservice.model;



public class OrderResponse {
    private Long orderId;
    private Long productId;
    private Double totalAmount;
    private String status;
    private String paymentMode;

    public OrderResponse() {}

    public OrderResponse(Long orderId, Long productId, String status, Double totalAmount, String paymentMode) {
        this.orderId = orderId;
        this.productId = productId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.paymentMode = paymentMode;
    }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }
}
