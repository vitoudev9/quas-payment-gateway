package com.quas.payment.payment.domain.enumz;

import java.util.Arrays;

public enum PaymentCurrency {
    USD(2),
    KHR(0);

    private final int fractionDigits;

    PaymentCurrency(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public int fractionDigits() {
        return fractionDigits;
    }

    public static PaymentCurrency fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }

        return Arrays.stream(values())
                .filter(currency -> currency.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency " + code));
    }
}
