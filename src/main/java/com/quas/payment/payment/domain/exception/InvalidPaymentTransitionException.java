package com.quas.payment.payment.domain.exception;

import com.quas.payment.payment.domain.enumz.PaymentStatus;

public class InvalidPaymentTransitionException extends RuntimeException {

    public InvalidPaymentTransitionException(PaymentStatus current, PaymentStatus requested) {

        super("Payment cannot transition from " + current + " to " + requested);
    }
}
