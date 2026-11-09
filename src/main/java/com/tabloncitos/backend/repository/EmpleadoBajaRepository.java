package com.tabloncitos.backend.repository;

import com.tabloncitos.backend.model.EmpleadoBaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpleadoBajaRepository extends JpaRepository<EmpleadoBaja, Long> {
    List<EmpleadoBaja> findAllByOrderByEliminadoEnDesc();
}
