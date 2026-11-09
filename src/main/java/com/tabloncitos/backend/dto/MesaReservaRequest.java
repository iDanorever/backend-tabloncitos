package com.tabloncitos.backend.dto;

import java.time.LocalDateTime;

public record MesaReservaRequest(
        String nombre,
        String telefono,
        LocalDateTime fechaHora,
        Integer personas) {
}
