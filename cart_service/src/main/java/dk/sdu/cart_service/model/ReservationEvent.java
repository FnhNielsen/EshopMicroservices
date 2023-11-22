package dk.sdu.cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReservationEvent {
    public String customerId;
    public int quantity;
    public String ProductId;
}
