package com.example.orderservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderModel order;

    // Owned by product-service, so only the id is stored here
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Snapshot at purchase time so order history doesn't depend on product-service
    private String productName;

    private Integer quantity;
    private BigDecimal priceAtPurchase;

    public OrderItemModel() {}
    public OrderItemModel(OrderModel order, Long productId, String productName, Integer quantity, BigDecimal priceAtPurchase) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderModel getOrder() { return order; }
    public void setOrder(OrderModel order) { this.order = order; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(BigDecimal priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }
}
