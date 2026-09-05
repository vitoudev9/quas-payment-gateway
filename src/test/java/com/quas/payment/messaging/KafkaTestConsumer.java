package com.quas.payment.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class KafkaTestConsumer {

    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

    @Incoming("test-events-in")
    public void consume(String message) {
        messages.offer(message);
    }

    public String awaitMessage() throws InterruptedException {
        return messages.take();
    }
}
