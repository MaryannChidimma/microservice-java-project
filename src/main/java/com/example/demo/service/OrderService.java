package com.example.demo.service;

import com.example.demo.dto.OrderDto;
import com.example.demo.dto.OrderItemDto;
import com.example.demo.model.*;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
    }

    private OrderDto toDto(OrderModel order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(i -> new OrderItemDto(i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getPriceAtPurchase()))
                .collect(Collectors.toList());

        return new OrderDto(order.getId(), order.getUser().getId(), itemDtos,
                order.getTotalAmount(), order.getStatus(), order.getOrderDate());
    }

    public OrderDto placeOrderFromCart(Long userId) {
        CartModel cart = cartRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place an order with an empty cart");
        }

        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderModel order = new OrderModel(cart.getUser(), total);
        List<OrderItemModel> orderItems = cart.getItems().stream()
                .map(ci -> new OrderItemModel(order, ci.getProduct(), ci.getQuantity(), ci.getProduct().getPrice()))
                .collect(Collectors.toList());
        order.setItems(orderItems);

        OrderModel saved = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return toDto(saved);
    }

    public OrderDto getOrderById(Long id) {
        return orderRepository.findById(id).map(this::toDto).orElse(null);
    }

    public List<OrderDto> getOrdersByUser(Long userId) {
        return orderRepository.findByUser_Id(userId).stream().map(this::toDto).collect(Collectors.toList());
    }
}