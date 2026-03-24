package com.coffeehut.coffeehut.payment.dto;

import lombok.Data;

@Data
public class HorsePayResponse {
    private PaymentSuccess paymentSuccess;

    @Data
    public static class PaymentSuccess {
        private Boolean Status;
    }
}
