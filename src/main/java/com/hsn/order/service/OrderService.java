package com.hsn.order.service;

import com.hsn.order.client.InventoryClient;
import com.hsn.order.dto.OrderRequest;
import com.hsn.order.event.OrderPlacedEvent;
import com.hsn.order.model.Order;
import com.hsn.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest) {
        var isProductInStock = inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());

        if (isProductInStock) {
            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setSkuCode(orderRequest.skuCode());
            order.setPrice(orderRequest.price());
            order.setQuantity(orderRequest.quantity());
            orderRepository.save(order);

            OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent(order.getOrderNumber(),
                    orderRequest.userDetails().email());
            kafkaTemplate.send("order-placed", orderPlacedEvent);
            log.info("Sending OrderPlacedEvent {} to Kafka Topics", orderPlacedEvent);
        } else {
            throw new RuntimeException("Product " + orderRequest.skuCode() + " is not available");
        }
    }
}
