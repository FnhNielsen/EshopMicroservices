package dk.sdu.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelOrderDto {
    public UUID orderId;
    public String customerId;
    public int quantity;
}
