package com.tabloncitos.backend.dto;

public record SolicitudOtpAdminDTO(
        String codigo,
        String nombre,
        String correo,
        String otpGenerado) {
}
