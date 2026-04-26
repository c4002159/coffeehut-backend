package com.coffeehut.coffeehut.payment.Service;

import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.payment.dto.HorsePayRequest;
import com.coffeehut.coffeehut.payment.dto.HorsePayResponse;
import com.coffeehut.coffeehut.payment.dto.PaymentPayRequest;
import com.coffeehut.coffeehut.payment.dto.PaymentRefundRequest;
import com.coffeehut.coffeehut.repository.OrderRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PaymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Resource
    private OrderRepository orderRepository;

    @Value("${horsepay.api.url:http://homepages.cs.ncl.ac.uk/daniel.nesbitt/CSC8019/HorsePay/api/pay}")
    private String horsePayApiUrl;

    @Value("${horsepay.api.refund-url:http://homepages.cs.ncl.ac.uk/daniel.nesbitt/CSC8019/HorsePay/api/refund}")
    private String horsePayRefundApiUrl;

    public HorsePayResponse processPayment(PaymentPayRequest request) {
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
            if (!"pending".equalsIgnoreCase(order.getStatus())) {
                throw new ResponseStatusException(BAD_REQUEST, "Order is not payable");
            }
            if (order.getTotalPrice() != null && !amountsEqual(order.getTotalPrice(), request.getTransactionAmount())) {
                throw new ResponseStatusException(BAD_REQUEST, "Amount does not match order total");
            }
        }

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
        HorsePayResponse body = response.getBody();
        if (request.getOrderId() != null && isGatewaySuccess(body)) {
            Order order = orderRepository.findById(request.getOrderId()).orElseThrow();
            order.setStatus("paid");
            orderRepository.save(order);
        }
        return body;
    }

    public HorsePayResponse processRefund(PaymentRefundRequest request) {
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
            if (!"paid".equalsIgnoreCase(order.getStatus()) && !"cancelled".equalsIgnoreCase(order.getStatus())) {
                throw new ResponseStatusException(BAD_REQUEST, "Order is not refundable");
            }
            if (order.getTotalPrice() != null && !amountsEqual(order.getTotalPrice(), request.getTransactionAmount())) {
                throw new ResponseStatusException(BAD_REQUEST, "Amount does not match order total");
            }
        }

        HorsePayRequest horsePayRequest = new HorsePayRequest();
        horsePayRequest.setCustomerID(request.getCustomerID());
        horsePayRequest.setTransactionAmount(request.getTransactionAmount());
        horsePayRequest.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        horsePayRequest.setTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<HorsePayRequest> entity = new HttpEntity<>(horsePayRequest, headers);

        ResponseEntity<HorsePayResponse> response = restTemplate.exchange(
                horsePayRefundApiUrl,
                HttpMethod.POST,
                entity,
                HorsePayResponse.class
        );
        HorsePayResponse body = response.getBody();
        if (request.getOrderId() != null && isGatewaySuccess(body)) {
            Order order = orderRepository.findById(request.getOrderId()).orElseThrow();
            order.setStatus("refunded");
            orderRepository.save(order);
        }
        return body;
    }

    private static boolean amountsEqual(Double orderTotal, Double requestAmount) {
        if (orderTotal == null || requestAmount == null) {
            return false;
        }
        return Math.abs(orderTotal - requestAmount) < 0.005;
    }

    private static boolean isGatewaySuccess(HorsePayResponse body) {
        return body != null
                && body.getPaymentSuccess() != null
                && Boolean.TRUE.equals(body.getPaymentSuccess().getStatus());
    }
}
