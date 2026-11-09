package com.tabloncitos.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100, unique = true)
    private String correo;

    @Column(unique = true, length = 20)
    private String codigo;

    @Column(length = 20)
    private String dni;

    @Column(length = 100)
    private String sede;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String rol; // ADMIN, MESERO, COCINERO, CAJERO

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "otp_generado", length = 10)
    private String otpGenerado;

    @Column(name = "estado_solicitud_otp", length = 20)
    private String estadoSolicitudOtp;
}