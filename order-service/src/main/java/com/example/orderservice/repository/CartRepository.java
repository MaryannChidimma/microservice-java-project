package com.example.orderservice.repository;

import com.example.orderservice.model.CartModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartModel, Long> {
    Optional<CartModel> findByUserId(Long userId);
}
