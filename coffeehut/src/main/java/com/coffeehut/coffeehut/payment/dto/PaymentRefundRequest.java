package com.coffeehut.coffeehut.payment.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) representing a refund request.
 * <p>
 * This class encapsulates the information required to initiate a refund
 * operation through the payment service. It is typically received from
 * the client layer and passed to {@link com.coffeehut.coffeehut.payment.Service.PaymentService}
 * for processing.
 * </p>
 */
@Data
public class PaymentRefundRequest {

    /**
     * Unique identifier of the customer in the payment system.
     * <p>
     * This value is used by the external payment gateway to identify
     * the account associated with the refund.
     * </p>
     */
    private String customerID;

    /**
     * The amount to be refunded.
     * <p>
     * This should match the original transaction amount of the order.
     * Minor floating-point differences may be tolerated during validation.
     * </p>
     */
    private Double transactionAmount;

    /**
     * Reference identifier for the refund transaction.
     * <p>
     * This may correspond to the original payment reference or a
     * system-generated identifier used for tracking purposes.
     * </p>
     */
    private String reference;

    /**
     * Identifier of the associated order.
     * <p>
     * If provided, the system will validate the order status and amount
     * before processing the refund. Can be {@code null} if the refund
     * is not tied to a specific order.
     * </p>
     */
    private Long orderId;
}