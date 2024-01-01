package dk.sdu.orderservice.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "OrderProduct")
public class OrderProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private UUID orderId;
    private String productId;
    private double price;
    private int quantity;


    public OrderProduct(UUID orderId, String productId, double price, int quantity) {
    }
}
