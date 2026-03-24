package com.coffeehut.coffeehut.payment.Service;

import com.coffeehut.coffeehut.payment.dto.HorsePayRequest;
import com.coffeehut.coffeehut.payment.dto.HorsePayResponse;
import com.coffeehut.coffeehut.payment.dto.PaymentPayRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class PaymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${horsepay.api.url:http://homepages.cs.ncl.ac.uk/daniel.nesbitt/CSC8019/HorsePay/api/pay}")
    private String horsePayApiUrl;

    public HorsePayResponse processPayment(PaymentPayRequest request) {
        HorsePayRequest horsePayRequest = new HorsePayRequest();
        horsePayRequest.setCustomerID(request.getCustomerID());
        horsePayRequest.setTransactionAmount(request.getTransactionAmount());
        horsePayRequest.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        horsePayRequest.setTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<HorsePayRequest> entity = new HttpEntity<>(horsePayRequest, headers);

        ResponseEntity<HorsePayResponse> response = restTemplate.exchange(
                horsePayApiUrl,
                HttpMethod.POST,
                entity,
                HorsePayResponse.class
        );
        return response.getBody();
    }
}
