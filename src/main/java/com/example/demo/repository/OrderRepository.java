package com.example.demo.repository;

import com.example.demo.model.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderModel, Long> {
    List<OrderModel> findByUser_Id(Long userId);
}