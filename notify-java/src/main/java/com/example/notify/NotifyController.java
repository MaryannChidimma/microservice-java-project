package com.example.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotifyController {

    private static final Logger log = LoggerFactory.getLogger(NotifyController.class);

    @PostMapping("/notify")
    public ResponseEntity<Void> notify(@RequestBody(required = false) String body) {
        log.info("notification received: {}", body);
        return ResponseEntity.ok().build();
    }
}
