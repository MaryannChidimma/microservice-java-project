package com.example.demo.model;

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

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductModel product;

    private Integer quantity;

    public CartItemModel() {}
    public CartItemModel(CartModel cart, ProductModel product, Integer quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CartModel getCart() { return cart; }
    public void setCart(CartModel cart) { this.cart = cart; }
    public ProductModel getProduct() { return product; }
    public void setProduct(ProductModel product) { this.product = product; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}