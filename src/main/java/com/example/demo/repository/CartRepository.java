package com.example.demo.repository;

import com.example.demo.model.CartModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartModel, Long> {
    Optional<CartModel> findByUser_Id(Long userId);
}