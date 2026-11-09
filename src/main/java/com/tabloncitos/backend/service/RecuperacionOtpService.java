package com.tabloncitos.backend.service;

import com.tabloncitos.backend.model.Empleado;
import com.tabloncitos.backend.model.Usuario;
import com.tabloncitos.backend.dto.SolicitudOtpAdminDTO;
import com.tabloncitos.backend.repository.EmpleadoRepository;
import com.tabloncitos.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RecuperacionOtpService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;

    public RecuperacionOtpService(EmpleadoRepository empleadoRepository, UsuarioRepository usuarioRepository) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<SolicitudOtpAdminDTO> solicitar(String codigo, String correo) {
        String codigoBuscado = codigo == null ? "" : codigo.trim();
        String correoBuscado = correo == null ? "" : correo.trim();
        if (codigoBuscado.isEmpty() && correoBuscado.isEmpty()) {
            return Optional.empty();
        }

        Optional<Empleado> empleadoEncontrado = empleadoRepository
                .findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigoBuscado, correoBuscado);
        if (empleadoEncontrado.isPresent()) {
            Empleado empleado = empleadoEncontrado.get();
            if (!empleado.isActivo()) {
                return Optional.empty();
            }
            String otp = generarOtp(empleado.getOtpGenerado());
            empleado.setOtpGenerado(otp);
            empleado.setEstadoSolicitudOtp("PENDIENTE");
            empleadoRepository.save(empleado);
            return Optional.of(new SolicitudOtpAdminDTO(
                    empleado.getCodigo(), empleado.getNombre(), empleado.getCorreo(), otp));
        }

        Optional<Usuario> usuarioEncontrado = usuarioRepository
                .findByCodigoIgnoreCaseOrCorreoIgnoreCase(codigoBuscado, correoBuscado);
        if (usuarioEncontrado.isEmpty() || !usuarioEncontrado.get().isActivo()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioEncontrado.get();
        String otp = generarOtp(usuario.getOtpGenerado());
        usuario.setOtpGenerado(otp);
        usuario.setEstadoSolicitudOtp("PENDIENTE");
        usuarioRepository.save(usuario);
        return Optional.of(new SolicitudOtpAdminDTO(
                usuario.getCodigo(), usuario.getNombre(), usuario.getCorreo(), otp));
    }

    private String generarOtp(String otpAnterior) {
        String nuevoOtp;
        do {
            nuevoOtp = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        } while (nuevoOtp.equals(otpAnterior)
                || empleadoRepository.existsByOtpGenerado(nuevoOtp)
                || usuarioRepository.existsByOtpGenerado(nuevoOtp));
        return nuevoOtp;
    }
}
