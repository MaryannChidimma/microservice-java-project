package com.example.demo.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class CartModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id" , unique = true)
    private UserModel user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemModel> items = new ArrayList<>();

    public CartModel() {}
    public CartModel(UserModel user) { this.user = user; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserModel getUser() { return user; }
    public void setUser(UserModel user) { this.user = user; }
    public List<CartItemModel> getItems() { return items; }
    public void setItems(List<CartItemModel> items) { this.items = items; }
}