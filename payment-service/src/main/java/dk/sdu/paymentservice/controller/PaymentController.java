package dk.sdu.paymentservice.controller;

import dk.sdu.paymentservice.model.PaymentDto;
import io.dapr.Topic;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("api/payment")
@Slf4j
public class PaymentController {
    public final String pubSubName = "kafka-pubsub";

    public PaymentController() {

    }

    @PostMapping(value = "/pay")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Order_Submit", pubsubName = pubSubName)
    public Mono<ResponseEntity<String>> handlePayment(@RequestBody CloudEvent<PaymentDto> cloudEvent) {
        return Mono.fromCallable(() -> {
            try {
                DaprClient client = new DaprClientBuilder().build();
                log.info("Received Order: {}, from Customer: {}", cloudEvent.getData().getCustomerId(), cloudEvent.getData().getOrderId());
                int tmp = (int) (Math.random() * 2 + 1); // will return either 1 or 2
                var event = cloudEvent.getData();
                if(tmp == 1) {
                    client.publishEvent(pubSubName, "On_Payment_Received", event).block();
                    log.info("PAYMENT received for Customer: {}, with Order: {}",
                            cloudEvent.getData().getCustomerId(), cloudEvent.getData().getOrderId());
                    return ResponseEntity.ok().body("Payment received");
                }

                client.publishEvent(pubSubName, "On_Payment_Failed", cloudEvent.getData()).block();
                log.info("Payment failed for Customer: {}, with Order: {}",
                        cloudEvent.getData().getCustomerId(), cloudEvent.getData().getOrderId());
                return ResponseEntity.ok().body("Payment failed");
            } catch (Exception e) {
                log.error("Error occurred while publishing event", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while publishing event");
            }
        });
    }

    @PostMapping(value = "/ship")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String>paymentSuccess(@RequestBody CloudEvent<PaymentDto> cloudEvent) {
        try {
            DaprClient client = new DaprClientBuilder().build();
            client.publishEvent(pubSubName, "On_Order_Shipped", cloudEvent.getData()).block();
            log.info("Order sent to shipping: {}", cloudEvent.getData().getOrderId());
            return ResponseEntity.ok().body("Order shipped");
        } catch (Exception e) {
            log.error("Error occurred while publishing event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while publishing event");
        }
    }
}
