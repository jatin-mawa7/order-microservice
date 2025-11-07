package com.example.orderservice.model;



public class OrderResponse {
    private Long orderId;
    private Long productId;
    private Double totalAmount;
    private String status;

    public OrderResponse() {}

    public OrderResponse(Long orderId, Long productId, String status, Double totalAmount) {
        this.orderId = orderId;
        this.productId = productId;
        this.status = status;
        this.totalAmount = totalAmount;
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
}
