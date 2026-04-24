package com.coffeehut.coffeehut.payment.dto;

import lombok.Data;

@Data
public class PaymentRefundRequest {
    private String customerID;
    private Double transactionAmount;
    private String reference;
    private Long orderId;
}
