package com.tabloncitos.backend.dto;

public record EmpleadoResumenDTO(
        Long id,
        String codigo,
        String dni,
        String nombre,
        String correo,
        String rol,
        String sede,
        String fuente,
        boolean activo) {
}
