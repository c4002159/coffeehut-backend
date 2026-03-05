package com.coffeehut.coffeehut.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            LocalDateTime now = LocalDateTime.now();
            String date = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));

            Map<String, Object> horsePayRequest = new HashMap<>();
            horsePayRequest.put("storeID", "Team09");
            horsePayRequest.put("customerID", request.get("customerID"));
            horsePayRequest.put("date", date);
            horsePayRequest.put("time", time);
            horsePayRequest.put("timeZone", "GMT");
            horsePayRequest.put("transactionAmount", request.get("transactionAmount"));
            horsePayRequest.put("currencyCode", "GBP");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(horsePayRequest, headers);

            String url = "http://homepages.cs.ncl.ac.uk/daniel.nesbitt/CSC8019/HorsePay/HorsePay.php";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
