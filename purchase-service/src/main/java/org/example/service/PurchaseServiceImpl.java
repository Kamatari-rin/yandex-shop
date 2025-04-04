package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.client.CartClient;
import org.example.client.OrderClient;
import org.example.client.ProductClient;
import org.example.dto.*;
import org.example.exception.PurchaseException;
import org.example.model.Purchase;
import org.example.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final OrderClient orderClient;
    private final PurchaseRepository purchaseRepository;

    @Override
    @Transactional
    public PurchaseResponseDTO createPurchase(UUID userId) {
        CartDTO cart = getValidatedCart(userId);
        validatePrices(cart);

        OrderDTO order = createOrder(cart);

        Purchase purchase = Purchase.builder()
                .orderId(order.id())
                .userId(userId)
                .paymentStatus("PENDING")
                .transactionDate(LocalDateTime.now())
                .details("Payment via card")
                .build();
        purchaseRepository.save(purchase);

        clearCart(userId);

        return new PurchaseResponseDTO(
                purchase.getOrderId(),
                purchase.getUserId(),
                cart.totalAmount(),
                purchase.getPaymentStatus(),
                purchase.getTransactionDate()
        );
    }

    private CartDTO getValidatedCart(UUID userId) {
        CartDTO cart = cartClient.getCart(userId);
        if (cart == null || cart.items().isEmpty()) {
            throw new PurchaseException("Cart is empty for user: " + userId);
        }
        return cart;
    }

    private void validatePrices(CartDTO cart) {
        cart.items().forEach(item -> {
            ProductDetailDTO product = productClient.getProductById(item.productId());
            if (!product.price().equals(item.priceAtTime())) {
                throw new PurchaseException("Price mismatch for product ID: " + item.productId());
            }
        });
    }

    private OrderDTO createOrder(CartDTO cart) {
        CreateOrderRequestDTO request = new CreateOrderRequestDTO(
                cart.userId(),
                LocalDateTime.now(),
                "PENDING",
                cart.totalAmount(),
                cart.items().stream()
                        .map(item -> new CreateOrderRequestDTO.OrderItemDTO(
                                item.productId(),
                                item.quantity(),
                                item.priceAtTime(),
                                item.imageUrl()))
                        .toList()
        );
        return orderClient.createOrder(request);
    }

    private void clearCart(UUID userId) {
        cartClient.clearCart(userId);
    }
}