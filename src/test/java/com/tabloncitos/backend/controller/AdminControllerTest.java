package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.EmpleadoRegistroRequest;
import com.tabloncitos.backend.model.Empleado;
import com.tabloncitos.backend.model.EmpleadoBaja;
import com.tabloncitos.backend.model.Usuario;
import com.tabloncitos.backend.repository.EmpleadoBajaRepository;
import com.tabloncitos.backend.repository.EmpleadoRepository;
import com.tabloncitos.backend.repository.UsuarioRepository;
import com.tabloncitos.backend.service.RecuperacionOtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    private EmpleadoRepository empleadoRepository;
    private UsuarioRepository usuarioRepository;
    private EmpleadoBajaRepository empleadoBajaRepository;
    private JdbcTemplate jdbcTemplate;
    private AdminController controller;

    @BeforeEach
    void setUp() {
        empleadoRepository = mock(EmpleadoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        empleadoBajaRepository = mock(EmpleadoBajaRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        controller = new AdminController(
                empleadoRepository,
                usuarioRepository,
                empleadoBajaRepository,
                mock(RecuperacionOtpService.class),
                jdbcTemplate);
    }

    @Test
    void registraNuevoColaboradorEnUsuariosConLosDatosDelFormulario() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(5L);
            return usuario;
        });

        var response = controller.registrarEmpleado(new EmpleadoRegistroRequest(
                "EMP-5005",
                "74839201",
                "Pablito Perez",
                "pablito@example.com",
                "1234",
                "MOZO",
                "Miraflores"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var saved = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(saved.capture());
        assertEquals("Pablito Perez", saved.getValue().getNombre());
        assertEquals("pablito@example.com", saved.getValue().getCorreo());
        assertEquals("EMP-5005", saved.getValue().getCodigo());
        assertEquals("74839201", saved.getValue().getDni());
        assertEquals("MOZO", saved.getValue().getRol());
        assertEquals("Miraflores", saved.getValue().getSede());
        verify(empleadoRepository, never()).save(any(Empleado.class));
    }

    @Test
    void eliminarUsuarioSinPedidosLoGuardaEnHistorialYBorraElRegistro() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setCodigo("EMP-5005");
        usuario.setDni("74839201");
        usuario.setNombre("Pablito Perez");
        usuario.setCorreo("pablito@example.com");
        usuario.setRol("MOZO");
        usuario.setSede("Miraflores");
        usuario.setPassword("no-archivar");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(5L), eq(5L))).thenReturn(0L);

        var response = controller.darDeBajaEmpleado("USUARIO", 5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(empleadoBajaRepository).save(argThat(baja ->
                baja.getNombre().equals("Pablito Perez")
                        && baja.getCorreo().equals("pablito@example.com")
                        && baja.getCodigo().equals("EMP-5005")));
        verify(usuarioRepository).delete(usuario);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void usuarioConPedidosSeArchivaYDesactivaParaConservarClavesForaneas() {
        Usuario usuario = new Usuario();
        usuario.setId(6L);
        usuario.setCodigo("EMP-5006");
        usuario.setNombre("Trabajador con pedidos");
        usuario.setCorreo("trabajador@example.com");
        usuario.setRol("CAJERO");
        usuario.setPassword("no-archivar");
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(usuario));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(6L), eq(6L))).thenReturn(2L);

        var response = controller.darDeBajaEmpleado("USUARIO", 6L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(empleadoBajaRepository).save(any(EmpleadoBaja.class));
        verify(usuarioRepository).save(argThat(saved -> !saved.isActivo()));
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void darDeBajaSoloDesactivaElEmpleadoSinArchivarloNiEliminarlo() {
        Empleado empleado = new Empleado();
        empleado.setId(7L);
        empleado.setActivo(true);
        when(empleadoRepository.findById(7L)).thenReturn(Optional.of(empleado));

        var response = controller.darDeBajaEmpleadoLogicamente("EMPLEADO", 7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(empleadoRepository).save(argThat(saved -> !saved.isActivo()));
        verify(empleadoRepository, never()).delete(any(Empleado.class));
        verify(empleadoBajaRepository, never()).save(any(EmpleadoBaja.class));
    }
}
