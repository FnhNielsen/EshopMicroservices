package dk.sdu.orderservice.dto;

import dk.sdu.orderservice.model.OrderProduct;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    public UUID orderId;
    public String customerId;
    public String orderStatus;
    public List<OrderProduct> orderProducts;
}
