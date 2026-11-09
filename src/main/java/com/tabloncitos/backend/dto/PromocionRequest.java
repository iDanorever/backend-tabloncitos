package com.tabloncitos.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PromocionRequest(
        String nombre,
        String descripcion,
        String tipoBeneficio,
        BigDecimal valorBeneficio,
        Long productoId,
        List<String> diasSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        List<String> canales) {
}
