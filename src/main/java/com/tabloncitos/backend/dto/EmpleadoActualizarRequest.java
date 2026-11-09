package com.tabloncitos.backend.dto;

public record EmpleadoActualizarRequest(
        String dni,
        String nombre,
        String correo,
        String password,
        String rol,
        String sede) {
}
