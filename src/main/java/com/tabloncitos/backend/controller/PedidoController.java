package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.model.Pedido;
import com.tabloncitos.backend.model.PedidoDetalle;
import com.tabloncitos.backend.repository.PedidoDetalleRepository;
import com.tabloncitos.backend.repository.PedidoRepository;
import com.tabloncitos.backend.repository.UsuarioRepository;
import com.tabloncitos.backend.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoDetalleRepository pedidoDetalleRepository;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @GetMapping("/estado/{estado}")
    public List<Pedido> listarPorEstado(@PathVariable String estado) {
        return pedidoRepository.findByEstadoPedido(estado);
    }

    @GetMapping("/cocina")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listarPedidosCocina(HttpSession session) {
        ResponseEntity<?> acceso = validarAccesoCocina(session);
        if (acceso != null) return acceso;

        List<Map<String, Object>> pedidos = new ArrayList<>();
        for (Pedido pedido : pedidoRepository.findByEstadoPedidoInOrderByFechaCreacionAsc(
                List.of("EN_COLA", "EN_PREPARACION", "LISTO"))) {
            Map<String, Object> mesa = null;
            if (pedido.getMesa() != null) {
                mesa = new LinkedHashMap<>();
                mesa.put("id", pedido.getMesa().getId());
                mesa.put("numeroMesa", pedido.getMesa().getNumeroMesa());
            }

            List<Map<String, Object>> detalles = pedidoDetalleRepository.findByPedidoId(pedido.getId())
                    .stream()
                    .map(detalle -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", detalle.getId());
                        item.put("productoNombre", detalle.getProducto().getNombre());
                        item.put("cantidad", detalle.getCantidad());
                        item.put("observacion", detalle.getObservacion() == null ? "" : detalle.getObservacion());
                        return item;
                    })
                    .toList();

            Map<String, Object> pedidoCocina = new LinkedHashMap<>();
            pedidoCocina.put("id", pedido.getId());
            pedidoCocina.put("mesa", mesa);
            pedidoCocina.put("tipoService", pedido.getTipoService());
            pedidoCocina.put("estadoPedido", pedido.getEstadoPedido());
            pedidoCocina.put("fechaCreacion", pedido.getFechaCreacion());
            pedidoCocina.put("segundosEnCola", segundosTranscurridos(pedido.getFechaCreacion(), LocalDateTime.now()));
            pedidoCocina.put("segundosPreparacion", pedido.getFechaInicioPreparacion() == null
                    ? null
                    : segundosTranscurridos(
                            pedido.getFechaInicioPreparacion(),
                            pedido.getFechaFinPreparacion() == null ? LocalDateTime.now() : pedido.getFechaFinPreparacion()));
            pedidoCocina.put("mozo", pedido.getMozo() == null ? "" : pedido.getMozo().getNombre());
            pedidoCocina.put("detalles", detalles);
            pedidos.add(pedidoCocina);
        }
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}/detalles")
    public List<PedidoDetalle> listarDetallesPorPedido(@PathVariable Long id) {
        return pedidoDetalleRepository.findByPedidoId(id);
    }

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, Object> payload, HttpSession session) {
        Object identidadSesion = session.getAttribute("usuarioAutenticado");
        if (!(identidadSesion instanceof Map<?, ?> identidad)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Inicie sesión para enviar una comanda"));
        }

        String rol = identidad.get("rol") == null ? "" : identidad.get("rol").toString().trim().toUpperCase();
        if (!List.of("MOZO", "MESERO").contains(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Solo un mesero puede enviar una comanda"));
        }

        String codigo = identidad.get("codigo") == null ? "" : identidad.get("codigo").toString().trim();
        if (codigo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La sesión no tiene un código de mesero"));
        }

        var mozo = usuarioRepository.findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigo, codigo);
        if (mozo.isEmpty() || !mozo.get().isActivo()
                || !List.of("MOZO", "MESERO").contains(mozo.get().getRol().trim().toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "El mesero de la sesión no está registrado en usuarios"));
        }

        try {
            Pedido cabecera = objectMapper.convertValue(payload.get("pedido"), Pedido.class);
            cabecera.setMozo(mozo.get());
            List<?> rawDetalles = (List<?>) payload.get("detalles");
            if (rawDetalles == null || rawDetalles.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "La comanda debe incluir productos"));
            }
            List<PedidoDetalle> detalles = rawDetalles.stream()
                    .<PedidoDetalle>map(detalle -> objectMapper.convertValue(detalle, PedidoDetalle.class))
                    .toList();
            Pedido guardado = pedidoService.crearPedidoCompleto(
                    cabecera, detalles, nombreMozoDeSesion(session));
            return ResponseEntity.ok(Map.of(
                    "id", guardado.getId(),
                    "estadoPedido", guardado.getEstadoPedido()));
        } catch (IllegalArgumentException | NullPointerException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Los datos de la comanda no son válidos"));
        }
    }

    @PutMapping("/{id}/estado-cocina")
    public ResponseEntity<?> actualizarEstadoCocina(
            @PathVariable Long id,
            @RequestParam String nuevoEstado,
            HttpSession session) {
        ResponseEntity<?> acceso = validarAccesoCocina(session);
        if (acceso != null) return acceso;

        String estado = nuevoEstado.trim().toUpperCase();
        if (!List.of("EN_PREPARACION", "LISTO").contains(estado)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El estado de cocina no es válido"));
        }

        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) {
            return ResponseEntity.notFound().build();
        }
        if (estado.equals(pedido.getEstadoPedido())) {
            return ResponseEntity.ok(Map.of("id", pedido.getId(), "estadoPedido", estado));
        }
        boolean transicionPermitida = ("EN_COLA".equals(pedido.getEstadoPedido()) && "EN_PREPARACION".equals(estado))
                || ("EN_PREPARACION".equals(pedido.getEstadoPedido()) && "LISTO".equals(estado));
        if (!transicionPermitida) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La comanda ya no permite ese cambio de estado"));
        }

        if ("EN_PREPARACION".equals(estado)) {
            pedido.setFechaInicioPreparacion(LocalDateTime.now());
        } else {
            pedido.setFechaFinPreparacion(LocalDateTime.now());
        }
        pedido.setEstadoPedido(estado);
        pedidoRepository.save(pedido);
        return ResponseEntity.ok(Map.of("id", pedido.getId(), "estadoPedido", estado));
    }

    @PutMapping("/{id}/estado")
    public Pedido actualizarEstadoPedido(@PathVariable Long id, @RequestParam String nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido != null) {
            pedido.setEstadoPedido(nuevoEstado);
            return pedidoRepository.save(pedido);
        }
        return null;
    }

    // GET and PUT both close the order and release its table.
    @RequestMapping(value = "/{id}/pagar", method = {RequestMethod.GET, RequestMethod.PUT})
    public Pedido pagarYLiberarMesa(@PathVariable Long id) {
        return pedidoService.cerrarYPagarPedido(id);
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

    private ResponseEntity<?> validarAccesoCocina(HttpSession session) {
        Object identidadSesion = session.getAttribute("usuarioAutenticado");
        if (!(identidadSesion instanceof Map<?, ?> identidad)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Inicie sesión con una cuenta de cocina"));
        }
        String rol = identidad.get("rol") == null ? "" : identidad.get("rol").toString().trim().toUpperCase();
        if (!"COCINERO".equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Solo una cuenta de cocina puede consultar esta cola"));
        }
        return null;
    }

    private long segundosTranscurridos(LocalDateTime inicio, LocalDateTime fin) {
        return Math.max(0, Duration.between(inicio, fin).getSeconds());
    }
}