/**
 * Unit tests for {@link PaymentService}.
 * <p>
 * Covers requirements: FR6.
 * All dependencies are mocked via Mockito — no real database or
 * external HorsePay gateway is contacted during these tests.
 * {@link RestTemplate} is replaced with a mock via
 * {@link org.springframework.test.util.ReflectionTestUtils} because
 * it is instantiated directly inside the service rather than injected.
 * </p>
 */
package com.coffeehut.coffeehut.payment;

import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.payment.Service.PaymentService;
import com.coffeehut.coffeehut.payment.dto.HorsePayResponse;
import com.coffeehut.coffeehut.payment.dto.PaymentPayRequest;
import com.coffeehut.coffeehut.payment.dto.PaymentRefundRequest;
import com.coffeehut.coffeehut.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private Order pendingOrder;
    private Order paidOrder;
    private Order cancelledOrder;

    /**
     * Initialises shared test fixtures and injects the mock
     * {@link RestTemplate} into the service before each test.
     * <p>
     * {@code RestTemplate} is created inside the service via
     * {@code new RestTemplate()} rather than being injected, so
     * {@link ReflectionTestUtils#setField} is used to replace it
     * with the Mockito mock after construction.
     * </p>
     */
    @BeforeEach
    void setUp() {
        // Replace the internally-created RestTemplate with the mock
        ReflectionTestUtils.setField(paymentService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(paymentService, "horsePayApiUrl",
                "http://mock-horsepay/pay");
        ReflectionTestUtils.setField(paymentService, "horsePayRefundApiUrl",
                "http://mock-horsepay/refund");

        pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setStatus("pending");
        pendingOrder.setTotalPrice(3.50);

        paidOrder = new Order();
        paidOrder.setId(2L);
        paidOrder.setStatus("paid");
        paidOrder.setTotalPrice(3.50);

        cancelledOrder = new Order();
        cancelledOrder.setId(3L);
        cancelledOrder.setStatus("cancelled");
        cancelledOrder.setTotalPrice(3.50);
    }

    // ══════════════════════════════════════════════════════
    // processPayment — FR6: Normal cases
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that a successful payment response from the gateway
     * updates the order status to {@code paid}.
     * <p>
     * When the HorsePay gateway returns a success response and an
     * {@code orderId} is present in the request, the order must be
     * updated to {@code "paid"} and persisted.
     * </p>
     *
     * @throws AssertionError if the returned response is {@code null} or
     *                        the order status is not updated to {@code paid}
     * Covers FR6
     */
    @Test
    void processPayment_gatewaySuccess_updatesOrderStatusToPaid() {
        PaymentPayRequest request = new PaymentPayRequest();
        request.setOrderId(1L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        HorsePayResponse.PaymentSuccess success = new HorsePayResponse.PaymentSuccess();
        success.setStatus(true);

        HorsePayResponse gatewayResponse = new HorsePayResponse();
        gatewayResponse.setPaymentSuccess(success);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(HorsePayResponse.class)))
                .thenReturn(new ResponseEntity<>(gatewayResponse, HttpStatus.OK));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HorsePayResponse result = paymentService.processPayment(request);

        assertNotNull(result);
        assertTrue(result.getPaymentSuccess().getStatus());
        verify(orderRepository, times(2)).findById(1L);
        verify(orderRepository).save(argThat(o -> "paid".equals(o.getStatus())));
    }

    /**
     * Verifies that a failed payment response from the gateway does not
     * update the order status.
     * <p>
     * When the HorsePay gateway returns {@code status = false}, the order
     * must remain in its original status and {@code save} must not be called.
     * </p>
     *
     * @throws AssertionError if the order status is modified after a
     *                        gateway failure
     * Covers FR6
     */
    @Test
    void processPayment_gatewayFailure_doesNotUpdateOrderStatus() {
        PaymentPayRequest request = new PaymentPayRequest();
        request.setOrderId(1L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        HorsePayResponse.PaymentSuccess failure = new HorsePayResponse.PaymentSuccess();
        failure.setStatus(false);
        failure.setReason("Insufficient funds");

        HorsePayResponse gatewayResponse = new HorsePayResponse();
        gatewayResponse.setPaymentSuccess(failure);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(HorsePayResponse.class)))
                .thenReturn(new ResponseEntity<>(gatewayResponse, HttpStatus.OK));

        HorsePayResponse result = paymentService.processPayment(request);

        assertNotNull(result);
        assertFalse(result.getPaymentSuccess().getStatus());
        // save must NOT be called when the gateway returns failure
        verify(orderRepository, never()).save(any());
    }

    /**
     * Verifies that a payment request without an {@code orderId} is
     * forwarded directly to the gateway without any order validation.
     * <p>
     * Guest checkout does not link to a stored order, so the service
     * must skip the order lookup entirely.
     * </p>
     *
     * @throws AssertionError if the repository is queried or the
     *                        response is {@code null}
     * Covers FR6
     */
    @Test
    void processPayment_noOrderId_skipsOrderValidation() {
        PaymentPayRequest request = new PaymentPayRequest();
        request.setOrderId(null);
        request.setCustomerID("CUST-GUEST");
        request.setTransactionAmount(2.50);

        HorsePayResponse.PaymentSuccess success = new HorsePayResponse.PaymentSuccess();
        success.setStatus(true);
        HorsePayResponse gatewayResponse = new HorsePayResponse();
        gatewayResponse.setPaymentSuccess(success);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(HorsePayResponse.class)))
                .thenReturn(new ResponseEntity<>(gatewayResponse, HttpStatus.OK));

        HorsePayResponse result = paymentService.processPayment(request);

        assertNotNull(result);
        // No orderId — repository must never be consulted
        verify(orderRepository, never()).findById(any());
    }

    // ══════════════════════════════════════════════════════
    // processPayment — FR6: Error cases
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that a {@code 404 Not Found} exception is thrown when
     * the order linked to the payment request does not exist.
     *
     * @throws AssertionError if no exception is thrown or the HTTP
     *                        status is not {@code 404}
     * Covers FR6
     */
    @Test
    void processPayment_orderNotFound_throwsNotFoundException() {
        PaymentPayRequest request = new PaymentPayRequest();
        request.setOrderId(99L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.processPayment(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Verifies that a {@code 400 Bad Request} exception is thrown when
     * the order status is not {@code pending} at the time of payment.
     * <p>
     * Only orders in {@code pending} status are payable. Attempting to
     * pay for an already-paid or collected order must be rejected.
     * </p>
     *
     * @throws AssertionError if no exception is thrown or the HTTP
     *                        status is not {@code 400}
     * Covers FR6
     */
    @Test
    void processPayment_orderNotPending_throwsBadRequestException() {
        PaymentPayRequest request = new PaymentPayRequest();
        request.setOrderId(2L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        when(orderRepository.findById(2L)).thenReturn(Optional.of(paidOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.processPayment(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    /**
     * Verifies that a {@code 400 Bad Request} exception is thrown when
     * the transaction amount does not match the stored order total.
     * <p>
     * The gateway request amount must match the order total within a
     * tolerance of £0.005 to prevent amount tampering.
     * </p>
     *
     * @throws AssertionError if no exception is thrown or the HTTP
     *                        status is not {@code 400}
     * Covers FR6
     */
    @Test
    void processPayment_amountMismatch_throwsBadRequestException() {
        PaymentPayRequest request = new PaymentPayRequest();
        request.setOrderId(1L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(9.99); // does not match order total of 3.50

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.processPayment(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ══════════════════════════════════════════════════════
    // processRefund — FR6: Normal cases
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that a successful refund response from the gateway
     * updates the order status to {@code refunded}.
     * <p>
     * When the gateway confirms the refund, the order must be persisted
     * with status {@code "refunded"}.
     * </p>
     *
     * @throws AssertionError if the response is {@code null} or the order
     *                        status is not updated to {@code refunded}
     * Covers FR6
     */
    @Test
    void processRefund_gatewaySuccess_updatesOrderStatusToRefunded() {
        PaymentRefundRequest request = new PaymentRefundRequest();
        request.setOrderId(2L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        HorsePayResponse.PaymentSuccess success = new HorsePayResponse.PaymentSuccess();
        success.setStatus(true);
        HorsePayResponse gatewayResponse = new HorsePayResponse();
        gatewayResponse.setPaymentSuccess(success);

        when(orderRepository.findById(2L)).thenReturn(Optional.of(paidOrder));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(HorsePayResponse.class)))
                .thenReturn(new ResponseEntity<>(gatewayResponse, HttpStatus.OK));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HorsePayResponse result = paymentService.processRefund(request);

        assertNotNull(result);
        assertTrue(result.getPaymentSuccess().getStatus());
        verify(orderRepository).save(argThat(o -> "refunded".equals(o.getStatus())));
    }

    /**
     * Verifies that a cancelled order can also be refunded.
     * <p>
     * The refund endpoint accepts both {@code paid} and {@code cancelled}
     * statuses as valid preconditions.
     * </p>
     *
     * @throws AssertionError if an exception is thrown or the order
     *                        status is not updated
     * Covers FR6
     */
    @Test
    void processRefund_cancelledOrder_allowsRefund() {
        PaymentRefundRequest request = new PaymentRefundRequest();
        request.setOrderId(3L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        HorsePayResponse.PaymentSuccess success = new HorsePayResponse.PaymentSuccess();
        success.setStatus(true);
        HorsePayResponse gatewayResponse = new HorsePayResponse();
        gatewayResponse.setPaymentSuccess(success);

        when(orderRepository.findById(3L)).thenReturn(Optional.of(cancelledOrder));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(HorsePayResponse.class)))
                .thenReturn(new ResponseEntity<>(gatewayResponse, HttpStatus.OK));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HorsePayResponse result = paymentService.processRefund(request);

        assertNotNull(result);
        verify(orderRepository).save(argThat(o -> "refunded".equals(o.getStatus())));
    }

    // ══════════════════════════════════════════════════════
    // processRefund — FR6: Error cases
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that a {@code 404 Not Found} exception is thrown when
     * the order linked to the refund request does not exist.
     *
     * @throws AssertionError if no exception is thrown or the HTTP
     *                        status is not {@code 404}
     * Covers FR6
     */
    @Test
    void processRefund_orderNotFound_throwsNotFoundException() {
        PaymentRefundRequest request = new PaymentRefundRequest();
        request.setOrderId(99L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.processRefund(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Verifies that a {@code 400 Bad Request} exception is thrown when
     * the order is in a non-refundable status such as {@code pending}.
     * <p>
     * Only {@code paid} and {@code cancelled} orders are eligible for
     * a refund. A {@code pending} order has not been charged and must
     * be rejected.
     * </p>
     *
     * @throws AssertionError if no exception is thrown or the HTTP
     *                        status is not {@code 400}
     * Covers FR6
     */
    @Test
    void processRefund_orderNotRefundable_throwsBadRequestException() {
        PaymentRefundRequest request = new PaymentRefundRequest();
        request.setOrderId(1L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.processRefund(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    /**
     * Verifies that a {@code 400 Bad Request} exception is thrown when
     * the refund amount does not match the stored order total.
     *
     * @throws AssertionError if no exception is thrown or the HTTP
     *                        status is not {@code 400}
     * Covers FR6
     */
    @Test
    void processRefund_amountMismatch_throwsBadRequestException() {
        PaymentRefundRequest request = new PaymentRefundRequest();
        request.setOrderId(2L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(99.99); // does not match order total of 3.50

        when(orderRepository.findById(2L)).thenReturn(Optional.of(paidOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.processRefund(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ══════════════════════════════════════════════════════
    // processPayment / processRefund — FR6: Boundary cases
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that two amounts differing by less than £0.005 are treated
     * as equal and do not trigger an amount-mismatch error.
     * <p>
     * The tolerance check uses {@code Math.abs(a - b) < 0.005}, so
     * a difference of £0.004 must be accepted as matching.
     * </p>
     *
     * @throws AssertionError if an exception is thrown for a within-tolerance
     *                        amount difference
     * Covers FR6
     */
    @Test
    void processPayment_amountWithinTolerance_doesNotThrow() {
        PaymentPayRequest request = new PaymentPayRequest();
        request.setOrderId(1L);
        request.setCustomerID("CUST-001");
        // 3.504 differs from 3.50 by 0.004, which is within the 0.005 tolerance
        request.setTransactionAmount(3.504);

        HorsePayResponse.PaymentSuccess success = new HorsePayResponse.PaymentSuccess();
        success.setStatus(true);
        HorsePayResponse gatewayResponse = new HorsePayResponse();
        gatewayResponse.setPaymentSuccess(success);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(HorsePayResponse.class)))
                .thenReturn(new ResponseEntity<>(gatewayResponse, HttpStatus.OK));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> paymentService.processPayment(request));
    }

    /**
     * Verifies that a {@code null} gateway response body does not cause
     * a {@link NullPointerException} and does not update the order status.
     * <p>
     * {@code isGatewaySuccess} must guard against a {@code null} body
     * returned by the gateway under error conditions.
     * </p>
     *
     * @throws AssertionError if an exception is thrown or the order
     *                        status is modified
     * Covers FR6
     */
    @Test
    void processPayment_nullGatewayResponse_doesNotUpdateOrder() {
        PaymentPayRequest request = new PaymentPayRequest();
        request.setOrderId(1L);
        request.setCustomerID("CUST-001");
        request.setTransactionAmount(3.50);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(HorsePayResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        HorsePayResponse result = paymentService.processPayment(request);

        assertNull(result);
        // null body → isGatewaySuccess returns false → save must not be called
        verify(orderRepository, never()).save(any());
    }
}