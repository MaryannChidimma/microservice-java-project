package com.example.orderservice.repository;

import com.example.orderservice.model.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderModel, Long> {
    List<OrderModel> findByUserId(Long userId);
}
