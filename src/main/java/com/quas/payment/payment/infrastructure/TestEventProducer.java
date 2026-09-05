package com.quas.payment.payment.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class TestEventProducer {

    @Inject
    @Channel("test-events-out")
    Emitter<String> emitter;

    public void send(String event) {
        emitter.send(event);
    }
}
