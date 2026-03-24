package com.coffeehut.coffeehut.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HorsePayRequest {
    private String storeID = "Team99";
    private String customerID;
    private String date;
    private String time;
    private String timeZone = "GMT";
    private Double transactionAmount;
    private String currencyCode = "GBP";

    @JsonProperty("forcePaymentSatusReturnType")
    private Boolean forcePaymentStatusReturnType;
}
