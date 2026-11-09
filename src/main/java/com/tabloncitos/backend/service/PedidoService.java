package com.tabloncitos.backend.service;

import com.tabloncitos.backend.model.Pedido;
import com.tabloncitos.backend.model.PedidoDetalle;
import com.tabloncitos.backend.model.Mesa;
import com.tabloncitos.backend.model.Producto;
import com.tabloncitos.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoDetalleRepository pedidoDetalleRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PromocionService promocionService;

    @Transactional
    public Pedido crearPedidoCompleto(Pedido pedidoCabecera, List<PedidoDetalle> detalles) {
        return crearPedidoCompleto(pedidoCabecera, detalles, null);
    }

    @Transactional
    public Pedido crearPedidoCompleto(
            Pedido pedidoCabecera,
            List<PedidoDetalle> detalles,
            String mozoResponsable) {
        promocionService.sincronizarPrecios();
        BigDecimal subtotalGeneral = BigDecimal.ZERO;

        // 1. Validar y procesar cada detalle (congelando el precio histórico)
        for (PedidoDetalle detalle : detalles) {
            Producto productoBD = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + detalle.getProducto().getId()));

            // CONGELAR PRECIO: Asignamos el precio actual del producto como precio histórico unitario
            detalle.setPrecioUnitario(productoBD.getPrecio());

            // Calcular subtotal del ítem (precio unitario * cantidad)
            BigDecimal subtotalItem = productoBD.getPrecio().multiply(new BigDecimal(detalle.getCantidad()));
            detalle.setSubtotal(subtotalItem);

            subtotalGeneral = subtotalGeneral.add(subtotalItem);
        }

        // 2. Calcular impuestos (IGV 18%) y Total
        BigDecimal igv = subtotalGeneral.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoTotal = subtotalGeneral.add(igv);

        // Asignar los valores calculados a la cabecera del pedido
        pedidoCabecera.setSubtotal(subtotalGeneral);
        pedidoCabecera.setIgv(igv);
        pedidoCabecera.setMontoTotal(montoTotal);
        pedidoCabecera.setEstadoPedido("EN_COLA");

        // 3. Guardar la Cabecera (Maestro)
        Pedido pedidoGuardado = pedidoRepository.save(pedidoCabecera);

        // 4. Asociar el ID del pedido guardado a los detalles y guardarlos
        for (PedidoDetalle detalle : detalles) {
            detalle.setPedido(pedidoGuardado);
            pedidoDetalleRepository.save(detalle);
        }

        // 5. Si el pedido está asociado a una mesa, actualizar el estado de la mesa a OCUPADA
        if (pedidoGuardado.getMesa() != null) {
            Mesa mesa = mesaRepository.findById(pedidoGuardado.getMesa().getId()).orElse(null);
            if (mesa != null) {
                mesa.actualizarEstado("OCUPADA", mozoResponsable);
                mesa.registrarComandaEnviada();
                mesaRepository.save(mesa);
            }
        }

        return pedidoGuardado;
    }

    // Método para cobrar/cerrar el pedido y liberar la mesa automáticamente
    @Transactional
    public Pedido cerrarYPagarPedido(Long pedidoId) {
        // 1. Buscamos el pedido por su ID
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + pedidoId));

        // 2. Cambiamos el estado del pedido a PAGADO
        pedido.setEstadoPedido("PAGADO");
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 3. Buscamos la mesa asociada y la ponemos en LIBRE
        if (pedidoGuardado.getMesa() != null) {
            Mesa mesa = mesaRepository.findById(pedidoGuardado.getMesa().getId()).orElse(null);
            if (mesa != null) {
                mesa.actualizarEstado("LIBRE");
                mesaRepository.save(mesa);
            }
        }

        return pedidoGuardado;
    }
}