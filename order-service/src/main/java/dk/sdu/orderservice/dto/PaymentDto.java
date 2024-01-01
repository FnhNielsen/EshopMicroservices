package dk.sdu.orderservice.dto;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentDto {
    public UUID orderId;
    public String customerId;
    public String orderStatus;
}
