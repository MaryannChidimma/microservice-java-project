package com.example.orderservice.controller;

import com.example.orderservice.dto.CartDto;
import com.example.orderservice.exception.BadRequestException;
import com.example.orderservice.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartDto> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartDto> addItem(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        Object productId = body.get("productId");
        Object quantityValue = body.get("quantity");
        if (productId == null || productId.toString().isBlank() || quantityValue == null) {
            throw new BadRequestException("productId and quantity are required");
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityValue.toString());
        } catch (NumberFormatException e) {
            throw new BadRequestException("quantity must be a whole number");
        }
        if (quantity < 1) {
            throw new BadRequestException("quantity must be at least 1");
        }
        return ResponseEntity.ok(cartService.addItemToCart(userId, productId.toString(), quantity));
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(@PathVariable Long userId, @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(userId, itemId));
    }
}