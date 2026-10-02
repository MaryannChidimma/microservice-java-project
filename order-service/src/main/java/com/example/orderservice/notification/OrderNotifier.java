package com.example.orderservice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

// Calls the serverless notify function after an order is committed.
// Fire-and-forget: a cold start or a failure never slows or fails checkout.
@Component
public class OrderNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrderNotifier.class);

    private final String notifyUrl;
    private final RestClient restClient;

    public OrderNotifier(@Value("${notifications.url:}") String notifyUrl) {
        this.notifyUrl = notifyUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        // Generous: the function may be starting from zero
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        if (notifyUrl.isBlank()) {
            return;
        }
        try {
            restClient.post()
                    .uri(notifyUrl + "/notify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "event", "OrderPlaced",
                            "orderId", event.orderId(),
                            "userId", event.userId(),
                            "total", event.total().toPlainString(),
                            "placedAt", event.placedAt().toString()))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notification sent for order {}", event.orderId());
        } catch (Exception e) {
            log.warn("Notification for order {} failed: {}", event.orderId(), e.getMessage());
        }
    }
}
