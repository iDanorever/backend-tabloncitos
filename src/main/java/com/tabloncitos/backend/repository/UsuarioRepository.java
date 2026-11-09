package com.tabloncitos.backend.repository;

import com.tabloncitos.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    Optional<Usuario> findByCodigoIgnoreCaseOrCorreoIgnoreCase(String codigo, String correo);
    boolean existsByOtpGenerado(String otpGenerado);
    List<Usuario> findByEstadoSolicitudOtp(String estado);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCorreoIgnoreCase(String correo);
}