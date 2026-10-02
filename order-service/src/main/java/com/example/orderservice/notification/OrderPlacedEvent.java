package com.example.orderservice.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPlacedEvent(Long orderId, Long userId, BigDecimal total, LocalDateTime placedAt) {
}
