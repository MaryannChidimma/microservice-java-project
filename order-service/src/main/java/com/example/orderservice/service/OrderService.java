package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.dto.OrderItemDto;
import com.example.orderservice.dto.ProductDto;
import com.example.orderservice.dto.UserDto;
import com.example.orderservice.exception.BadRequestException;
import com.example.orderservice.exception.DownstreamUnavailableException;
import com.example.orderservice.exception.NotFoundException;
import com.example.orderservice.model.CartItemModel;
import com.example.orderservice.model.CartModel;
import com.example.orderservice.model.OrderItemModel;
import com.example.orderservice.model.OrderModel;
import com.example.orderservice.notification.OrderPlacedEvent;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.repository.OrderRepository;
import feign.FeignException;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        ProductClient productClient, UserClient userClient,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productClient = productClient;
        this.userClient = userClient;
        this.eventPublisher = eventPublisher;
    }

    private ProductDto fetchProduct(String productId) {
        ProductDto product;
        try {
            product = productClient.getProductById(productId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Product not found: " + productId);
        } catch (FeignException e) {
            throw new DownstreamUnavailableException("Product service unavailable", e);
        }
        if (product == null) {
            throw new NotFoundException("Product not found: " + productId);
        }
        return product;
    }

    private UserDto fetchUser(Long userId) {
        UserDto user;
        try {
            user = userClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found: " + userId);
        } catch (FeignException e) {
            throw new DownstreamUnavailableException("User service unavailable", e);
        }
        if (user == null) {
            throw new NotFoundException("User not found: " + userId);
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
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place an order with an empty cart");
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

        // Delivered to OrderNotifier only after this transaction commits
        eventPublisher.publishEvent(new OrderPlacedEvent(saved.getId(), userId, total, saved.getOrderDate()));

        return toDto(saved);
    }

    public OrderDto getOrderById(Long id) {
        return orderRepository.findById(id).map(this::toDto).orElse(null);
    }

    public List<OrderDto> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
    }
}
