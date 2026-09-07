package com.quas.payment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.f4b6a3.uuid.UuidCreator;
import com.quas.payment.payment.domain.Payment;
import com.quas.payment.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void shouldCreatePaymentWithCreatedStatus() {
        Payment payment = new Payment(
                UuidCreator.getTimeOrderedEpochPlus1(),
                "merchant-123",
                new BigDecimal("100.00"),
                "USD",
                "idempotency-key-123");
        assertEquals(PaymentStatus.CREATED, payment.getStatus());
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        Payment payment = createPayment();
        assertThrows(IllegalStateException.class, payment::succeed);
    }

    @Test
    void shouldStatusMoveThroughValidTransitions() {
        Payment payment = createPayment();

        payment.moveToPending();
        assertEquals(PaymentStatus.PENDING, payment.getStatus());

        payment.startProcessing();
        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());

        payment.succeed();
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());

        payment.refund();
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    private Payment createPayment() {
        return new Payment(
                UuidCreator.getTimeOrderedEpochPlus1(),
                "merchant-123",
                new BigDecimal("100.00"),
                "USD",
                "idempotency-key-123");
    }
}
