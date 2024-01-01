package dk.sdu.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.sdu.orderservice.dto.OrderProductDto;
import jakarta.persistence.*;
import jdk.jfr.Timestamp;
import lombok.*;
import org.hibernate.FetchMode;
import org.hibernate.annotations.Fetch;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "Orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;
    private String customerId;
    private String orderStatus;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "orderId")
    private List<OrderProduct> orderProducts;
}
