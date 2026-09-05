package com.quas.payment.payment.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class TestEventConsumer {

    @Incoming("test-events-in")
    public void consume(String event) {
        System.out.println("Received event: " + event);
    }
}
