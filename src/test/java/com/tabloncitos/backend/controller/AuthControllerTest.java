package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.LoginRequest;
import com.tabloncitos.backend.model.Usuario;
import com.tabloncitos.backend.repository.EmpleadoRepository;
import com.tabloncitos.backend.repository.UsuarioRepository;
import com.tabloncitos.backend.service.RecuperacionOtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private EmpleadoRepository empleadoRepository;
    private UsuarioRepository usuarioRepository;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        empleadoRepository = mock(EmpleadoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        controller = new AuthController(
                empleadoRepository,
                usuarioRepository,
                mock(RecuperacionOtpService.class));
    }

    @Test
    void devuelveLaIdentidadDeLaCuentaAutenticada() {
        Usuario usuario = new Usuario();
        usuario.setCodigo("EMP-1001");
        usuario.setNombre("Sara Jimenez");
        usuario.setCorreo("sara@example.com");
        usuario.setPassword("1234");
        usuario.setRol("MESERO");
        usuario.setActivo(true);
        when(empleadoRepository.findByCodigoIgnoreCaseOrCorreoIgnoreCase("EMP-1001", "EMP-1001"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCodigoIgnoreCaseOrCorreoIgnoreCase("EMP-1001", "EMP-1001"))
                .thenReturn(Optional.of(usuario));

        MockHttpSession session = new MockHttpSession();
        ResponseEntity<?> response = controller.autenticarPersonal(
                new LoginRequest("EMP-1001", "1234", null), session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Sara Jimenez", body.get("nombre"));
        assertEquals("MESERO", body.get("rol"));
        assertEquals("EMP-1001", body.get("codigo"));
        assertEquals("mesas.html", body.get("redirect"));
        Map<?, ?> identidadSesion = (Map<?, ?>) session.getAttribute("usuarioAutenticado");
        assertEquals("Sara Jimenez", identidadSesion.get("nombre"));
        assertEquals("MESERO", identidadSesion.get("rol"));
        assertEquals("EMP-1001", identidadSesion.get("codigo"));
        assertEquals(identidadSesion, controller.obtenerSesion(session).getBody());
    }

    @Test
    void explicaCuandoElOtpCorrectoAunEstaPendienteDeAprobacion() {
        Usuario usuario = new Usuario();
        usuario.setCodigo("EMP-1001");
        usuario.setActivo(true);
        usuario.setOtpGenerado("751846");
        usuario.setEstadoSolicitudOtp("PENDIENTE");
        when(empleadoRepository.findByCodigoIgnoreCaseOrCorreoIgnoreCase("EMP-1001", "EMP-1001"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCodigoIgnoreCaseOrCorreoIgnoreCase("EMP-1001", "EMP-1001"))
                .thenReturn(Optional.of(usuario));

        ResponseEntity<?> response = controller.validarOtp(Map.of(
                "codigo", "EMP-1001",
                "otp", "751846"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(
                "El administrador debe aprobar este OTP antes de que pueda utilizarse",
                ((Map<?, ?>) response.getBody()).get("error"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void validaOtpDeUsuarioLuegoDeLaAprobacionDelAdministrador() {
        Usuario usuario = new Usuario();
        usuario.setCodigo("EMP-1001");
        usuario.setActivo(true);
        usuario.setOtpGenerado("751846");
        usuario.setEstadoSolicitudOtp("APROBADO");
        when(empleadoRepository.findByCodigoIgnoreCaseOrCorreoIgnoreCase("EMP-1001", "EMP-1001"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCodigoIgnoreCaseOrCorreoIgnoreCase("EMP-1001", "EMP-1001"))
                .thenReturn(Optional.of(usuario));

        ResponseEntity<?> response = controller.validarOtp(Map.of(
                "codigo", "EMP-1001",
                "otp", "751846"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("VALIDADO", usuario.getEstadoSolicitudOtp());
        verify(usuarioRepository).save(usuario);
    }
}
