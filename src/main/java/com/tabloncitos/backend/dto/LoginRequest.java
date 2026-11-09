package com.tabloncitos.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LoginRequest(
        @JsonAlias({"codigo", "correo"}) String identificador,
        @JsonAlias("pin") String password,
        String rolSeleccionado) {
}
