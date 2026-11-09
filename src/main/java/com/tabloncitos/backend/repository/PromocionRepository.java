package com.tabloncitos.backend.repository;

import com.tabloncitos.backend.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {
    List<Promocion> findAllByOrderByIdDesc();

    List<Promocion> findByEstado(String estado);

    boolean existsByProductoIdAndEstado(Long productoId, String estado);
}
