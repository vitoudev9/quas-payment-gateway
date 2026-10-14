package com.quas.payment.payment.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final String merchantId;
    private final BigDecimal amount;
    private final String currency;
    private final String idempotencyKey;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Clock clock;

    public Payment(UUID id, String merchantId, BigDecimal amount, String currency, String idempotencyKey, Clock clock) {

        this.id = Objects.requireNonNull(id, "ID must not be null");

        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("Merchant ID must not be blank");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        PaymentCurrency paymentCurrency = PaymentCurrency.fromCode(currency);

        if (amount.scale() > paymentCurrency.fractionDigits()) {
            throw new IllegalArgumentException("Payment amount has too many decimal places for " + currency);
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency Key must not be blank");
        }

        Instant now = Instant.now(clock);

        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.CREATED;
        this.createdAt = now;
        this.updatedAt = now;
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void moveToPending() {
        requireStatus(PaymentStatus.CREATED);
        status = PaymentStatus.PENDING;
        touch();
    }

    public void startProcessing() {
        requireStatus(PaymentStatus.PENDING);
        status = PaymentStatus.PROCESSING;
        touch();
    }

    public void succeed() {
        requireStatus(PaymentStatus.PROCESSING);
        status = PaymentStatus.SUCCEEDED;
        touch();
    }

    public void fail() {
        requireStatus(PaymentStatus.PROCESSING);
        status = PaymentStatus.FAILED;
        touch();
    }

    public void refund() {
        requireStatus(PaymentStatus.SUCCEEDED);
        status = PaymentStatus.REFUNDED;
        touch();
    }

    private void requireStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Payment cannot transition from " + status + " to " + expected);
        }
    }

    private void touch() {
        updatedAt = Instant.now(clock);
    }
}
