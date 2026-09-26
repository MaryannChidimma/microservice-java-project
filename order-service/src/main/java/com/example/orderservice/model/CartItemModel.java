package com.example.orderservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_items")
public class CartItemModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private CartModel cart;

    // Owned by product-service, so only the id is stored here
    @Column(name = "product_id", nullable = false)
    private Long productId;

    private Integer quantity;

    public CartItemModel() {}
    public CartItemModel(CartModel cart, Long productId, Integer quantity) {
        this.cart = cart;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CartModel getCart() { return cart; }
    public void setCart(CartModel cart) { this.cart = cart; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
