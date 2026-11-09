package com.tabloncitos.backend.dto;

import java.time.LocalDateTime;

public record EmpleadoBajaDTO(
        Long id,
        String codigo,
        String dni,
        String nombre,
        String correo,
        String rol,
        String sede,
        String fuente,
        String motivo,
        LocalDateTime eliminadoEn) {
}
