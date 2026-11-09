package com.tabloncitos.backend.repository;

import com.tabloncitos.backend.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByCodigo(String codigo);
    Optional<Empleado> findByCodigoIgnoreCaseOrCorreoIgnoreCase(String codigo, String correo);
    Optional<Empleado> findByCorreoIgnoreCase(String correo);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByOtpGenerado(String otpGenerado);
    List<Empleado> findByEstadoSolicitudOtp(String estado);
}