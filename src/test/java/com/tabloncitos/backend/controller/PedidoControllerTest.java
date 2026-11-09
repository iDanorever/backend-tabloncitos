package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.model.Mesa;
import com.tabloncitos.backend.model.Pedido;
import com.tabloncitos.backend.model.PedidoDetalle;
import com.tabloncitos.backend.model.Producto;
import com.tabloncitos.backend.repository.PedidoDetalleRepository;
import com.tabloncitos.backend.repository.PedidoRepository;
import com.tabloncitos.backend.repository.UsuarioRepository;
import com.tabloncitos.backend.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PedidoControllerTest {

    private PedidoRepository pedidoRepository;
    private PedidoDetalleRepository pedidoDetalleRepository;
    private PedidoController controller;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        pedidoDetalleRepository = mock(PedidoDetalleRepository.class);
        controller = new PedidoController();
        ReflectionTestUtils.setField(controller, "pedidoRepository", pedidoRepository);
        ReflectionTestUtils.setField(controller, "pedidoDetalleRepository", pedidoDetalleRepository);
        ReflectionTestUtils.setField(controller, "pedidoService", mock(PedidoService.class));
        ReflectionTestUtils.setField(controller, "usuarioRepository", mock(UsuarioRepository.class));
        ReflectionTestUtils.setField(controller, "objectMapper", mock(ObjectMapper.class));
    }

    @Test
    void requiereSesionDeCocinaParaConsultarLaCola() {
        ResponseEntity<?> response = controller.listarPedidosCocina(mock(HttpSession.class));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(pedidoRepository, pedidoDetalleRepository);
    }

    @Test
    void devuelveLaMesaYLosPlatosDeCadaComanda() {
        Mesa mesa = new Mesa();
        mesa.setId(9410L);
        mesa.setNumeroMesa(2);
        Producto producto = new Producto();
        producto.setNombre("Pollo a la brasa");
        Pedido pedido = new Pedido();
        pedido.setId(31L);
        pedido.setMesa(mesa);
        pedido.setEstadoPedido("EN_COLA");
        pedido.setFechaCreacion(LocalDateTime.now().minusMinutes(7).minusSeconds(10));
        PedidoDetalle detalle = new PedidoDetalle();
        detalle.setId(7L);
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setObservacion("Sin picante");

        when(pedidoRepository.findByEstadoPedidoInOrderByFechaCreacionAsc(
                List.of("EN_COLA", "EN_PREPARACION", "LISTO"))).thenReturn(List.of(pedido));
        when(pedidoDetalleRepository.findByPedidoId(31L)).thenReturn(List.of(detalle));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("usuarioAutenticado")).thenReturn(Map.of("rol", "COCINERO"));

        ResponseEntity<?> response = controller.listarPedidosCocina(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> pedidos = (List<?>) response.getBody();
        Map<?, ?> resultado = (Map<?, ?>) pedidos.get(0);
        Map<?, ?> mesaResultado = (Map<?, ?>) resultado.get("mesa");
        List<?> detalles = (List<?>) resultado.get("detalles");
        Map<?, ?> detalleResultado = (Map<?, ?>) detalles.get(0);
        assertEquals(9410L, mesaResultado.get("id"));
        assertEquals("Pollo a la brasa", detalleResultado.get("productoNombre"));
        assertEquals(2, detalleResultado.get("cantidad"));
        assertEquals("Sin picante", detalleResultado.get("observacion"));
        long segundosEnCola = ((Number) resultado.get("segundosEnCola")).longValue();
        assertTrue(segundosEnCola >= 430 && segundosEnCola <= 431);
    }

    @Test
    void cocinaPuedeAvanzarUnaComandaAPreparacion() {
        Pedido pedido = new Pedido();
        pedido.setId(31L);
        pedido.setEstadoPedido("EN_COLA");
        when(pedidoRepository.findById(31L)).thenReturn(Optional.of(pedido));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("usuarioAutenticado")).thenReturn(Map.of("rol", "COCINERO"));

        ResponseEntity<?> response = controller.actualizarEstadoCocina(31L, "EN_PREPARACION", session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("EN_PREPARACION", pedido.getEstadoPedido());
        assertTrue(pedido.getFechaInicioPreparacion() != null);
        assertNull(pedido.getFechaFinPreparacion());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void conservaElTiempoDePreparacionAlConsultarUnaComandaLista() {
        Pedido pedido = new Pedido();
        pedido.setId(31L);
        pedido.setEstadoPedido("LISTO");
        pedido.setFechaInicioPreparacion(LocalDateTime.now().minusMinutes(8));
        pedido.setFechaFinPreparacion(pedido.getFechaInicioPreparacion().plusMinutes(7).plusSeconds(10));
        when(pedidoRepository.findByEstadoPedidoInOrderByFechaCreacionAsc(
                List.of("EN_COLA", "EN_PREPARACION", "LISTO"))).thenReturn(List.of(pedido));
        when(pedidoDetalleRepository.findByPedidoId(31L)).thenReturn(List.of());
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("usuarioAutenticado")).thenReturn(Map.of("rol", "COCINERO"));

        ResponseEntity<?> response = controller.listarPedidosCocina(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> resultado = (Map<?, ?>) ((List<?>) response.getBody()).get(0);
        assertEquals(430L, resultado.get("segundosPreparacion"));
    }

    @Test
    void alMarcarLaComandaListaSeDetieneElTiempoDePreparacion() {
        Pedido pedido = new Pedido();
        pedido.setId(31L);
        pedido.setEstadoPedido("EN_PREPARACION");
        pedido.setFechaInicioPreparacion(LocalDateTime.now().minusMinutes(4));
        when(pedidoRepository.findById(31L)).thenReturn(Optional.of(pedido));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("usuarioAutenticado")).thenReturn(Map.of("rol", "COCINERO"));

        ResponseEntity<?> response = controller.actualizarEstadoCocina(31L, "LISTO", session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("LISTO", pedido.getEstadoPedido());
        assertTrue(pedido.getFechaFinPreparacion() != null);
        verify(pedidoRepository).save(pedido);
    }
}
