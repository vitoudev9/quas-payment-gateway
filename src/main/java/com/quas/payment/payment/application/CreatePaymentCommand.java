package com.quas.payment.payment.application;

import java.math.BigDecimal;

public record CreatePaymentCommand(
        String merchantId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {
}
