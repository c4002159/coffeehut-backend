package com.coffeehut.coffeehut.payment.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) representing a payment request.
 * <p>
 * This class encapsulates the information required to initiate a payment
 * operation through the payment service. It is typically received from
 * the client layer and passed to
 * {@link com.coffeehut.coffeehut.payment.Service.PaymentService}
 * for processing.
 * </p>
 */
@Data
public class PaymentPayRequest {

    /**
     * Unique identifier of the customer in the payment system.
     * <p>
     * This value is used by the external payment gateway to associate
     * the payment with a specific customer account.
     * </p>
     */
    private String customerID;

    /**
     * The amount to be charged for the transaction.
     * <p>
     * If an {@code orderId} is provided, this amount is expected to match
     * the total price of the corresponding order, with minor tolerance
     * for floating-point precision.
     * </p>
     */
    private Double transactionAmount;

    /**
     * Identifier of the associated order.
     * <p>
     * If provided, the system will validate the order status (must be
     * {@code pending}) and amount before processing the payment.
     * Can be {@code null} if the payment is not linked to a specific order.
     * </p>
     */
    private Long orderId;
}
