package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.CartDto;
import com.example.orderservice.dto.CartItemDto;
import com.example.orderservice.dto.ProductDto;
import com.example.orderservice.dto.UserDto;
import com.example.orderservice.model.CartItemModel;
import com.example.orderservice.model.CartModel;
import com.example.orderservice.repository.CartRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final UserClient userClient;

    public CartService(CartRepository cartRepository, ProductClient productClient, UserClient userClient) {
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

    private CartDto toDto(CartModel cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(i -> {
                    ProductDto product = fetchProduct(i.getProductId());
                    return new CartItemDto(i.getId(), i.getProductId(), product.getName(),
                            i.getQuantity(), product.getPrice());
                })
                .collect(Collectors.toList());

        BigDecimal total = itemDtos.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(cart.getId(), cart.getUserId(), itemDtos, total);
    }

    private CartModel getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    fetchUser(userId);
                    return cartRepository.save(new CartModel(userId));
                });
    }

    public CartDto getCartByUserId(Long userId) {
        return toDto(getOrCreateCart(userId));
    }

    public CartDto addItemToCart(Long userId, Long productId, Integer quantity) {
        CartModel cart = getOrCreateCart(userId);
        fetchProduct(productId);

        cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + quantity),
                        () -> cart.getItems().add(new CartItemModel(cart, productId, quantity))
                );

        return toDto(cartRepository.save(cart));
    }

    public CartDto removeItemFromCart(Long userId, Long itemId) {
        CartModel cart = getOrCreateCart(userId);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        return toDto(cartRepository.save(cart));
    }
}
