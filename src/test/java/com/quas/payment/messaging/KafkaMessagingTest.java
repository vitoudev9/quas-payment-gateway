package com.quas.payment.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.quas.payment.payment.infrastructure.TestEventProducer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class KafkaMessagingTest {

    @Inject
    TestEventProducer producer;

    @Inject
    KafkaTestConsumer consumer;

    @Test
    void shouldSendAndReceiveMessage() throws InterruptedException {
        String message = "Hello from Quas Payment!";
        producer.send(message);
        String receivedMessage = consumer.awaitMessage();
        assertEquals(message, receivedMessage);
    }
}
