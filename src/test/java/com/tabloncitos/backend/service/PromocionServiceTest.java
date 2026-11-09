package com.tabloncitos.backend.service;

import com.tabloncitos.backend.dto.PromocionRequest;
import com.tabloncitos.backend.dto.PromocionResponse;
import com.tabloncitos.backend.model.Producto;
import com.tabloncitos.backend.model.Promocion;
import com.tabloncitos.backend.repository.ProductoRepository;
import com.tabloncitos.backend.repository.PromocionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PromocionServiceTest {

    private PromocionRepository promocionRepository;
    private ProductoRepository productoRepository;
    private PromocionService service;

    @BeforeEach
    void setUp() {
        promocionRepository = mock(PromocionRepository.class);
        productoRepository = mock(ProductoRepository.class);
        service = new PromocionService(promocionRepository, productoRepository);
    }

    @Test
    void publicarDescuentoSincronizaElPrecioDelProducto() {
        Producto producto = producto(100);
        Promocion promocion = promocion(producto, LocalDate.now(ZoneId.of("America/Lima")));
        when(promocionRepository.findById(1L)).thenReturn(Optional.of(promocion));
        when(promocionRepository.existsByProductoIdAndEstado(1L, "ACTIVA")).thenReturn(false);
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promocionRepository.findByEstado("ACTIVA")).thenReturn(List.of(promocion));

        PromocionResponse response = service.publicar(1L);

        assertEquals("ACTIVA", response.estado());
        assertEquals(new BigDecimal("85.00"), producto.getPrecio());
        verify(productoRepository).save(producto);
    }

    @Test
    void venceLaPromocionRestauraPrecioYConservaElRegistro() {
        Producto producto = producto(85);
        Promocion promocion = promocion(producto, LocalDate.now(ZoneId.of("America/Lima")).minusDays(1));
        promocion.setEstado("ACTIVA");
        when(promocionRepository.findByEstado("ACTIVA")).thenReturn(List.of(promocion));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sincronizarPrecios();

        assertEquals("VENCIDA", promocion.getEstado());
        assertEquals(new BigDecimal("100.00"), producto.getPrecio());
        verify(productoRepository).save(producto);
        verify(promocionRepository).save(promocion);
    }

    private Producto producto(int precio) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Pollo a la brasa");
        producto.setPrecio(new BigDecimal(precio).setScale(2));
        producto.setActivo(true);
        return producto;
    }

    private Promocion promocion(Producto producto, LocalDate fechaFin) {
        PromocionRequest request = new PromocionRequest(
                "Promo prueba",
                "Descuento",
                "PORCENTAJE",
                new BigDecimal("15.00"),
                producto.getId(),
                Arrays.stream(DayOfWeek.values()).map(DayOfWeek::name).toList(),
                LocalTime.MIN,
                LocalTime.MAX,
                LocalDate.now(ZoneId.of("America/Lima")),
                fechaFin,
                List.of("SALON"));
        Promocion promocion = new Promocion();
        promocion.setId(1L);
        promocion.setNombre(request.nombre());
        promocion.setDescripcion(request.descripcion());
        promocion.setTipoBeneficio(request.tipoBeneficio());
        promocion.setValorBeneficio(request.valorBeneficio());
        promocion.setProducto(producto);
        promocion.setPrecioOriginal(new BigDecimal("100.00"));
        promocion.setDiasSemana(String.join(",", request.diasSemana()));
        promocion.setHoraInicio(request.horaInicio());
        promocion.setHoraFin(request.horaFin());
        promocion.setFechaInicio(request.fechaInicio());
        promocion.setFechaFin(request.fechaFin());
        promocion.setCanales(String.join(",", request.canales()));
        promocion.setEstado("BORRADOR");
        return promocion;
    }
}
