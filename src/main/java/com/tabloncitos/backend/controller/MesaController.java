package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.MesaReservaRequest;
import com.tabloncitos.backend.model.Mesa;
import com.tabloncitos.backend.repository.MesaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mesas")
@CrossOrigin(origins = "*")
public class MesaController {

    private final MesaRepository mesaRepository;

    public MesaController(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    // GET: Listar todas las mesas o filtrar por salón opcionalmente
    @GetMapping
    public List<Mesa> listarMesas(@RequestParam(required = false) String salon) {
        if (salon != null && !salon.isBlank()) {
            // Nota: Asegúrate de tener el método findBySalon en tu MesaRepository si usas este filtro
            return mesaRepository.findAll();
        }
        return mesaRepository.findAll();
    }

    // PUT/PATCH: Actualizar el estado de la mesa enviando un JSON (ej: {"estado": "OCUPADA"})
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstadoMesa(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        Optional<Mesa> mesaOpt = mesaRepository.findById(id);
        if (mesaOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", "error", "mensaje", "Mesa no encontrada"));
        }

        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || !List.of("LIBRE", "OCUPADA", "SERVIDA", "CUENTA_PEDIDA").contains(nuevoEstado)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "El estado no es válido; las reservas deben crearse con sus datos"));
        }

        Mesa mesa = mesaOpt.get();
        mesa.actualizarEstado(nuevoEstado, nombreMozoDeSesion(session));
        limpiarReserva(mesa);
        mesaRepository.save(mesa);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "mensaje", "Estado actualizado correctamente",
                "mesa", mesa));
    }

    @PostMapping("/{id}/reserva")
    public ResponseEntity<?> reservarMesa(
            @PathVariable Long id,
            @RequestBody MesaReservaRequest request) {
        Optional<Mesa> mesaOpt = mesaRepository.findById(id);
        if (mesaOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", "error", "mensaje", "Mesa no encontrada"));
        }

        Mesa mesa = mesaOpt.get();
        String estadoActual = mesa.getEstado() == null ? "LIBRE" : mesa.getEstado();
        if (!"LIBRE".equals(estadoActual)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "Solo se pueden reservar mesas que estén libres"));
        }
        if (request.nombre() == null || request.nombre().isBlank() || request.nombre().trim().length() > 120) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "mensaje", "Ingrese el nombre del cliente"));
        }
        if (request.telefono() == null || !request.telefono().trim().matches("[0-9+() -]{7,30}")) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "mensaje", "Ingrese un teléfono válido"));
        }
        if (request.fechaHora() == null || !request.fechaHora().isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "La fecha y hora de la reserva deben ser futuras"));
        }
        if (request.personas() == null || request.personas() < 1 || request.personas() > 50) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "La cantidad de personas debe estar entre 1 y 50"));
        }

        mesa.actualizarEstado("RESERVADA");
        mesa.setReservaNombre(request.nombre().trim());
        mesa.setReservaTelefono(request.telefono().trim());
        mesa.setReservaFechaHora(request.fechaHora());
        mesa.setReservaPersonas(request.personas());
        mesaRepository.save(mesa);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "mensaje", "Mesa reservada correctamente",
                "mesa", mesa));
    }

    private void limpiarReserva(Mesa mesa) {
        mesa.setReservaNombre(null);
        mesa.setReservaTelefono(null);
        mesa.setReservaFechaHora(null);
        mesa.setReservaPersonas(null);
    }

    private String nombreMozoDeSesion(HttpSession session) {
        Object usuario = session.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof Map<?, ?> identidad)) return null;

        Object rol = identidad.get("rol");
        Object nombre = identidad.get("nombre");
        if (rol == null || nombre == null
                || !List.of("MOZO", "MESERO").contains(rol.toString().trim().toUpperCase())) {
            return null;
        }
        return nombre.toString().trim();
    }
}