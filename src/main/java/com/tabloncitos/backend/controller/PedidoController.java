package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.model.Pedido;
import com.tabloncitos.backend.model.PedidoDetalle;
import com.tabloncitos.backend.repository.PedidoDetalleRepository;
import com.tabloncitos.backend.repository.PedidoRepository;
import com.tabloncitos.backend.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

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
    private ObjectMapper objectMapper;

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @GetMapping("/estado/{estado}")
    public List<Pedido> listarPorEstado(@PathVariable String estado) {
        return pedidoRepository.findByEstadoPedido(estado);
    }

    @GetMapping("/{id}/detalles")
    public List<PedidoDetalle> listarDetallesPorPedido(@PathVariable Long id) {
        return pedidoDetalleRepository.findByPedidoId(id);
    }

    @PostMapping
    public Pedido crearPedido(@RequestBody Map<String, Object> payload) {
        Pedido cabecera = objectMapper.convertValue(payload.get("pedido"), Pedido.class);
        
        List<?> rawDetalles = (List<?>) payload.get("detalles");
        List<PedidoDetalle> detalles = rawDetalles.stream()
                .<PedidoDetalle>map(d -> objectMapper.convertValue(d, PedidoDetalle.class))
                .toList();

        return pedidoService.crearPedidoCompleto(cabecera, detalles);
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
}