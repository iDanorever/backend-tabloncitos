package com.tabloncitos.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "empleado_bajas")
@Data
public class EmpleadoBaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String fuente;

    @Column(name = "id_original", nullable = false)
    private Long idOriginal;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(length = 20)
    private String dni;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(nullable = false, length = 50)
    private String rol;

    @Column(length = 100)
    private String sede;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "eliminado_en", nullable = false)
    private LocalDateTime eliminadoEn;
}
