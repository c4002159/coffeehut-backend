package com.coffeehut.coffeehut.payment.Controller;

import com.coffeehut.coffeehut.payment.Service.PaymentService;
import com.coffeehut.coffeehut.payment.dto.HorsePayResponse;
import com.coffeehut.coffeehut.payment.dto.PaymentPayRequest;
import com.coffeehut.coffeehut.payment.dto.PaymentRefundRequest;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<HorsePayResponse> pay(@RequestBody PaymentPayRequest request) {
        HorsePayResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<HorsePayResponse> refund(@RequestBody PaymentRefundRequest request) {
        HorsePayResponse response = paymentService.processRefund(request);
        return ResponseEntity.ok(response);
    }
}
