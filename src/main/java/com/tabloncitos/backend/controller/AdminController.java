package com.tabloncitos.backend.controller;

import com.tabloncitos.backend.dto.EmpleadoActualizarRequest;
import com.tabloncitos.backend.dto.EmpleadoBajaDTO;
import com.tabloncitos.backend.dto.EmpleadoRegistroRequest;
import com.tabloncitos.backend.dto.EmpleadoResumenDTO;
import com.tabloncitos.backend.dto.SolicitudOtpAdminDTO;
import com.tabloncitos.backend.model.Empleado;
import com.tabloncitos.backend.model.EmpleadoBaja;
import com.tabloncitos.backend.model.Usuario;
import com.tabloncitos.backend.repository.EmpleadoBajaRepository;
import com.tabloncitos.backend.repository.EmpleadoRepository;
import com.tabloncitos.backend.repository.UsuarioRepository;
import com.tabloncitos.backend.service.RecuperacionOtpService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Set<String> ROLES_VALIDOS = Set.of("ADMIN", "MOZO", "MESERO", "COCINERO", "CAJERO");

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoBajaRepository empleadoBajaRepository;
    private final RecuperacionOtpService recuperacionOtpService;
    private final JdbcTemplate jdbcTemplate;

    public AdminController(
            EmpleadoRepository empleadoRepository,
            UsuarioRepository usuarioRepository,
            EmpleadoBajaRepository empleadoBajaRepository,
            RecuperacionOtpService recuperacionOtpService,
            JdbcTemplate jdbcTemplate) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.empleadoBajaRepository = empleadoBajaRepository;
        this.recuperacionOtpService = recuperacionOtpService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/empleados")
    public ResponseEntity<List<EmpleadoResumenDTO>> listarEmpleados() {
        List<EmpleadoResumenDTO> empleados = new ArrayList<>(empleadoRepository.findAll().stream()
                .map(empleado -> new EmpleadoResumenDTO(
                        empleado.getId(),
                        empleado.getCodigo(),
                        empleado.getDni(),
                        empleado.getNombre(),
                        empleado.getCorreo(),
                        empleado.getRol(),
                        empleado.getSede(),
                        "EMPLEADO",
                        empleado.isActivo()))
                .toList());
        empleados.addAll(usuarioRepository.findAll().stream()
                .map(usuario -> new EmpleadoResumenDTO(
                        usuario.getId(),
                        usuario.getCodigo() == null ? "USR-" + usuario.getId() : usuario.getCodigo(),
                        usuario.getDni(),
                        usuario.getNombre(),
                        usuario.getCorreo(),
                        usuario.getRol(),
                        usuario.getSede(),
                        "USUARIO",
                        usuario.isActivo()))
                .toList());
        return ResponseEntity.ok(empleados);
    }

    @PostMapping("/empleados")
    public ResponseEntity<?> registrarEmpleado(@RequestBody EmpleadoRegistroRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()
                || request.correo() == null || request.correo().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.rol() == null || request.rol().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "Nombre, correo, PIN y rol son obligatorios"));
        }

        String rol = request.rol().trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rol)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "Rol inválido. Valores permitidos: ADMIN, MOZO, MESERO, COCINERO, CAJERO"));
        }

        String correo = request.correo().trim();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)
                || empleadoRepository.existsByCorreoIgnoreCase(correo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "mensaje", "Ya existe una cuenta con ese correo"));
        }

        String codigo = request.codigo();
        if (codigo == null || codigo.isBlank()) {
            codigo = generarCodigoEmpleado();
        } else if (empleadoRepository.findByCodigo(codigo.trim()).isPresent()
                || usuarioRepository.existsByCodigoIgnoreCase(codigo.trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "mensaje", "Ya existe un colaborador con el código " + codigo.trim()));
        } else {
            codigo = codigo.trim();
        }

        Usuario empleado = new Usuario();
        empleado.setCodigo(codigo);
        empleado.setDni(normalizarOpcional(request.dni()));
        empleado.setNombre(request.nombre().trim());
        empleado.setCorreo(correo);
        empleado.setPassword(request.password());
        empleado.setRol(rol);
        empleado.setSede(normalizarOpcional(request.sede()));
        empleado.setActivo(true);

        Usuario guardado = usuarioRepository.save(empleado);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "mensaje", "Colaborador registrado correctamente",
                "empleado", new EmpleadoResumenDTO(
                        guardado.getId(),
                        guardado.getCodigo(),
                        guardado.getDni(),
                        guardado.getNombre(),
                        guardado.getCorreo(),
                        guardado.getRol(),
                        guardado.getSede(),
                        "USUARIO",
                        guardado.isActivo())));
    }

    @PutMapping("/empleados/{fuente}/{id}")
    public ResponseEntity<?> actualizarEmpleado(
            @PathVariable String fuente,
            @PathVariable Long id,
            @RequestBody EmpleadoActualizarRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()
                || request.correo() == null || request.correo().isBlank()
                || request.rol() == null || request.rol().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "Nombre, correo y rol son obligatorios"));
        }

        String rol = request.rol().trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rol)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "Rol inválido. Valores permitidos: ADMIN, MOZO, MESERO, COCINERO, CAJERO"));
        }

        String correo = request.correo().trim();
        switch (fuente.toUpperCase()) {
            case "EMPLEADO" -> {
                Optional<Empleado> encontrado = empleadoRepository.findById(id);
                if (encontrado.isEmpty()) {
                    return empleadoNoEncontrado();
                }
                Empleado empleado = encontrado.get();
                if (correoEnUso(correo, "EMPLEADO", id)) {
                    return correoEnConflicto();
                }
                empleado.setDni(normalizarOpcional(request.dni()));
                empleado.setNombre(request.nombre().trim());
                empleado.setCorreo(correo);
                empleado.setRol(rol);
                empleado.setSede(normalizarOpcional(request.sede()));
                if (request.password() != null && !request.password().isBlank()) {
                    empleado.setPassword(request.password());
                }
                empleadoRepository.save(empleado);
            }
            case "USUARIO" -> {
                Optional<Usuario> encontrado = usuarioRepository.findById(id);
                if (encontrado.isEmpty()) {
                    return empleadoNoEncontrado();
                }
                Usuario usuario = encontrado.get();
                if (correoEnUso(correo, "USUARIO", id)) {
                    return correoEnConflicto();
                }
                usuario.setNombre(request.nombre().trim());
                usuario.setCorreo(correo);
                usuario.setRol(rol);
                if (request.password() != null && !request.password().isBlank()) {
                    usuario.setPassword(request.password());
                }
                usuarioRepository.save(usuario);
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "mensaje", "Origen de empleado inválido"));
            }
        }

        return ResponseEntity.ok(Map.of("status", "success", "mensaje", "Empleado actualizado correctamente"));
    }

    @DeleteMapping("/empleados/{fuente}/{id}")
    @Transactional
    public ResponseEntity<?> darDeBajaEmpleado(@PathVariable String fuente, @PathVariable Long id) {
        switch (fuente.toUpperCase()) {
            case "EMPLEADO" -> {
                Optional<Empleado> empleado = empleadoRepository.findById(id);
                if (empleado.isEmpty()) {
                    return empleadoNoEncontrado();
                }
                empleadoBajaRepository.save(crearHistorial(
                        "EMPLEADO",
                        empleado.get().getId(),
                        empleado.get().getCodigo(),
                        empleado.get().getDni(),
                        empleado.get().getNombre(),
                        empleado.get().getCorreo(),
                        empleado.get().getRol(),
                        empleado.get().getSede(),
                        "Registro eliminado de empleados"));
                empleadoRepository.delete(empleado.get());
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "eliminadoFisicamente", true,
                        "mensaje", "Empleado eliminado de la tabla empleados y archivado en el historial"));
            }
            case "USUARIO" -> {
                Optional<Usuario> usuario = usuarioRepository.findById(id);
                if (usuario.isEmpty()) {
                    return empleadoNoEncontrado();
                }
                Usuario empleado = usuario.get();
                long pedidosAsociados = jdbcTemplate.queryForObject(
                        "SELECT (SELECT COUNT(*) FROM pedidos WHERE mozo_id = ?) + "
                                + "(SELECT COUNT(*) FROM pedidos WHERE cajero_id = ?)",
                        Long.class,
                        id,
                        id);
                boolean puedeEliminar = pedidosAsociados == 0;
                empleadoBajaRepository.save(crearHistorial(
                        "USUARIO",
                        empleado.getId(),
                        empleado.getCodigo() == null ? "USR-" + empleado.getId() : empleado.getCodigo(),
                        empleado.getDni(),
                        empleado.getNombre(),
                        empleado.getCorreo(),
                        empleado.getRol(),
                        empleado.getSede(),
                        puedeEliminar
                                ? "Registro eliminado de usuarios"
                                : "Inactivado para conservar pedidos asociados"));
                if (puedeEliminar) {
                    usuarioRepository.delete(empleado);
                } else {
                    empleado.setActivo(false);
                    usuarioRepository.save(empleado);
                }
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "eliminadoFisicamente", puedeEliminar,
                        "mensaje", puedeEliminar
                                ? "Empleado eliminado de la tabla usuarios y archivado en el historial"
                                : "El empleado se archivó y desactivó; se conserva en usuarios porque tiene pedidos asociados"));
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "mensaje", "Origen de empleado inválido"));
            }
        }
    }

    @PutMapping("/empleados/{fuente}/{id}/reactivar")
    public ResponseEntity<?> reactivarEmpleado(@PathVariable String fuente, @PathVariable Long id) {
        return cambiarEstadoEmpleado(fuente, id, true);
    }

    @PutMapping("/empleados/{fuente}/{id}/baja")
    public ResponseEntity<?> darDeBajaEmpleadoLogicamente(@PathVariable String fuente, @PathVariable Long id) {
        return cambiarEstadoEmpleado(fuente, id, false);
    }

    @GetMapping("/empleados/historial")
    public ResponseEntity<List<EmpleadoBajaDTO>> obtenerHistorialBajas() {
        List<EmpleadoBajaDTO> historial = empleadoBajaRepository.findAllByOrderByEliminadoEnDesc().stream()
                .map(baja -> new EmpleadoBajaDTO(
                        baja.getId(),
                        baja.getCodigo(),
                        baja.getDni(),
                        baja.getNombre(),
                        baja.getCorreo(),
                        baja.getRol(),
                        baja.getSede(),
                        baja.getFuente(),
                        baja.getMotivo(),
                        baja.getEliminadoEn()))
                .toList();
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/solicitudes-pendientes")
    public ResponseEntity<List<SolicitudOtpAdminDTO>> obtenerSolicitudesPendientes() {
        List<SolicitudOtpAdminDTO> pendientes = new ArrayList<>(empleadoRepository.findByEstadoSolicitudOtp("PENDIENTE").stream()
                .map(empleado -> new SolicitudOtpAdminDTO(
                        empleado.getCodigo(),
                        empleado.getNombre(),
                        empleado.getCorreo(),
                        empleado.getOtpGenerado()))
                .toList());
        pendientes.addAll(usuarioRepository.findByEstadoSolicitudOtp("PENDIENTE").stream()
                .map(usuario -> new SolicitudOtpAdminDTO(
                        usuario.getCodigo(),
                        usuario.getNombre(),
                        usuario.getCorreo(),
                        usuario.getOtpGenerado()))
                .toList());
        return ResponseEntity.ok(pendientes);
    }

    @PostMapping("/aprobar-otp")
    public ResponseEntity<?> aprobarOtp(@RequestBody Map<String, String> request) {
        String codigo = request.get("codigo");
        Optional<Empleado> optEmp = empleadoRepository.findByCodigo(codigo);
        
        if (optEmp.isPresent()) {
            Empleado emp = optEmp.get();
            emp.setEstadoSolicitudOtp("APROBADO");
            empleadoRepository.save(emp);
            return ResponseEntity.ok(Map.of("status", "success", "mensaje", "OTP autorizado correctamente"));
        }
        Optional<Usuario> usuario = usuarioRepository.findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigo, codigo);
        if (usuario.isPresent()) {
            Usuario cuenta = usuario.get();
            cuenta.setEstadoSolicitudOtp("APROBADO");
            usuarioRepository.save(cuenta);
            return ResponseEntity.ok(Map.of("status", "success", "mensaje", "OTP autorizado correctamente"));
        }
        return ResponseEntity.status(404).body(Map.of("status", "error", "mensaje", "Empleado no encontrado"));
    }

    @PostMapping("/baja-logica")
    public ResponseEntity<?> bajaLogica(@RequestBody Map<String, String> request) {
        String codigo = request.get("codigo");
        Optional<Empleado> optEmp = empleadoRepository.findByCodigo(codigo);
        
        if (optEmp.isPresent()) {
            Empleado emp = optEmp.get();
            emp.setActivo(false);
            empleadoRepository.save(emp);
            return ResponseEntity.ok(Map.of("status", "success", "mensaje", "Baja lógica aplicada y guardada en BD"));
        }
        return ResponseEntity.status(404).body(Map.of("status", "error", "mensaje", "Empleado no encontrado"));
    }

    @PostMapping("/solicitar-recuperacion")
    public ResponseEntity<?> solicitarRecuperacion(@RequestBody Map<String, String> request) {
        String codigo = request.get("codigo");
        String correo = request.get("correo");
        String identificador = request.get("identificador");
        if (identificador != null && !identificador.isBlank()) {
            if (identificador.contains("@")) {
                if (correo == null || correo.isBlank()) {
                    correo = identificador;
                }
            } else if (codigo == null || codigo.isBlank()) {
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

    private String generarCodigoEmpleado() {
        String codigo;
        do {
            codigo = "EMP-" + java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 10000);
        } while (empleadoRepository.findByCodigo(codigo).isPresent()
                || usuarioRepository.existsByCodigoIgnoreCase(codigo));
        return codigo;
    }

    private EmpleadoBaja crearHistorial(
            String fuente,
            Long idOriginal,
            String codigo,
            String dni,
            String nombre,
            String correo,
            String rol,
            String sede,
            String motivo) {
        EmpleadoBaja baja = new EmpleadoBaja();
        baja.setFuente(fuente);
        baja.setIdOriginal(idOriginal);
        baja.setCodigo(codigo);
        baja.setDni(dni);
        baja.setNombre(nombre);
        baja.setCorreo(correo);
        baja.setRol(rol);
        baja.setSede(sede);
        baja.setMotivo(motivo);
        baja.setEliminadoEn(java.time.LocalDateTime.now());
        return baja;
    }

    private boolean correoEnUso(String correo, String fuente, Long id) {
        Optional<Empleado> empleado = empleadoRepository.findByCorreoIgnoreCase(correo);
        if (empleado.isPresent()
                && (!"EMPLEADO".equals(fuente) || !empleado.get().getId().equals(id))) {
            return true;
        }
        Optional<Usuario> usuario = usuarioRepository.findByCorreoIgnoreCase(correo);
        return usuario.isPresent()
                && (!"USUARIO".equals(fuente) || !usuario.get().getId().equals(id));
    }

    private ResponseEntity<?> cambiarEstadoEmpleado(String fuente, Long id, boolean activo) {
        switch (fuente.toUpperCase()) {
            case "EMPLEADO" -> {
                Optional<Empleado> empleado = empleadoRepository.findById(id);
                if (empleado.isEmpty()) {
                    return empleadoNoEncontrado();
                }
                empleado.get().setActivo(activo);
                empleadoRepository.save(empleado.get());
            }
            case "USUARIO" -> {
                Optional<Usuario> usuario = usuarioRepository.findById(id);
                if (usuario.isEmpty()) {
                    return empleadoNoEncontrado();
                }
                usuario.get().setActivo(activo);
                usuarioRepository.save(usuario.get());
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "mensaje", "Origen de empleado inválido"));
            }
        }
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "mensaje", activo ? "Empleado reactivado correctamente" : "Empleado dado de baja; el registro se conserva"));
    }

    private ResponseEntity<Map<String, String>> empleadoNoEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", "error",
                "mensaje", "Empleado no encontrado"));
    }

    private ResponseEntity<Map<String, String>> correoEnConflicto() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", "error",
                "mensaje", "Ya existe otra cuenta con ese correo"));
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}