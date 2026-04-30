package com.coffeehut.coffeehut.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Data Transfer Object (DTO) representing the response from the HorsePay API.
 * <p>
 * This class maps the JSON response returned by the external HorsePay payment
 * gateway. It contains a nested {@link PaymentSuccess} object that indicates
 * whether the transaction was successful and provides additional details.
 * </p>
 */
@Data
public class HorsePayResponse {

    /**
     * Wrapper object containing payment success information.
     * <p>
     * Note: The JSON field name is mapped from {@code paymetSuccess}
     * (as defined by the external API, including its typo).
     * </p>
     */
    @JsonProperty("paymetSuccess")
    private PaymentSuccess paymentSuccess;

    /**
     * Nested DTO representing the success status of a payment or refund.
     * <p>
     * This class is used to deserialize the inner JSON object returned
     * by the HorsePay API, which includes a boolean status and an optional reason.
     * </p>
     */
    @Data
    public static class PaymentSuccess {

        /**
         * Indicates whether the transaction was successful.
         * <p>
         * Mapped from the JSON field {@code Status}.
         * A value of {@code true} means success, while {@code false} indicates failure.
         * </p>
         */
        @JsonProperty("Status")
        private Boolean status;

        /**
         * Additional message explaining the result of the transaction.
         * <p>
         * This field may contain error details or contextual information
         * when the transaction fails. It can be {@code null} if no message is provided.
         * </p>
         */
        private String reason;
    }
}
