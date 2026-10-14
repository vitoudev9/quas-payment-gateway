package com.quas.payment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.f4b6a3.uuid.UuidCreator;
import com.quas.payment.payment.domain.Payment;
import com.quas.payment.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void shouldCreatePaymentWithCreatedStatus() {
        Payment payment = new Payment(
                UuidCreator.getTimeOrderedEpochPlus1(),
                "merchant-123",
                new BigDecimal("100.00"),
                "USD",
                "idempotency-key-123",
                Clock.systemUTC());
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

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        "merchant-001",
                        new BigDecimal("0.00"),
                        "USD",
                        "idempotency-key-123",
                        Clock.systemUTC()));
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        "merchant-002",
                        new BigDecimal("-5"),
                        "USD",
                        "idempotency-key-123",
                        Clock.systemUTC()));
    }

    @Test
    void shouldRejectAmountWithExcessPrecision() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        "merchant-003",
                        new BigDecimal("10,003"),
                        "USD",
                        "idempotency-key-123",
                        Clock.systemUTC()));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        "merchant-003",
                        new BigDecimal("4000,01"),
                        "KHR",
                        "idempotency-key-123",
                        Clock.systemUTC()));
    }

    @Test
    void shouldRejectUnsupportedCurrency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        "merchant-001",
                        new BigDecimal("10,00"),
                        "SGD",
                        "idempotency-key-123",
                        Clock.systemUTC()));
    }

    @Test
    void shouldRejectBlankMerchantId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        "",
                        new BigDecimal("10,00"),
                        "USD",
                        "idempotency-key-123",
                        Clock.systemUTC()));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        null,
                        new BigDecimal("10,00"),
                        "USD",
                        "idempotency-key-123",
                        Clock.systemUTC()));
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(), "merchant-001", new BigDecimal("10,00"), "USD", "", Clock.systemUTC()));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(UUID.randomUUID(), "", new BigDecimal("10,00"), "USD", null, Clock.systemUTC()));
    }

    private Payment createPayment() {
        return new Payment(
                UuidCreator.getTimeOrderedEpochPlus1(),
                "merchant-123",
                new BigDecimal("100.00"),
                "USD",
                "idempotency-key-123",
                Clock.systemUTC());
    }
}
