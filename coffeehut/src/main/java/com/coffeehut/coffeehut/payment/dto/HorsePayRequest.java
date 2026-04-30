package com.coffeehut.coffeehut.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Data Transfer Object (DTO) representing a request to the HorsePay API.
 * <p>
 * This class is used to construct the JSON payload sent to the external
 * HorsePay payment gateway for both payment and refund operations.
 * It includes transaction details such as customer ID, amount, date/time,
 * and configuration fields required by the API.
 * </p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HorsePayRequest {

    /**
     * Identifier of the store initiating the transaction.
     * <p>
     * This value is predefined as {@code "Team99"} and is required by the
     * HorsePay API to identify the merchant.
     * </p>
     */
    private String storeID = "Team99";

    /**
     * Unique identifier of the customer in the payment system.
     * <p>
     * This value is used by the payment gateway to associate the transaction
     * with a specific customer account.
     * </p>
     */
    private String customerID;

    /**
     * Transaction date in format <code>dd/MM/yyyy</code>.
     * <p>
     * This field is generated at runtime and represents the date when
     * the request is sent to the payment gateway.
     * </p>
     */
    private String date;

    /**
     * Transaction time in format <code>HH:mm</code>.
     * <p>
     * This field is generated at runtime and represents the time when
     * the request is sent to the payment gateway.
     * </p>
     */
    private String time;

    /**
     * Time zone of the transaction.
     * <p>
     * Default value is {@code "GMT"} as required by the HorsePay API.
     * </p>
     */
    private String timeZone = "GMT";

    /**
     * The monetary amount for the transaction.
     * <p>
     * Represents the amount to be charged or refunded.
     * Must be a positive value and should match the order total when applicable.
     * </p>
     */
    private Double transactionAmount;

    /**
     * Currency code of the transaction.
     * <p>
     * Default value is {@code "GBP"} (British Pound Sterling).
     * </p>
     */
    private String currencyCode = "GBP";

    /**
     * Flag to enforce the return type of payment status.
     * <p>
     * Mapped from JSON field {@code forcePaymentSatusReturnType}
     * (note the API-defined typo). When set, it may influence how
     * the gateway formats the response status.
     * </p>
     */
    @JsonProperty("forcePaymentSatusReturnType")
    private Boolean forcePaymentStatusReturnType;
}