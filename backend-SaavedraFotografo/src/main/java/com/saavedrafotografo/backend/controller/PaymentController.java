package com.saavedrafotografo.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    // Optional legacy endpoint for PaymentIntent (not used by frontend now)
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> body) {
        if (Stripe.apiKey == null || Stripe.apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Stripe secret key no configurada"));
        }
        long amount = 0L;
        try {
            Object amountObj = body.get("amount");
            amount = Long.parseLong(String.valueOf(amountObj));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount requerido"));
        }
        String currency = String.valueOf(body.getOrDefault("currency", "usd"));

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params);
            return ResponseEntity.ok(Map.of("clientSecret", intent.getClientSecret()));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody Map<String, Object> body) {
        if (Stripe.apiKey == null || Stripe.apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Stripe secret key no configurada"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "items requerido"));
        }
        String currency = String.valueOf(body.getOrDefault("currency", "usd"));
        String successUrl = String.valueOf(body.getOrDefault("successUrl",
                "http://localhost:4200/calendario?success=true"));
        String cancelUrl = String.valueOf(body.getOrDefault("cancelUrl",
                "http://localhost:4200/carrito?canceled=true"));

        // Ensure successUrl has session id placeholder
        if (!successUrl.contains("{CHECKOUT_SESSION_ID}")) {
            if (successUrl.contains("?")) {
                successUrl = successUrl + "&session_id={CHECKOUT_SESSION_ID}";
            } else {
                successUrl = successUrl + "?session_id={CHECKOUT_SESSION_ID}";
            }
        }

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl);

        for (Map<String, Object> it : items) {
            String name = String.valueOf(it.getOrDefault("name", "Item"));
            long amountMinor = 0L;
            try {
                long amount = Long.parseLong(String.valueOf(it.getOrDefault("amount", 0)));
                boolean zeroDecimal = java.util.Set.of("BIF","CLP","DJF","GNF","JPY","KMF","KRW","MGA","PYG","RWF","UGX","VND","VUV","XAF","XOF","XPF")
                        .contains(currency.toUpperCase());
                amountMinor = zeroDecimal ? amount : amount * 100;
            } catch (NumberFormatException ex) {
                // skip invalid amounts
                continue;
            }
            if (amountMinor <= 0) continue;

            SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(currency)
                    .setUnitAmount(amountMinor)
                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(name)
                            .build())
                    .build();
            builder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(priceData)
                    .build());
        }

        try {
            Session session = Session.create(builder.build());
            return ResponseEntity.ok(Map.of("id", session.getId(), "url", session.getUrl()));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
