package com.example.orderservice.dto;

import java.math.BigDecimal;

public class OrderItemDto {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal priceAtPurchase;

    public OrderItemDto() {}
    public OrderItemDto(String productId, String productName, Integer quantity, BigDecimal priceAtPurchase) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(BigDecimal priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }
}