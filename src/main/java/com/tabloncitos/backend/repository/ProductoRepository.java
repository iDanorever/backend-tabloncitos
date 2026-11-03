package com.tabloncitos.backend.repository;

import com.tabloncitos.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // Método personalizado para listar solo productos activos (útil para el menú del mesero)
    List<Producto> findByActivoTrue();
}