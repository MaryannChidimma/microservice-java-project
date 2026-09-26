package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.dto.OrderItemDto;
import com.example.orderservice.dto.ProductDto;
import com.example.orderservice.dto.UserDto;
import com.example.orderservice.model.CartItemModel;
import com.example.orderservice.model.CartModel;
import com.example.orderservice.model.OrderItemModel;
import com.example.orderservice.model.OrderModel;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.repository.OrderRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final UserClient userClient;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        ProductClient productClient, UserClient userClient) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productClient = productClient;
        this.userClient = userClient;
    }

    private ProductDto fetchProduct(Long productId) {
        ProductDto product;
        try {
            product = productClient.getProductById(productId);
        } catch (FeignException.NotFound e) {
            throw new RuntimeException("Product not found: " + productId);
        } catch (FeignException e) {
            throw new RuntimeException("Product service unavailable", e);
        }
        if (product == null) {
            throw new RuntimeException("Product not found: " + productId);
        }
        return product;
    }

    private UserDto fetchUser(Long userId) {
        UserDto user;
        try {
            user = userClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new RuntimeException("User not found: " + userId);
        } catch (FeignException e) {
            throw new RuntimeException("User service unavailable", e);
        }
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        return user;
    }

    private OrderDto toDto(OrderModel order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(i -> new OrderItemDto(i.getProductId(), i.getProductName(),
                        i.getQuantity(), i.getPriceAtPurchase()))
                .collect(Collectors.toList());

        return new OrderDto(order.getId(), order.getUserId(), itemDtos,
                order.getTotalAmount(), order.getStatus(), order.getOrderDate());
    }

    @Transactional
    public OrderDto placeOrderFromCart(Long userId) {
        fetchUser(userId);

        CartModel cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place an order with an empty cart");
        }

        OrderModel order = new OrderModel(userId, BigDecimal.ZERO);
        List<OrderItemModel> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Price every line from product-service at the moment of purchase
        for (CartItemModel ci : cart.getItems()) {
            ProductDto product = fetchProduct(ci.getProductId());
            orderItems.add(new OrderItemModel(order, ci.getProductId(), product.getName(),
                    ci.getQuantity(), product.getPrice()));
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        order.setItems(orderItems);
        order.setTotalAmount(total);

        OrderModel saved = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return toDto(saved);
    }

    public OrderDto getOrderById(Long id) {
        return orderRepository.findById(id).map(this::toDto).orElse(null);
    }

    public List<OrderDto> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
    }
}
