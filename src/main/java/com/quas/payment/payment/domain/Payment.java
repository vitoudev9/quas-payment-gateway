package com.quas.payment.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
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

    public Payment(UUID id, String merchantId, BigDecimal amount, String currency, String idempotencyKey) {
        this.id = id;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
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
        updatedAt = Instant.now();
    }
}
