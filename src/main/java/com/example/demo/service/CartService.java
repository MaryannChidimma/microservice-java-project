package com.example.demo.service;

import com.example.demo.dto.CartDto;
import com.example.demo.dto.CartItemDto;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    private CartDto toDto(CartModel cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(i -> new CartItemDto(i.getId(), i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getProduct().getPrice()))
                .collect(Collectors.toList());

        BigDecimal total = itemDtos.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(cart.getId(), cart.getUser().getId(), itemDtos, total);
    }

    private CartModel getOrCreateCart(Long userId) {
        return cartRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    UserModel user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return cartRepository.save(new CartModel(user));
                });
    }

    public CartDto getCartByUserId(Long userId) {
        return toDto(getOrCreateCart(userId));
    }

    public CartDto addItemToCart(Long userId, Long productId, Integer quantity) {
        CartModel cart = getOrCreateCart(userId);
        ProductModel product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + quantity),
                        () -> cart.getItems().add(new CartItemModel(cart, product, quantity))
                );

        return toDto(cartRepository.save(cart));
    }

    public CartDto removeItemFromCart(Long userId, Long itemId) {
        CartModel cart = getOrCreateCart(userId);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        return toDto(cartRepository.save(cart));
    }
}