package com.tabloncitos.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record EmpleadoRegistroRequest(
        String codigo,
        String dni,
        String nombre,
        String correo,
        @JsonAlias("pin") String password,
        String rol,
        String sede) {
}
