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

/**
 * Service class responsible for handling payment and refund operations.
 * <p>
 * This class integrates with an external payment gateway (HorsePay) to process
 * payment and refund requests. It validates order status and transaction amount
 * before sending requests, and updates the corresponding {@link Order} status
 * based on the gateway response.
 * </p>
 */
@Service
public class PaymentService {

    /** REST client used to communicate with the external payment gateway. */
    private final RestTemplate restTemplate = new RestTemplate();

    /** Repository for accessing and updating {@link Order} data. */
    @Resource
    private OrderRepository orderRepository;

    /**
     * Endpoint URL for the HorsePay payment API.
     * <p>
     * Default value is provided, but can be overridden via application configuration.
     * </p>
     */
    @Value("${horsepay.api.url:http://homepages.cs.ncl.ac.uk/daniel.nesbitt/CSC8019/HorsePay/HorsePay.php}")
    private String horsePayApiUrl;

    /**
     * Endpoint URL for the HorsePay refund API.
     * <p>
     * Default value is provided, but can be overridden via application configuration.
     * </p>
     */
    @Value("${horsepay.api.refund-url:http://homepages.cs.ncl.ac.uk/daniel.nesbitt/CSC8019/HorsePay/api/refund}")
    private String horsePayRefundApiUrl;

    /**
     * Processes a payment request through the HorsePay gateway.
     * <p>
     * If an {@code orderId} is provided, this method validates that the order exists,
     * is in {@code pending} status, and that the requested amount matches the order total.
     * Upon successful payment, the order status is updated to {@code paid}.
     * </p>
     *
     * @param request the payment request containing customer and transaction details
     * @return the {@link HorsePayResponse} returned by the payment gateway, or {@code null} if no response body is returned
     * @throws ResponseStatusException if the order is not found, not payable, or the amount does not match
     */
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

        // Update order status only when gateway confirms success
        if (request.getOrderId() != null && isGatewaySuccess(body)) {
            Order order = orderRepository.findById(request.getOrderId()).orElseThrow();
            order.setStatus("paid");
            orderRepository.save(order);
        }

        return body;
    }

    /**
     * Processes a refund request through the HorsePay gateway.
     * <p>
     * If an {@code orderId} is provided, this method validates that the order exists,
     * is in {@code paid} or {@code cancelled} status, and that the refund amount matches
     * the order total. Upon successful refund, the order status is updated to {@code refunded}.
     * </p>
     *
     * @param request the refund request containing customer and transaction details
     * @return the {@link HorsePayResponse} returned by the refund gateway, or {@code null} if no response body is returned
     * @throws ResponseStatusException if the order is not found, not refundable, or the amount does not match
     */
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

        // Update order status only when gateway confirms success
        if (request.getOrderId() != null && isGatewaySuccess(body)) {
            Order order = orderRepository.findById(request.getOrderId()).orElseThrow();
            order.setStatus("refunded");
            orderRepository.save(order);
        }

        return body;
    }

    /**
     * Compares two monetary amounts with tolerance for floating-point precision.
     * <p>
     * This method accounts for small rounding differences by allowing a tolerance
     * of {@code 0.005} when comparing values.
     * </p>
     *
     * @param orderTotal the total amount from the order
     * @param requestAmount the transaction amount from the request
     * @return {@code true} if the amounts are considered equal within tolerance,
     *         {@code false} otherwise or if either value is {@code null}
     */
    private static boolean amountsEqual(Double orderTotal, Double requestAmount) {
        if (orderTotal == null || requestAmount == null) {
            return false;
        }
        return Math.abs(orderTotal - requestAmount) < 0.005;
    }

    /**
     * Determines whether the payment gateway response indicates success.
     * <p>
     * This method checks that the response and its nested status object are not {@code null},
     * and that the {@code status} field is {@code true}.
     * </p>
     *
     * @param body the {@link HorsePayResponse} returned by the gateway
     * @return {@code true} if the payment or refund was successful, {@code false} otherwise
     */
    private static boolean isGatewaySuccess(HorsePayResponse body) {
        return body != null
                && body.getPaymentSuccess() != null
                && Boolean.TRUE.equals(body.getPaymentSuccess().getStatus());
    }
}