package com.tabloncitos.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "mesas")
@Data
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_mesa", nullable = false)
    private Integer numeroMesa;

    @Column(length = 50)
    private String zona; // Salón Principal, Terraza, etc.

    @Column(length = 20)
    private String estado = "LIBRE"; // LIBRE, OCUPADA, etc.

    @Column(name = "reserva_nombre", length = 120)
    private String reservaNombre;

    @Column(name = "reserva_telefono", length = 30)
    private String reservaTelefono;

    @Column(name = "reserva_fecha_hora")
    private LocalDateTime reservaFechaHora;

    @Column(name = "reserva_personas")
    private Integer reservaPersonas;

    @Column(name = "ocupada_desde")
    private LocalDateTime ocupadaDesde;

    @Column(name = "mozo_responsable", length = 100)
    private String mozoResponsable;

    @Column(name = "ultima_comanda_enviada")
    private LocalDateTime ultimaComandaEnviada;

    public void actualizarEstado(String nuevoEstado) {
        actualizarEstado(nuevoEstado, null);
    }

    public void actualizarEstado(String nuevoEstado, String nuevoMozoResponsable) {
        boolean estabaOcupada = esEstadoOcupado(estado);
        boolean quedaOcupada = esEstadoOcupado(nuevoEstado);
        if (quedaOcupada && !estabaOcupada) {
            ocupadaDesde = LocalDateTime.now();
            ultimaComandaEnviada = null;
        } else if (!quedaOcupada) {
            ocupadaDesde = null;
            mozoResponsable = null;
            ultimaComandaEnviada = null;
        }
        if (quedaOcupada && (mozoResponsable == null || mozoResponsable.isBlank())
                && nuevoMozoResponsable != null && !nuevoMozoResponsable.isBlank()) {
            mozoResponsable = nuevoMozoResponsable.trim();
        }
        estado = nuevoEstado;
    }

    public void registrarComandaEnviada() {
        ultimaComandaEnviada = LocalDateTime.now();
    }

    private boolean esEstadoOcupado(String valorEstado) {
        return valorEstado != null && Set.of("OCUPADA", "SERVIDA", "CUENTA_PEDIDA").contains(valorEstado);
    }
}