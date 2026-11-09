package com.tabloncitos.backend.dto;

import java.math.BigDecimal;

public record ProductoCrearRequest(
        String nombre,
        BigDecimal precio,
        Long categoriaId,
        String estacionKds) {
}
