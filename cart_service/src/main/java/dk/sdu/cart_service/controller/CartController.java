package dk.sdu.cart_service.controller;

import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.model.ReservationEvent;
import io.dapr.client.DaprClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/cart")
public class CartController {
    public final String redisStore = "cart-store";
    public final String pubSubName = "kafka-commonpubsub";
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @GetMapping(value = "/status")
    @ResponseStatus(HttpStatus.OK)
    public String getStatus() {
        return "Connected to shopping cart";
    }

    @PostMapping(value = "/reserve")
    public ResponseEntity<String> addProductToBasket(@RequestBody(required = false) Reservation reservation, DaprClient daprClient)
    {
        if (reservation == null || reservation.getItems().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        daprClient.saveState(redisStore,reservation.getCustomerId(), Reservation.class);
        var state = daprClient.getState(redisStore, reservation.getCustomerId(), Reservation.class);
        state.block().getValue();

        for (var item: reservation.getItems()) {
            var reservationEvent = new ReservationEvent(reservation.getCustomerId(), item.getQuantity(), item.getProductId());
            daprClient.publishEvent(pubSubName,"On_Products_Reserved",reservationEvent);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}


