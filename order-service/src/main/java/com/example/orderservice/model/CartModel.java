package com.example.orderservice.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class CartModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owned by user-service, so only the id is stored here
    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemModel> items = new ArrayList<>();

    public CartModel() {}
    public CartModel(Long userId) { this.userId = userId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<CartItemModel> getItems() { return items; }
    public void setItems(List<CartItemModel> items) { this.items = items; }
}
