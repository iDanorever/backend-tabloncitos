package com.tabloncitos.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "promociones")
@Getter
@Setter
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "tipo_beneficio", nullable = false, length = 20)
    private String tipoBeneficio;

    @Column(name = "valor_beneficio", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorBeneficio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "precio_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioOriginal;

    @Column(name = "dias_semana", nullable = false, length = 100)
    private String diasSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, length = 100)
    private String canales;

    @Column(nullable = false, length = 20)
    private String estado = "BORRADOR";
}
