package com.example.demo.model;

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

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductModel product;

    private Integer quantity;
    private BigDecimal priceAtPurchase;

    public OrderItemModel() {}
    public OrderItemModel(OrderModel order, ProductModel product, Integer quantity, BigDecimal priceAtPurchase) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderModel getOrder() { return order; }
    public void setOrder(OrderModel order) { this.order = order; }
    public ProductModel getProduct() { return product; }
    public void setProduct(ProductModel product) { this.product = product; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(BigDecimal priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }
}