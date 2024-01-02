package dk.sdu.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.orderservice.dto.*;
import dk.sdu.orderservice.mapper.OrderDtoMapper;
import dk.sdu.orderservice.model.Customer;
import dk.sdu.orderservice.model.Order;
import dk.sdu.orderservice.model.OrderProduct;
import dk.sdu.orderservice.service.OrderService;
import io.dapr.Topic;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import io.dapr.exceptions.DaprException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.mapper.Mapper;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("api/order")
public class OrderController {
    private final OrderService orderService;
    private final OrderDtoMapper orderDtoMapper;
    private final String pubSubName = "kafka-pubsub";

    @Autowired
    public OrderController(OrderService orderService, OrderDtoMapper orderDtoMapper) {
        this.orderService = orderService;
        this.orderDtoMapper = orderDtoMapper;
    }

    @GetMapping(value = "/status")
    @ResponseStatus(HttpStatus.OK)
    public String getStatus() {
        return "Connected to order-service";
    }

    @GetMapping(value = "/{orderId}")
    public Optional<OrderDto> getOrder(@PathVariable String orderId) {
        var res = orderService.getOrder(orderId);
        System.out.println(res);
        Hibernate.initialize(res);
        return ResponseEntity.ok().body(res).getBody();
    }

    @PostMapping
    public ResponseEntity<Void> addOrder(@RequestBody OrderDto orderDto) {
        orderService.addOrder(orderDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping(value = "/initOrder")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Checkout", pubsubName = pubSubName)
    public Mono<ResponseEntity<String>> startOrder(@RequestBody CloudEvent<OrderEvent> cloudEvent) {
        return Mono.fromSupplier(() -> {

            var order = cloudEvent.getData();
            var orderId = UUID.randomUUID().toString();

            List<OrderProduct> orderProductList = new ArrayList<>();
            for (Item o : order.getItems()){
                var orderToAdd = new OrderProduct(orderId,o.getProductId(),o.getPrice(),o.getQuantity());
                orderProductList.add(orderToAdd);
                log.info("Added product to order: " + orderToAdd.getProductId());
            }
            Order orderToSave = new Order(orderId, order.getCustomerId(), "Pending", orderProductList);
            log.info("Created a new order with status Pending: " + orderToSave.getOrderId());
            OrderDto dto = new OrderDto(orderToSave.getOrderId(),orderToSave.getCustomerId(), orderToSave.getOrderStatus(), orderToSave.getOrderProducts());
            orderService.addOrder(dto);
            return ResponseEntity.ok().build();
        });
    }

    @PostMapping("/submit/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> submitOrder(@PathVariable String id) {
        DaprClient client = new DaprClientBuilder().build();
        try {
            var order = orderService.getOrder(id);
            if (order.isEmpty()) {
                log.error("Order with ID {} not found", id);
                return ResponseEntity.notFound().build();
            }
            order.get().setOrderStatus("Reserved");
            log.info("Order with Id {} , status set to reserved {}", id, order.get().getOrderStatus());
            var paymentDto = new PaymentDto(order.get().getOrderId(), order.get().getCustomerId(), order.get().getOrderStatus());
            client.publishEvent(pubSubName, "On_Order_Submit", paymentDto).block();
            log.info("Order {} submitted", id);
            return ResponseEntity.ok().body(String.valueOf(order));
        } catch (DaprException e) {
            log.error("Error with Dapr client: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the order");
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
        }
    }

    @PostMapping("/add/{orderId}")
    public ResponseEntity<Void> addProductToOrder(@PathVariable String orderId, @RequestBody OrderProductDto orderProductDto) {
        orderService.addProductToOrder(UUID.fromString(orderId), orderProductDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping(value = "/payed")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Order_Shipped", pubsubName = pubSubName)
    public Mono<ResponseEntity<String>> orderPayed(@RequestBody CloudEvent<PaymentDto> cloudEvent) {
        return Mono.fromSupplier(() -> {
            try {
                var event = cloudEvent.getData();
                var orderId = event.getOrderId();
                var order = orderService.getOrder(orderId);
                if (order.isEmpty()) {
                    log.error("Order with ID {} not found", orderId);
                    return ResponseEntity.notFound().build();
                }
                order.get().setOrderStatus("Shipped");
                orderService.updateOrderStatus(order.get());
                log.info("Order {} shipped", orderId);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                log.error("Unexpected error: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
            }
        });
    }

    @DeleteMapping(value = "/delete/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteOrder(@PathVariable String orderId, CloudEvent<CancelOrderDto> cloudEvent) {
        DaprClient client = new DaprClientBuilder().build();
        try {
               var order = orderService.getOrder(orderId);
            if (order.isEmpty()) {
                log.error("Order with ID {} not found", orderId);
                return ResponseEntity.notFound().build();
            }
            orderService.deleteOrder(order.get().getOrderId());
            for (var product : order.get().getOrderProducts()) {
                var cancelOrder = new CancelOrderDto(order.get().getOrderId(), product.getProductId(), product.getQuantity());
                client.publishEvent(pubSubName, "On_Order_Cancel", cancelOrder).block();
            }
            log.info("Order {} deleted", orderId);
            return ResponseEntity.ok().build();
        } catch (DaprException e) {
            log.error("Error with Dapr client: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting the order");
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
        }
    }

    @PostMapping(value = "/paymentFailed")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Payment_Failed", pubsubName = pubSubName)
    public Mono<ResponseEntity<String>> paymentFailed(@RequestBody CloudEvent<PaymentDto> cloudEvent) {
        return Mono.fromSupplier(() -> {
            try {
                DaprClient client = new DaprClientBuilder().build();
                var order = orderService.getOrder(cloudEvent.getData().getCustomerId());
                if (order.isEmpty()) {
                    log.error("Order with ID {} not found", cloudEvent.getData().getCustomerId());
                    return ResponseEntity.notFound().build();
                }
                order.get().setOrderStatus("Cancelled");
                log.info("Order {} payment failed. Order is now canceled", cloudEvent.getData().getCustomerId());
                orderService.updateOrderStatus(order.get());
                for (var product : order.get().getOrderProducts()) {
                    log.info("Canceeling:  " + product.getProductId());
                    var cancelOrder = new CancelOrderDto(order.get().getOrderId(), product.getProductId(), product.getQuantity());
                    client.publishEvent(pubSubName, "On_Order_Cancel", cancelOrder).block();
                }
                log.info("Order {} payment failed. Order is now canceled", cloudEvent.getData().getOrderId());
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                log.error("Unexpected error: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
            }
        });
    }
}