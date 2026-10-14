package com.quas.payment.payment.application;

import com.quas.payment.payment.domain.model.Payment;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.util.UUID;

@ApplicationScoped
public class CreatePaymentUseCase {

    private final Clock clock;

    public CreatePaymentUseCase() {
        this.clock = Clock.systemUTC();
    }

    public Payment execute(CreatePaymentCommand command) {
        return new Payment(
                UUID.randomUUID(),
                command.merchantId(),
                command.amount(),
                command.currency(),
                command.idempotencyKey(),
                clock
        );
    }
}
