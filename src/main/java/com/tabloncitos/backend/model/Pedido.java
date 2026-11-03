package com.tabloncitos.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Data
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con Mesa (puede ser nulo si es delivery)
    @ManyToOne
    @JoinColumn(name = "mesa_id", nullable = true)
    private Mesa mesa;

    // Mozo que tomó el pedido
    @ManyToOne
    @JoinColumn(name = "mozo_id", nullable = false)
    private Usuario mozo;

    // Cajero que procesó el cobro (puede ser nulo al inicio)
    @ManyToOne
    @JoinColumn(name = "cajero_id", nullable = true)
    private Usuario cajero;

    @Column(name = "tipo_servicio", length = 30)
    private String tipoService; // SALON, TAKEAWAY, DELIVERY

    @Column(name = "estado_pedido", length = 30)
    private String estadoPedido = "EN_COLA"; // EN_COLA, EN_PREPARACION, LISTO, ENTREGADO, PAGADO

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "cliente_nombre", length = 150)
    private String clienteNombre;

    @Column(name = "cliente_documento", length = 20)
    private String clienteDocumento;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}