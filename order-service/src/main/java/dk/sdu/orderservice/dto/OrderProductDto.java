package dk.sdu.orderservice.dto;


import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductDto {
    public int id;
    public UUID orderId;
    public String productId;
    public double price;
    public int quantity;
}
