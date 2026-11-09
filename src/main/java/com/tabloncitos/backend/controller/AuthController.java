package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.LoginRequest;
import com.tabloncitos.backend.dto.SolicitudOtpAdminDTO;
import com.tabloncitos.backend.model.Empleado;
import com.tabloncitos.backend.model.Usuario;
import com.tabloncitos.backend.repository.EmpleadoRepository;
import com.tabloncitos.backend.repository.UsuarioRepository;
import com.tabloncitos.backend.service.RecuperacionOtpService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final Set<String> ROLES_VALIDOS = Set.of("ADMIN", "COCINERO", "MOZO", "MESERO", "CAJERO");

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecuperacionOtpService recuperacionOtpService;

    public AuthController(
            EmpleadoRepository empleadoRepository,
            UsuarioRepository usuarioRepository,
            RecuperacionOtpService recuperacionOtpService) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.recuperacionOtpService = recuperacionOtpService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> autenticarPersonal(
            @RequestBody LoginRequest request, HttpSession session) {
        String identificador = primerValor(request.identificador());
        String password = request.password();
        String rolSeleccionado = primerValor(request.rolSeleccionado());

        log.info("Login recibido para identificador={}, passwordPresent={}, passwordLength={}",
                identificador, password != null, password == null ? 0 : password.length());

        if (identificador == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ingrese su código o correo y contraseña"));
        }

        if (rolSeleccionado != null && !ROLES_VALIDOS.contains(rolSeleccionado.toUpperCase())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El rol seleccionado no es válido"));
        }

        Optional<Empleado> empleadoEncontrado = empleadoRepository
                .findByCodigoIgnoreCaseOrCorreoIgnoreCase(identificador.trim(), identificador.trim());

        if (empleadoEncontrado.isPresent()) {
            Empleado empleado = empleadoEncontrado.get();
            if (empleado.isActivo() && password.equals(empleado.getPassword())) {
                return respuestaLogin(
                        empleado.getRol(), empleado.getNombre(), empleado.getCodigo(), rolSeleccionado, session);
            }
        }

        Optional<Usuario> usuarioEncontrado = usuarioRepository
                .findByCodigoIgnoreCaseOrCorreoIgnoreCase(identificador.trim(), identificador.trim());
        if (usuarioEncontrado.isPresent()) {
            Usuario usuario = usuarioEncontrado.get();
            if (usuario.isActivo() && password.equals(usuario.getPassword())) {
                return respuestaLogin(
                        usuario.getRol(), usuario.getNombre(), usuario.getCodigo(), rolSeleccionado, session);
            }
        }

        return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
    }

    @GetMapping("/sesion")
    public ResponseEntity<?> obtenerSesion(HttpSession session) {
        Object usuario = session.getAttribute("usuarioAutenticado");
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No hay una cuenta iniciada"));
        }
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> cerrarSesion(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada"));
    }

    @PostMapping("/recuperar")
    public ResponseEntity<?> solicitarRecuperacion(@RequestBody Map<String, String> datos) {
        String codigo = primerValor(datos.get("codigo"));
        String correo = primerValor(datos.get("correo"));
        String identificador = primerValor(datos.get("identificador"));
        if (identificador != null) {
            if (identificador.contains("@")) {
                if (correo == null) {
                    correo = identificador;
                }
            } else if (codigo == null) {
                codigo = identificador;
            }
        }
        Optional<SolicitudOtpAdminDTO> solicitud = recuperacionOtpService.solicitar(codigo, correo);
        if (solicitud.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "Este correo/código no está registrado en el sistema"));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "mensaje", "Solicitud registrada. El administrador debe validar su identidad.",
                "codigo", solicitud.get().codigo(),
                "correo", solicitud.get().correo(),
                "next", "otp.html"));
    }

    @PostMapping("/validar-otp")
    public ResponseEntity<?> validarOtp(@RequestBody Map<String, String> datos) {
        String codigo = datos.get("codigo");
        String otp = datos.get("otp");

        if (codigo == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código de empleado y OTP son obligatorios"));
        }

        Optional<Empleado> empleadoEncontrado = empleadoRepository
                .findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigo.trim(), codigo.trim());
        if (empleadoEncontrado.isPresent()) {
            Empleado empleado = empleadoEncontrado.get();
            if (empleado.isActivo()
                    && "PENDIENTE".equals(empleado.getEstadoSolicitudOtp())
                    && otp.equals(empleado.getOtpGenerado())) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "El administrador debe aprobar este OTP antes de que pueda utilizarse"));
            }
            if (empleado.isActivo()
                    && "APROBADO".equals(empleado.getEstadoSolicitudOtp())
                    && otp.equals(empleado.getOtpGenerado())) {
                empleado.setEstadoSolicitudOtp("VALIDADO");
                empleadoRepository.save(empleado);
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Token validado correctamente",
                        "next", "nueva-clave.html"));
            }
        }

        Optional<Usuario> usuarioEncontrado = usuarioRepository
                .findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigo.trim(), codigo.trim());
        if (usuarioEncontrado.isPresent()) {
            Usuario usuario = usuarioEncontrado.get();
            if (usuario.isActivo()
                    && "PENDIENTE".equals(usuario.getEstadoSolicitudOtp())
                    && otp.equals(usuario.getOtpGenerado())) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "El administrador debe aprobar este OTP antes de que pueda utilizarse"));
            }
            if (usuario.isActivo()
                    && "APROBADO".equals(usuario.getEstadoSolicitudOtp())
                    && otp.equals(usuario.getOtpGenerado())) {
                usuario.setEstadoSolicitudOtp("VALIDADO");
                usuarioRepository.save(usuario);
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Token validado correctamente",
                        "next", "nueva-clave.html"));
            }
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Código OTP incorrecto o no autorizado"));
    }

    @PostMapping("/actualizar-clave")
    public ResponseEntity<?> actualizarClave(@RequestBody Map<String, String> datos) {
        String codigo = datos.get("codigo");
        String nuevaClave = datos.get("nuevaClave");

        if (codigo == null || nuevaClave == null || nuevaClave.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "La nueva contraseña debe tener al menos 8 caracteres"));
        }

        Optional<Empleado> empleadoEncontrado = empleadoRepository
                .findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigo.trim(), codigo.trim());
        if (empleadoEncontrado.isPresent()) {
            Empleado empleado = empleadoEncontrado.get();
            if (empleado.isActivo() && "VALIDADO".equals(empleado.getEstadoSolicitudOtp())) {
                empleado.setPassword(nuevaClave);
                empleado.setOtpGenerado(null);
                empleado.setEstadoSolicitudOtp("COMPLETADO");
                empleadoRepository.save(empleado);
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Contraseña actualizada exitosamente",
                        "next", "index.html"));
            }
        }

        Optional<Usuario> usuarioEncontrado = usuarioRepository
                .findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigo.trim(), codigo.trim());
        if (usuarioEncontrado.isPresent()) {
            Usuario usuario = usuarioEncontrado.get();
            if (usuario.isActivo() && "VALIDADO".equals(usuario.getEstadoSolicitudOtp())) {
                usuario.setPassword(nuevaClave);
                usuario.setOtpGenerado(null);
                usuario.setEstadoSolicitudOtp("COMPLETADO");
                usuarioRepository.save(usuario);
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Contraseña actualizada exitosamente",
                        "next", "index.html"));
            }
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Primero debe validar un OTP autorizado"));
    }

    private ResponseEntity<?> respuestaLogin(
            String rol, String nombre, String codigo, String rolSeleccionado, HttpSession session) {
        String rolNormalizado = rol == null ? "" : rol.trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rolNormalizado)) {
            return ResponseEntity.status(403).body(Map.of("error", "El rol de esta cuenta no está habilitado para el acceso"));
        }
        if (rolSeleccionado != null && !rolesCoinciden(rolNormalizado, rolSeleccionado.toUpperCase())) {
            return ResponseEntity.status(403).body(Map.of("error", "Esta cuenta no pertenece al rol seleccionado"));
        }

        String destino = switch (rolNormalizado) {
            case "ADMIN" -> "admin-empleados.html";
            case "COCINERO" -> "kds.html";
            case "MOZO", "MESERO" -> "mesas.html";
            case "CAJERO" -> "caja.html";
            default -> throw new IllegalStateException("Rol validado sin una ruta asignada");
        };
        Map<String, String> identidad = Map.of(
                "rol", rolNormalizado,
                "nombre", nombre == null ? "" : nombre,
                "codigo", codigo == null ? "" : codigo);
        session.setAttribute("usuarioAutenticado", identidad);
        return ResponseEntity.ok(Map.of(
                "rol", identidad.get("rol"),
                "nombre", identidad.get("nombre"),
                "codigo", identidad.get("codigo"),
                "redirect", destino));
    }

    private boolean rolesCoinciden(String rolCuenta, String rolSeleccionado) {
        return "MOZO".equals(rolCuenta) || "MESERO".equals(rolCuenta)
                ? "MOZO".equals(rolSeleccionado) || "MESERO".equals(rolSeleccionado)
                : rolCuenta.equals(rolSeleccionado);
    }

    private String primerValor(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }
}
