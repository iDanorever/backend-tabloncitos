package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.MesaReservaRequest;
import com.tabloncitos.backend.model.Mesa;
import com.tabloncitos.backend.repository.MesaRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class MesaControllerTest {

    private MesaRepository mesaRepository;
    private MesaController controller;

    @BeforeEach
    void setUp() {
        mesaRepository = mock(MesaRepository.class);
        controller = new MesaController(mesaRepository);
    }

    @Test
    void reservaMesaLibreYGuardaLosDatosDelCliente() {
        Mesa mesa = mesaLibre();
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        LocalDateTime fechaHora = LocalDateTime.now().plusDays(1);

        ResponseEntity<?> response = controller.reservarMesa(
                1L,
                new MesaReservaRequest("Ana Pérez", "999 123 456", fechaHora, 4));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RESERVADA", mesa.getEstado());
        assertEquals("Ana Pérez", mesa.getReservaNombre());
        assertEquals("999 123 456", mesa.getReservaTelefono());
        assertEquals(fechaHora, mesa.getReservaFechaHora());
        assertEquals(4, mesa.getReservaPersonas());
        verify(mesaRepository).save(mesa);
    }

    @Test
    void rechazaReservarUnaMesaQueNoEstaLibre() {
        Mesa mesa = mesaLibre();
        mesa.setEstado("OCUPADA");
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        ResponseEntity<?> response = controller.reservarMesa(
                1L,
                new MesaReservaRequest("Ana Pérez", "999 123 456", LocalDateTime.now().plusDays(1), 4));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(mesaRepository, never()).save(any(Mesa.class));
    }

    @Test
    void liberarMesaEliminaLosDatosDeLaReserva() {
        Mesa mesa = mesaLibre();
        mesa.setEstado("RESERVADA");
        mesa.setReservaNombre("Ana Pérez");
        mesa.setReservaTelefono("999 123 456");
        mesa.setReservaFechaHora(LocalDateTime.now().plusDays(1));
        mesa.setReservaPersonas(4);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        ResponseEntity<?> response = controller.actualizarEstadoMesa(
                1L, Map.of("estado", "LIBRE"), mock(HttpSession.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("LIBRE", mesa.getEstado());
        assertNull(mesa.getReservaNombre());
        assertNull(mesa.getReservaTelefono());
        assertNull(mesa.getReservaFechaHora());
        assertNull(mesa.getReservaPersonas());
        verify(mesaRepository).save(mesa);
    }

    @Test
    void iniciaContadorAlOcuparYLoConservaHastaLiberar() {
        Mesa mesa = mesaLibre();

        mesa.actualizarEstado("OCUPADA");
        LocalDateTime inicio = mesa.getOcupadaDesde();

        assertEquals("OCUPADA", mesa.getEstado());
        assertNotNull(inicio);

        mesa.actualizarEstado("CUENTA_PEDIDA");
        assertEquals(inicio, mesa.getOcupadaDesde());

        mesa.actualizarEstado("LIBRE");
        assertNull(mesa.getOcupadaDesde());
    }

    @Test
    void asignaElMozoDeLaSesionAlOcuparLaMesa() {
        Mesa mesa = mesaLibre();
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("usuarioAutenticado"))
                .thenReturn(Map.of("nombre", "Pancho Camilo", "rol", "MOZO"));

        ResponseEntity<?> response = controller.actualizarEstadoMesa(
                1L, Map.of("estado", "OCUPADA"), session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Pancho Camilo", mesa.getMozoResponsable());
        verify(mesaRepository).save(mesa);
    }

    private Mesa mesaLibre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setNumeroMesa(1);
        mesa.setZona("Salón Principal");
        mesa.setEstado("LIBRE");
        return mesa;
    }
}
