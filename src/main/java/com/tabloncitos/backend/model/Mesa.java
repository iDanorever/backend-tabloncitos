package com.tabloncitos.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "mesas")
@Data
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_mesa", nullable = false)
    private Integer numeroMesa;

    @Column(length = 50)
    private String zona; // Salón Principal, Terraza, etc.

    @Column(length = 20)
    private String estado = "LIBRE"; // LIBRE, OCUPADA, etc.
}